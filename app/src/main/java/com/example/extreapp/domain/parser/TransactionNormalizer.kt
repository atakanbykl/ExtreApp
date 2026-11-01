package com.example.extreapp.domain.parser

import com.example.extreapp.data.local.entity.TransactionEntity
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.domain.engine.SmartKeywordExtractor
import com.example.extreapp.domain.engine.TurkishTextNormalizer
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object TransactionNormalizer {
    private val supportedDateFormatters = listOf(
        DateTimeFormatter.ofPattern("d.M.yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("d.M.yy"),
        DateTimeFormatter.ofPattern("dd.MM.yy")
    )

    private val installmentRegex = Regex("""\(\s*0?(\d{1,2})\s*/\s*0?(\d{1,2})\s*\)""")
    private val installmentAltRegex = Regex("""(?i)\b0?(\d{1,2})\s*/\s*0?(\d{1,2})\s*TAKS[İI]T\b""")
    private val foreignExchangeLineRegex = Regex("""\b(\d+[.,]\d{2})\s+(USD|EUR|GBP|CHF|JPY|CAD|AUD)\b""", RegexOption.IGNORE_CASE)

    fun parseTurkishDate(raw: String): LocalDate? {
        val cleaned = raw.trim().replace(Regex("""\s+"""), "")
        if (cleaned.isEmpty()) return null
        for (formatter in supportedDateFormatters) {
            try {
                val parsed = LocalDate.parse(cleaned, formatter)
                if (parsed.year in 2000..2100) {
                    return parsed
                }
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }

    /**
     * Parses Turkish / international currency amount strings such as:
     * - "1.555,44 TL" -> 1555.44
     * - "-966,50 TL"  -> -966.50
     * - "-56.602,83 TL" -> -56602.83
     * - "0,00 TL" -> 0.0
     * - "1,234.56" -> 1234.56
     */
    fun parseAmount(raw: String): Pair<Double, String>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val currency = when {
            trimmed.contains("USD", ignoreCase = true) || trimmed.contains("$") -> "USD"
            trimmed.contains("EUR", ignoreCase = true) || trimmed.contains("€") -> "EUR"
            trimmed.contains("GBP", ignoreCase = true) || trimmed.contains("£") -> "GBP"
            else -> "TRY"
        }

        // Keep only digits, '-', '+', '.', ','
        var numStr = trimmed.replace(Regex("""[^0-9+\-.,]"""), "")
        if (numStr.isEmpty() || numStr == "-" || numStr == "+") return null

        // Handle trailing minus sign like "966,50-"
        val isNegative = numStr.startsWith("-") || numStr.endsWith("-") || trimmed.contains("(Borç)", ignoreCase = false)
        numStr = numStr.replace("-", "").replace("+", "")

        val lastComma = numStr.lastIndexOf(',')
        val lastDot = numStr.lastIndexOf('.')

        val normalizedNumber = when {
            lastComma > lastDot -> {
                // Turkish / European format: 1.555,44 -> remove '.', replace ',' with '.'
                numStr.replace(".", "").replace(",", ".")
            }
            lastDot > lastComma && lastComma != -1 -> {
                // US format: 1,555.44 -> remove ','
                numStr.replace(",", "")
            }
            lastDot != -1 && lastComma == -1 -> {
                // Only dots: if multiple dots or 3 digits after dot with no cents, check carefully
                val parts = numStr.split(".")
                if (parts.size > 2) {
                    numStr.replace(".", "")
                } else {
                    numStr
                }
            }
            else -> numStr
        }

        val value = normalizedNumber.toDoubleOrNull() ?: return null
        val signedValue = if (isNegative && value > 0) -value else value
        return signedValue to currency
    }

    fun extractInstallment(description: String): Pair<Int, Int>? {
        val match = installmentRegex.find(description) ?: installmentAltRegex.find(description) ?: return null
        val current = match.groupValues[1].toIntOrNull() ?: return null
        val total = match.groupValues[2].toIntOrNull() ?: return null
        if (current in 1..60 && total in 1..60 && current <= total) {
            return current to total
        }
        return null
    }

    fun classifyTransactionType(rawDescription: String, signedAmount: Double, rewardText: String?): TransactionType {
        val normDesc = TurkishTextNormalizer.normalize(rawDescription)

        // 0,00 TL rows
        if (abs(signedAmount) < 0.0001) {
            return TransactionType.REWARD_INFO
        }

        // Check if this is a statement debt payment ("İNTERNET Şb-Ödemeniz için Teşekkürler", "Borç Ödeme", etc.)
        val isPaymentKeyword = normDesc.contains("odemeniz icin tesekkurler") ||
            normDesc.contains("borc odeme") ||
            normDesc.contains("ekstre odemesi") ||
            normDesc.contains("otomatik odeme tahsilati") ||
            normDesc.contains("hesaptan aktarim") ||
            normDesc.contains("kredi karti odeme")

        if (isPaymentKeyword && signedAmount < 0) {
            return TransactionType.PAYMENT
        }

        // Check if this is a refund
        val isRefundKeyword = normDesc.contains("- iade") ||
            normDesc.startsWith("iade-") ||
            normDesc.startsWith("iade ") ||
            normDesc.contains("iptal/iade")

        if (signedAmount < 0 || isRefundKeyword) {
            return TransactionType.REFUND
        }

        return TransactionType.EXPENSE
    }

    fun normalizeTable(
        table: RawStatementTable,
        mapping: ColumnMapping
    ): StatementParseResult {
        val parsedTransactions = mutableListOf<TransactionEntity>()
        val occurrenceCounter = mutableMapOf<String, Int>()
        var skippedZeroCurrencyRows = 0
        var skippedFooterRows = 0

        // Pending foreign currency note from a 0,00 TL info line (e.g., "10/02/26 44.48 USD USA")
        var pendingForeignCurrencyNote: Pair<LocalDate, String>? = null

        val fallbackStatementYearMonth = table.headerMetadata.statementYearMonth

        for (row in table.dataRows) {
            val maxIdx = maxOf(mapping.dateColumnIndex, mapping.descriptionColumnIndex, mapping.amountColumnIndex)
            if (row.size <= maxIdx) {
                skippedFooterRows++
                continue
            }

            val dateStr = row.getOrNull(mapping.dateColumnIndex)?.trim().orEmpty()
            val descStr = row.getOrNull(mapping.descriptionColumnIndex)?.trim().orEmpty()
            val amountStr = row.getOrNull(mapping.amountColumnIndex)?.trim().orEmpty()
            val rewardStr = mapping.rewardColumnIndex?.let { row.getOrNull(it)?.trim() }

            val date = parseTurkishDate(dateStr)
            val amountPair = parseAmount(amountStr)

            if (date == null || amountPair == null || descStr.isEmpty()) {
                skippedFooterRows++
                continue
            }

            val (signedAmount, currency) = amountPair

            // Check if this is a 0,00 TL row
            if (abs(signedAmount) < 0.0001) {
                val fxMatch = foreignExchangeLineRegex.find(descStr)
                if (fxMatch != null) {
                    val fxAmount = fxMatch.groupValues[1].replace(',', '.')
                    val fxCurr = fxMatch.groupValues[2].uppercase(Locale.ROOT)
                    pendingForeignCurrencyNote = date to "$fxAmount $fxCurr"
                    skippedZeroCurrencyRows++
                    continue
                }

                // Check if it has non-zero Chip-Para / reward info worth keeping or if we skip 0,00 TL rows from expenses
                val cleanReward = parseNonZeroRewardText(rewardStr)
                if (cleanReward == null) {
                    skippedZeroCurrencyRows++
                    continue
                }

                val yearMonth = String.format(Locale.ROOT, "%04d-%02d", date.year, date.monthValue)
                val stmtYearMonth = fallbackStatementYearMonth ?: yearMonth
                val suggestion = SmartKeywordExtractor.analyze(descStr)
                val baseKey = "${date.toEpochDay()}|${TurkishTextNormalizer.normalize(descStr)}|0.00|REWARD|$cleanReward"
                val occurrence = (occurrenceCounter[baseKey] ?: 0) + 1
                occurrenceCounter[baseKey] = occurrence

                parsedTransactions.add(
                    TransactionEntity(
                        dateEpochDay = date.toEpochDay(),
                        yearMonth = yearMonth,
                        statementYearMonth = stmtYearMonth,
                        rawDescription = descStr,
                        cleanedDescription = suggestion.cleanedDescription,
                        amount = 0.0,
                        signedAmount = 0.0,
                        currency = currency,
                        rewardPointsText = cleanReward,
                        type = TransactionType.REWARD_INFO,
                        cardInfo = table.headerMetadata.cardInfo,
                        isApproved = true,
                        sourceFileName = table.fileName,
                        dedupHash = sha256("$baseKey|#$occurrence")
                    )
                )
                continue
            }

            val type = classifyTransactionType(descStr, signedAmount, rewardStr)
            val installment = extractInstallment(descStr)
            val suggestion = SmartKeywordExtractor.analyze(descStr)
            val yearMonth = String.format(Locale.ROOT, "%04d-%02d", date.year, date.monthValue)
            val stmtYearMonth = fallbackStatementYearMonth ?: yearMonth

            // Attach pending foreign currency note if on the same date
            val fxNote = if (pendingForeignCurrencyNote != null && pendingForeignCurrencyNote.first == date) {
                val note = pendingForeignCurrencyNote.second
                pendingForeignCurrencyNote = null
                note
            } else {
                null
            }

            val cleanReward = parseNonZeroRewardText(rewardStr)
            val formattedSigned = String.format(Locale.ROOT, "%.2f", signedAmount)
            val installmentKey = installment?.let { "${it.first}/${it.second}" } ?: ""
            val baseKey = "${date.toEpochDay()}|${TurkishTextNormalizer.normalize(descStr)}|$formattedSigned|$installmentKey"
            val occurrence = (occurrenceCounter[baseKey] ?: 0) + 1
            occurrenceCounter[baseKey] = occurrence

            parsedTransactions.add(
                TransactionEntity(
                    dateEpochDay = date.toEpochDay(),
                    yearMonth = yearMonth,
                    statementYearMonth = stmtYearMonth,
                    rawDescription = descStr,
                    cleanedDescription = suggestion.cleanedDescription,
                    amount = abs(signedAmount),
                    signedAmount = signedAmount,
                    currency = currency,
                    foreignCurrencyNote = fxNote,
                    rewardPointsText = cleanReward,
                    type = type,
                    installmentCurrent = installment?.first,
                    installmentTotal = installment?.second,
                    cardInfo = table.headerMetadata.cardInfo,
                    isApproved = (type == TransactionType.PAYMENT || type == TransactionType.REWARD_INFO),
                    sourceFileName = table.fileName,
                    dedupHash = sha256("$baseKey|#$occurrence")
                )
            )
        }

        // If statementYearMonth wasn't in metadata, infer the most common yearMonth across parsed rows
        val finalTransactions = if (fallbackStatementYearMonth == null && parsedTransactions.isNotEmpty()) {
            val dominantYearMonth = parsedTransactions
                .groupingBy { it.yearMonth }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: parsedTransactions.first().yearMonth
            parsedTransactions.map { it.copy(statementYearMonth = dominantYearMonth) }
        } else {
            parsedTransactions
        }

        val finalMetadata = if (table.headerMetadata.statementYearMonth == null && finalTransactions.isNotEmpty()) {
            table.headerMetadata.copy(statementYearMonth = finalTransactions.first().statementYearMonth)
        } else {
            table.headerMetadata
        }

        return StatementParseResult(
            fileName = table.fileName,
            detectedCharset = table.detectedCharset,
            metadata = finalMetadata,
            transactions = finalTransactions,
            skippedZeroCurrencyRows = skippedZeroCurrencyRows,
            skippedFooterRows = skippedFooterRows
        )
    }

    private fun parseNonZeroRewardText(rewardRaw: String?): String? {
        if (rewardRaw.isNullOrBlank()) return null
        // Example in HESAP_OZETI.csv: "0 TL / 0" or "200,00 TL / 0" or "14,96 TL / 0"
        val firstPart = rewardRaw.split("/").firstOrNull()?.trim() ?: return null
        val parsed = parseAmount(firstPart) ?: return null
        return if (abs(parsed.first) > 0.001) firstPart else null
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

