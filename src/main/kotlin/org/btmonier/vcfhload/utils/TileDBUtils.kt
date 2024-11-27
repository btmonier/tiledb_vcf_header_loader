package org.btmonier.vcfhload.utils

import io.tiledb.java.api.*
import io.tiledb.java.api.Array
import io.tiledb.java.api.Constants.TILEDB_VAR_NUM


fun createArray(ctx: Context, uri: String) {
    // Create dimensions
    val d1 = Dimension(ctx, "contig", Datatype.TILEDB_STRING_ASCII, null, null)
    val d2 = Dimension(ctx, "start", Datatype.TILEDB_INT64, Pair(1L, 400L), 1L)

    // Create domain
    val domain = Domain(ctx)
    domain.addDimension(d1)
    domain.addDimension(d2)

    // Add attributes
    val attA = Attribute(ctx, "a1", Datatype.TILEDB_INT32)
    attA.setCellValNum(1L)
    val attB = Attribute(ctx, "hash", Datatype.TILEDB_STRING_ASCII)
    attB.setCellValNum(TILEDB_VAR_NUM)

    val schema: ArraySchema = ArraySchema(ctx, ArrayType.TILEDB_SPARSE)
    schema.setTileOrder(Layout.TILEDB_ROW_MAJOR)
    schema.setCellOrder(Layout.TILEDB_ROW_MAJOR)
    schema.capacity = 2
    schema.domain = domain
    schema.addAttribute(attA)
    schema.addAttribute(attB)

    schema.check()
    schema.dump()

    // Create the array
    Array.create(uri, schema)
}

fun writeArray(ctx: Context, uri: String) {
    val arr = Array(ctx, uri, QueryType.TILEDB_WRITE)

    val query = Query(arr)

    // Set string dim ("contig")
    val contigValues = NativeArray(ctx, arrayOf("chr1", "chr1", "chr2", "chr3", "chr1").joinToString(""), Datatype.TILEDB_STRING_ASCII)
    val contigOffset = NativeArray(ctx, longArrayOf(0, 4, 8, 12, 16), Datatype.TILEDB_UINT64)
    query.setDataBuffer("contig", contigValues)
    query.setOffsetsBuffer("contig", contigOffset)

    // Set int dim ("start")
    val startValues = NativeArray(ctx, longArrayOf(100, 200, 300, 400, 300), Datatype.TILEDB_INT64)
    query.setDataBuffer("start", startValues)

    // Set attribute data ("a1")
    val attrValues = NativeArray(ctx, intArrayOf(4, 8, 12, 16, 42), Datatype.TILEDB_INT32)
    query.setDataBuffer("a1", attrValues)

    // Set attribute data ("hash")
    val hashValues = NativeArray(ctx, arrayOf(
        "abc123", "def456", "ghi789", "jkl012", "jjjjjj"
    ).joinToString(""), Datatype.TILEDB_STRING_ASCII)
    val hashOffset = NativeArray(ctx, longArrayOf(0, 6, 12, 18, 24), Datatype.TILEDB_UINT64)
    query.setDataBuffer("hash", hashValues)
    query.setOffsetsBuffer("hash", hashOffset)

    query.submit()
    query.close()

}