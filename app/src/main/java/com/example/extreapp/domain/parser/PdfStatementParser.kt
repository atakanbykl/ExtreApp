package com.example.extreapp.domain.parser

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

object PdfStatementParser {

    // Matches lines starting with a date like "24.09.2026" or "1.10.2026" or "24/09/2026"
    // followed by description and ending with an amount like "1.555,44 TL" or "-966,50"
    private val pdfTransactionLineRegex = Regex(
        """^\s*(\d{1,2}[./]\d{1,2}[./]\d{2,4})\s+(.+?)\s+([+\-]?\d{1,3}(?:\.\d{3})*,\d{2}(?:\s*(?:TL|TRY|USD|EUR|GBP))?)(?:\s+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    fun parseRawTable(bytes: ByteArray, fileName: String, context: Context? = null): RawStatementTable {
        if (context != null && !PDFBoxResourceLoader.isReady()) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }

        val extractedText = try {
            PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                stripper.getText(document)
            }
        } catch (_: Exception) {
            CharsetDetector.decodeBytes(bytes).content
        }

        return parseExtractedPdfText(extractedText, fileName)
    }

    fun parseExtractedPdfText(text: String, fileName: String): RawStatementTable {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val dataRows = mutableListOf<List<String>>()

        for (line in lines) {
            val match = pdfTransactionLineRegex.find(line)
            if (match != null) {
                val dateStr = match.groupValues[1].trim()
                val descStr = match.groupValues[2].trim()
                val amountStr = match.groupValues[3].trim()
                dataRows.add(listOf(dateStr, descStr, amountStr))
            }
        }

        val mapping = ColumnMapping(
            dateColumnIndex = 0,
            descriptionColumnIndex = 1,
            amountColumnIndex = 2
        )

        return RawStatementTable(
            fileName = fileName,
            detectedCharset = "PDF",
            headers = listOf("Tarih", "Açıklama", "Tutar"),
            dataRows = dataRows,
            autoDetectedMapping = mapping,
            needsColumnMappingWizard = false
        )
    }
}

