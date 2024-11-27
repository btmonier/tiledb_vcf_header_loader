package org.btmonier.vcfhload.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import htsjdk.variant.vcf.VCFFileReader
import io.tiledb.java.api.Context
import org.btmonier.vcfhload.utils.createArray
import org.btmonier.vcfhload.utils.parseALTHeader
import org.btmonier.vcfhload.utils.writeArray
import java.io.File


class IngestVcfHeader : CliktCommand() {
    init {
        context {
            helpFormatter = {
                MordantHelpFormatter(
                    it,
                    showRequiredTag = true,
                    showDefaultValues = true
                )
            }
        }
    }

    private val allowedExtensions = listOf(".h.vcf", ".h.vcf.gz")

    private fun hasAllowedExtension(file: File, whiteList: List<String>): Boolean {
        return whiteList.any { ext -> file.name.lowercase().endsWith(ext) }
    }

    private val vcfPath by option("-v", "--vcf-path")
        .file(mustExist = true, canBeDir = false)
        .required()
        .help("Path to hVCF file")
        .validate {
            require(hasAllowedExtension(it, allowedExtensions)) {
                "File must be one of these types: ${allowedExtensions.joinToString(", ")}"
            }
        }

    private val dbPath: String by option("-d", "--db-path")
        .required()
        .help("Output path to TileDB instance")

    override fun run() {
        val head = VCFFileReader(vcfPath, false).fileHeader
        val meta = parseALTHeader(header = head).map { it.value }

        val ctx = Context()
        createArray(ctx, dbPath)
        writeArray(ctx, dbPath, meta)
    }
}

fun main(args: Array<String>) = IngestVcfHeader().main(args)
