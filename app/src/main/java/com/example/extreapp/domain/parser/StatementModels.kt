package com.example.extreapp.domain.parser

import com.example.extreapp.data.local.entity.TransactionEntity
import kotlinx.serialization.Serializable

@Serializable
data class StatementHeaderMetadata(
    val cardInfo: String? = null,
    val statementDate: String? = null,
    val statementYearMonth: String? = null,
    val statementDebt: String? = null,
    val minimumPayment: String? = null,
    val dueDate: String? = null,
    val totalRewardPoints: String? = null,
    val earnedRewardPoints: String? = null
)

data class ColumnMapping(
    val dateColumnIndex: Int,
    val descriptionColumnIndex: Int,
    val amountColumnIndex: Int,
    val rewardColumnIndex: Int? = null
)

data class RawStatementTable(
    val fileName: String,
    val detectedCharset: String = "UTF-8",
    val headerMetadata: StatementHeaderMetadata = StatementHeaderMetadata(),
    val headers: List<String> = emptyList(),
    val dataRows: List<List<String>> = emptyList(),
    val autoDetectedMapping: ColumnMapping? = null,
    val needsColumnMappingWizard: Boolean = false
)

data class StatementParseResult(
    val fileName: String,
    val detectedCharset: String,
    val metadata: StatementHeaderMetadata,
    val transactions: List<TransactionEntity>,
    val skippedZeroCurrencyRows: Int = 0,
    val skippedFooterRows: Int = 0
)

data class StatementImportReport(
    val fileName: String,
    val detectedCharset: String,
    val metadata: StatementHeaderMetadata,
    val totalParsedCount: Int,
    val newlyInsertedCount: Int,
    val autoMatchedCount: Int,
    val queuedForApprovalCount: Int,
    val duplicateSkippedCount: Int,
    val paymentAndRewardCount: Int
)

