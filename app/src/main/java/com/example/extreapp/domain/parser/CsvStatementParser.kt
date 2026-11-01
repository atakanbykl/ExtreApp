package com.example.extreapp.domain.parser

import com.example.extreapp.domain.engine.TurkishTextNormalizer
import java.util.Locale

object CsvStatementParser {

    fun parseRawTable(bytes: ByteArray, fileName: String): RawStatementTable {
        val decoded = CharsetDetector.decodeBytes(bytes)
        return parseFromText(decoded.content, fileName, decoded.charsetName)
    }

    fun parseFromText(content: String, fileName: String, charsetName: String = "UTF-8"): RawStatementTable {
        val rawLines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) {
            return RawStatementTable(fileName = fileName, detectedCharset = charsetName)
        }

        val delimiter = detectDelimiter(rawLines)
        val tokenizedLines = rawLines.map { splitCsvLine(it, delimiter) }

        var cardInfo: String? = null
        var statementDate: String? = null
        var statementYearMonth: String? = null
        var statementDebt: String? = null
        var minimumPayment: String? = null
        var dueDate: String? = null
        var totalRewardPoints: String? = null
        var earnedRewardPoints: String? = null

        var headerRowIndex = -1
        var detectedMapping: ColumnMapping? = null

        for ((index, tokens) in tokenizedLines.withIndex()) {
            val mappingCandidate = tryDetectHeaderRow(tokens)
            if (mappingCandidate != null) {
                headerRowIndex = index
                detectedMapping = mappingCandidate
                break
            }

            // If before header row, inspect Key;Value metadata pairs (like lines 1-8 of HESAP_OZETI.csv)
            if (tokens.size >= 2) {
                val keyNorm = TurkishTextNormalizer.normalize(tokens[0])
                val value = tokens[1].trim()
                if (value.isNotEmpty()) {
                    when {
                        keyNorm.contains("kart turu") || keyNorm.contains("kart no") -> cardInfo = value
                        keyNorm.contains("hesap kesim") -> {
                            statementDate = value
                            val parsedDate = TransactionNormalizer.parseTurkishDate(value)
                            if (parsedDate != null) {
                                statementYearMonth = String.format(
                                    Locale.ROOT,
                                    "%04d-%02d",
                                    parsedDate.year,
                                    parsedDate.monthValue
                                )
                            }
                        }
                        keyNorm.contains("ekstre borcu") || keyNorm.contains("donem borcu") -> statementDebt = value
                        keyNorm.contains("asgari odeme") -> minimumPayment = value
                        keyNorm.contains("son odeme") -> dueDate = value
                        keyNorm.contains("toplam chip") || keyNorm.contains("toplam puan") -> totalRewardPoints = value
                        keyNorm.contains("kazanilan chip") || keyNorm.contains("kazanilan puan") -> earnedRewardPoints = value
                    }
                }
            }
        }

        val metadata = StatementHeaderMetadata(
            cardInfo = cardInfo,
            statementDate = statementDate,
            statementYearMonth = statementYearMonth,
            statementDebt = statementDebt,
            minimumPayment = minimumPayment,
            dueDate = dueDate,
            totalRewardPoints = totalRewardPoints,
            earnedRewardPoints = earnedRewardPoints
        )

        if (headerRowIndex != -1 && detectedMapping != null) {
            val headers = tokenizedLines[headerRowIndex]
            val dataRows = tokenizedLines.drop(headerRowIndex + 1)
            return RawStatementTable(
                fileName = fileName,
                detectedCharset = charsetName,
                headerMetadata = metadata,
                headers = headers,
                dataRows = dataRows,
                autoDetectedMapping = detectedMapping,
                needsColumnMappingWizard = false
            )
        }

        // Fallback: heuristic detection for custom bank CSVs without standard header labels
        val firstTransactionRowIdx = tokenizedLines.indexOfFirst { row ->
            row.size >= 3 && row.any { TransactionNormalizer.parseTurkishDate(it) != null }
        }
        val startIdx = if (firstTransactionRowIdx > 0) firstTransactionRowIdx - 1 else 0
        val headers = if (firstTransactionRowIdx > 0) {
            tokenizedLines[firstTransactionRowIdx - 1]
        } else {
            val colCount = tokenizedLines.maxOfOrNull { it.size } ?: 3
            (1..colCount).map { "Sütun $it" }
        }
        val dataRows = if (firstTransactionRowIdx >= 0) {
            tokenizedLines.drop(firstTransactionRowIdx)
        } else {
            tokenizedLines.drop(startIdx)
        }
        val heuristicMapping = inferMappingFromSampleRows(dataRows)

        return RawStatementTable(
            fileName = fileName,
            detectedCharset = charsetName,
            headerMetadata = metadata,
            headers = headers,
            dataRows = dataRows,
            autoDetectedMapping = heuristicMapping,
            needsColumnMappingWizard = true
        )
    }

    fun tryDetectHeaderRow(tokens: List<String>): ColumnMapping? {
        if (tokens.size < 3) return null
        val normalized = tokens.map { TurkishTextNormalizer.normalize(it) }

        val dateIdx = normalized.indexOfFirst {
            it == "tarih" || it == "islem tarihi" || it == "date" || it.startsWith("tarih")
        }
        val descIdx = normalized.indexOfFirst {
            it == "aciklama" || it == "islem aciklamasi" || it == "islem" || it == "description" || it.contains("aciklama")
        }
        val amountIdx = normalized.indexOfFirst {
            it == "tutar" || it == "islem tutari" || it == "tutar (tl)" || it == "amount" || it.startsWith("tutar")
        }
        val rewardIdx = normalized.indexOfFirst {
            it.contains("chip para") || it.contains("puan") || it.contains("mil")
        }.takeIf { it >= 0 }

        if (dateIdx != -1 && descIdx != -1 && amountIdx != -1 &&
            dateIdx != descIdx && descIdx != amountIdx && dateIdx != amountIdx
        ) {
            return ColumnMapping(
                dateColumnIndex = dateIdx,
                descriptionColumnIndex = descIdx,
                amountColumnIndex = amountIdx,
                rewardColumnIndex = rewardIdx
            )
        }
        return null
    }

    fun inferMappingFromSampleRows(rows: List<List<String>>): ColumnMapping? {
        val sample = rows.firstOrNull { it.size >= 3 } ?: return null
        var dateIdx = -1
        var amountIdx = -1
        var descIdx = -1

        for ((i, cell) in sample.withIndex()) {
            if (dateIdx == -1 && TransactionNormalizer.parseTurkishDate(cell) != null) {
                dateIdx = i
            } else if (amountIdx == -1 && TransactionNormalizer.parseAmount(cell) != null && cell.any { it.isDigit() }) {
                amountIdx = i
            }
        }
        for (i in sample.indices) {
            if (i != dateIdx && i != amountIdx) {
                descIdx = i
                break
            }
        }
        if (dateIdx == -1) dateIdx = 0
        if (descIdx == -1) descIdx = 1.coerceAtMost(sample.lastIndex)
        if (amountIdx == -1) amountIdx = 2.coerceAtMost(sample.lastIndex)

        return ColumnMapping(
            dateColumnIndex = dateIdx,
            descriptionColumnIndex = descIdx,
            amountColumnIndex = amountIdx
        )
    }

    private fun detectDelimiter(lines: List<String>): Char {
        val sample = lines.take(20)
        val semicolonCount = sample.sumOf { line -> line.count { it == ';' } }
        val commaCount = sample.sumOf { line -> line.count { it == ',' } }
        val tabCount = sample.sumOf { line -> line.count { it == '\t' } }

        return when {
            semicolonCount >= commaCount && semicolonCount >= tabCount && semicolonCount > 0 -> ';'
            tabCount > commaCount && tabCount > semicolonCount -> '\t'
            else -> ','
        }
    }

    fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        val lastToken = current.toString().trim()
        // If line ends with a trailing semicolon (like in HESAP_OZETI.csv), don't add an empty trailing cell
        if (lastToken.isNotEmpty() || !line.trimEnd().endsWith(delimiter)) {
            result.add(lastToken)
        }
        return result
    }
}

