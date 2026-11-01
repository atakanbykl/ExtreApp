package com.example.extreapp.domain.parser

import android.content.Context
import java.util.Locale

object StatementParserDispatcher {

    fun parseRawStatement(
        bytes: ByteArray,
        fileName: String,
        context: Context? = null
    ): RawStatementTable {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val isPdfHeader = bytes.size >= 4 &&
            bytes[0] == '%'.code.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()

        val isZipHeader = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()

        return when {
            lowerName.endsWith(".pdf") || isPdfHeader -> {
                PdfStatementParser.parseRawTable(bytes, fileName, context)
            }
            lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || isZipHeader -> {
                ExcelStatementParser.parseRawTable(bytes, fileName)
            }
            else -> {
                CsvStatementParser.parseRawTable(bytes, fileName)
            }
        }
    }

    fun parseAndNormalize(
        bytes: ByteArray,
        fileName: String,
        customMapping: ColumnMapping? = null,
        context: Context? = null
    ): StatementParseResult {
        val rawTable = parseRawStatement(bytes, fileName, context)
        val mapping = customMapping
            ?: rawTable.autoDetectedMapping
            ?: ColumnMapping(0, 1, 2)
        return TransactionNormalizer.normalizeTable(rawTable, mapping)
    }
}

