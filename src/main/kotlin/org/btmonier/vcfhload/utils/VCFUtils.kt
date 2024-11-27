package org.btmonier.vcfhload.utils

import htsjdk.variant.vcf.VCFAltHeaderLine
import htsjdk.variant.vcf.VCFHeader

data class SampleGamete(val name: String, val gameteId: Int = 0) : Comparable<SampleGamete> {
    override fun compareTo(other: SampleGamete): Int {
        val namesEqual = name.compareTo(other.name)
        return if (namesEqual == 0) gameteId.compareTo(other.gameteId) else namesEqual
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SampleGamete) {
            name == other.name && gameteId == other.gameteId
        } else false
    }

    override fun toString(): String {
        return "$name:$gameteId"
    }

    override fun hashCode(): Int {
        return name.hashCode() + gameteId.hashCode()
    }
}

data class Position (val contig: String, val position: Int) : Comparable<Position> {
    override fun compareTo(other: Position): Int {
        if (this.contig == other.contig) {
            return this.position.compareTo(other.position)
        }
        return this.contig.compareTo(other.contig)
    }

    override fun toString(): String {
        return "$contig:$position"
    }
}

data class AltHeaderMetaData(
    val id: String, val description: String, val source: String, val sampleGamete: SampleGamete,
    val regions: List<Pair<Position, Position>>, val checksum: String, val refRange: String,
    val refChecksum: String = ""
) {
    fun sampleName() = sampleGamete.name
    fun gamete() = sampleGamete.gameteId
}

/**
 * Helper function to parse out the ALT headers from a VCF file.
 *
 * We need to do a bit more involved parsing in this function as we cannot use the .getOtherHeaders() call from HTSJDK.
 * For some reason this only returns the first header when called, and we need all of them.
 * The workaround is that we can get all the metadata, filter out any that are not ALT then parse the ALT header using normal string parsing.
 * To make this easy, we just parse each piece of metadata into a key-value pair and then store in a map.
 */
fun parseALTHeader(header: VCFHeader): Map<String, AltHeaderMetaData> {
    return parseALTHeader(listOf(header))
}

/**
 * Helper function to parse out the ALT headers from a VCF file.
 *
 * This function is used when we have multiple VCF headers to parse.
 * And the results are added to an existing map.
 */
fun parseALTHeader(header: VCFHeader, result: MutableMap<String, AltHeaderMetaData>) {

    header.metaDataInInputOrder.asSequence().filter { it.key == "ALT" }
        .map { it as VCFAltHeaderLine }
        .filter { !result.containsKey(it.id) }
        .map { it.genericFields }
        .associateBy { it["ID"]!! }
        .forEach {
            check(it.value.containsKey("ID")) { "ALT Header does not contain ID" }
            check(it.value.containsKey("Description")) { "ALT Header does not contain Description" }
            // These are optional header fields, so we check these in the unit test.
            check(it.value.containsKey("Source")) { "ALT Header does not contain Source" }
            check(it.value.containsKey("SampleName")) { "ALT Header does not contain SampleName" }
            check(it.value.containsKey("Regions")) { "ALT Header does not contain Regions" }
            check(it.value.containsKey("Checksum")) { "ALT Header does not contain Checksum" }
            check(it.value.containsKey("RefRange")) { "ALT Header does not contain RefRange" }
            result[it.key] = AltHeaderMetaData(
                it.value["ID"]!!,
                it.value["Description"]!!,
                it.value["Source"]!!,
                SampleGamete(it.value["SampleName"]!!, it.value["Gamete"]?.toInt() ?: 0),
                parseRegions(it.value["Regions"]!!),
                it.value["Checksum"]!!,
                it.value["RefRange"]!!,
                it.value["RefChecksum"] ?: ""
            )
        }

}

/**
 * Helper function to parse out the ALT headers from multiple VCF files.
 */
fun parseALTHeader(headers: List<VCFHeader>): Map<String, AltHeaderMetaData> {

    return headers.asSequence().map { header -> header.metaDataInInputOrder.asSequence().filter { it.key == "ALT" } }
        .flatten()
        .map { it as VCFAltHeaderLine }
        .distinctBy { it.id }
        .map { it.genericFields }
        .associateBy { it["ID"]!! }
        .map {
            check(it.value.containsKey("ID")) { "ALT Header does not contain ID" }
            check(it.value.containsKey("Description")) { "ALT Header does not contain Description" }
            // These are optional header fields, so we check these in the unit test.
            check(it.value.containsKey("Source")) { "ALT Header does not contain Source" }
            check(it.value.containsKey("SampleName")) { "ALT Header does not contain SampleName" }
            check(it.value.containsKey("Regions")) { "ALT Header does not contain Regions" }
            check(it.value.containsKey("Checksum")) { "ALT Header does not contain Checksum" }
            check(it.value.containsKey("RefRange")) { "ALT Header does not contain RefRange" }
            it.key to AltHeaderMetaData(
                it.value["ID"]!!,
                it.value["Description"]!!,
                it.value["Source"]!!,
                SampleGamete(it.value["SampleName"]!!, it.value["Gamete"]?.toInt() ?: 0),
                parseRegions(it.value["Regions"]!!),
                it.value["Checksum"]!!,
                it.value["RefRange"]!!,
                it.value["RefChecksum"] ?: ""
            )
        }.toMap()

}

/**
 * Function to parse the regions from the ALT header.
 */
fun parseRegions(regions: String): List<Pair<Position, Position>> {
    return regions.split(",").map { it.split(":") }.map {
        val positions = it[1].split("-").map { position -> position.toInt() }
        check(positions.size == 2) { "Region $it is not in the correct format.  It needs to be in the form: chr:stPos-endPos." }
        Pair(Position(it[0], positions[0]), Position(it[0], positions[1]))
    }
}