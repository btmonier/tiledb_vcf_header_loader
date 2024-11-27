package org.btmonier.vcfhload.utils

import io.tiledb.java.api.*
import io.tiledb.java.api.Array
import io.tiledb.java.api.Constants.TILEDB_VAR_NUM


fun createArray(ctx: Context, uri: String) {
    // Create dimensions
    val dimRRId        = Dimension(ctx, "rr_id", Datatype.TILEDB_STRING_ASCII, null, null)
    val dimRRChecksum  = Dimension(ctx, "rr_checksum", Datatype.TILEDB_STRING_ASCII, null, null)
    val dimRRChr       = Dimension(ctx, "rr_chr", Datatype.TILEDB_STRING_ASCII, null, null)
    val dimRRStart     = Dimension(ctx, "rr_start", Datatype.TILEDB_INT64, Pair(1, Long.MAX_VALUE), 1)
    val dimRREnd       = Dimension(ctx, "rr_end", Datatype.TILEDB_INT64, Pair(1, Long.MAX_VALUE), 1)
    val dimSampleId    = Dimension(ctx, "sample_id", Datatype.TILEDB_STRING_ASCII, null, null)
    val dimSampleChr   = Dimension(ctx, "sample_chr", Datatype.TILEDB_STRING_ASCII, null, null)
    val dimSampleStart = Dimension(ctx, "sample_start", Datatype.TILEDB_INT64, Pair(1, Long.MAX_VALUE), 1)
    val dimSampleEnd   = Dimension(ctx, "sample_end", Datatype.TILEDB_INT64, Pair(1, Long.MAX_VALUE), 1)


    // Create domain
    val domain = Domain(ctx)
    domain
        .addDimension(dimRRId)
        .addDimension(dimRRChecksum)
        .addDimension(dimRRChr)
        .addDimension(dimRRStart)
        .addDimension(dimRREnd)
        .addDimension(dimSampleId)
        .addDimension(dimSampleChr)
        .addDimension(dimSampleStart)
        .addDimension(dimSampleEnd)

    // Add attributes
    val attChecksum = Attribute(ctx, "checksum", Datatype.TILEDB_STRING_ASCII)
    attChecksum.setCellValNum(TILEDB_VAR_NUM)

    val schema = ArraySchema(ctx, ArrayType.TILEDB_SPARSE)
    schema.domain = domain
    schema.addAttribute(attChecksum)

    schema.check()
    schema.dump()

    // Create the array
    Array.create(uri, schema)
}

fun writeArray(ctx: Context, uri: String, altData: List<AltHeaderMetaData>) {
    val arr = Array(ctx, uri, QueryType.TILEDB_WRITE)
    val query = Query(arr)

    // Dimensions - ref ranges
    val rrComps = altData.map { it.refRange.split(":", "-") }
    setTileDBString(query, ctx, "rr_checksum", altData.map { it.refChecksum })
    setTileDBString(query, ctx, "rr_id", altData.map { it.refRange })
    setTileDBString(query, ctx, "rr_chr", rrComps.map { it[0] })
    setTileDBInt(query, ctx, "rr_start", rrComps.map { it[1].toLong()})
    setTileDBInt(query, ctx, "rr_end", rrComps.map { it[2].toLong()})

    // Dimensions - asm ranges
    val asmComps = altData.map { it.regions[0]}
    setTileDBString(query, ctx, "sample_id", altData.map { it.sampleGamete.name })
    setTileDBString(query, ctx, "sample_chr",  asmComps.map { it.first.contig })
    setTileDBInt(query, ctx, "sample_start", asmComps.map { it.first.position.toLong() })
    setTileDBInt(query, ctx, "sample_end", asmComps.map { it.second.position.toLong() })

    // Attributes
    setTileDBString(query, ctx, "checksum", altData.map { it.checksum })

    query.submit()
    query.close()
}

private fun createOffsets(strings: List<String>): LongArray {
    // Method 2: Using accumulator
    val offsets = LongArray(strings.size)
    var currentOffset = 0L

    strings.forEachIndexed { index, _ ->
        offsets[index] = currentOffset
        currentOffset += strings[index].length
    }

    return offsets
}

private fun setTileDBString(
    query: Query,
    ctx: Context,
    dimensionName: String,
    values: List<String>
) {
    val valuesArray = NativeArray(ctx, values.joinToString(""), Datatype.TILEDB_STRING_ASCII)
    val offsetsArray = NativeArray(ctx, createOffsets(values), Datatype.TILEDB_UINT64)
    query.setDataBuffer(dimensionName, valuesArray)
    query.setOffsetsBuffer(dimensionName, offsetsArray)
}

private fun setTileDBInt(
    query: Query,
    ctx: Context,
    dimensionName: String,
    values: List<Long>
) {
    val valuesArray = NativeArray(ctx, values.toLongArray(), Datatype.TILEDB_INT64)
    query.setDataBuffer(dimensionName, valuesArray)
}
