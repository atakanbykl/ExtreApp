package com.example.extreapp.data.local.entity

enum class PeriodFilterMode {
    STATEMENT_PERIOD,   // Hesap Kesim Tarihine göre (statementYearMonth)
    TRANSACTION_MONTH   // İşlem Tarihine göre (yearMonth)
}

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val merchant: MerchantEntity? = null,
    val category: CategoryEntity? = null,
    val matchedRule: RuleEntity? = null
)

data class CategoryBreakdownItem(
    val category: CategoryEntity,
    val grossExpense: Double,
    val refundTotal: Double,
    val netExpense: Double,
    val transactionCount: Int,
    val percentageOfTotal: Float
)

data class MonthlyOverviewSummary(
    val periodKey: String,             // "2026-10"
    val filterMode: PeriodFilterMode,
    val grossExpenseTotal: Double,     // Toplam brüt harcama
    val refundTotal: Double,           // Toplam ürün/taksit iadeleri
    val netExpenseTotal: Double,       // Brüt harcama - İadeler
    val paymentTotal: Double,          // Dönem borç ödemeleri (Örn: 56.602,83 TL)
    val rewardPointsTotalText: String?,
    val unapprovedCount: Int,
    val categoryBreakdowns: List<CategoryBreakdownItem>,
    val transactions: List<TransactionWithDetails>
)

data class MerchantSummaryItem(
    val merchant: MerchantEntity,
    val category: CategoryEntity?,
    val netSpend: Double,
    val grossSpend: Double,
    val refundTotal: Double,
    val transactionCount: Int,
    val lastTransactionEpochDay: Long,
    val ruleKeywords: List<String>
)

data class MerchantMonthlyTotal(
    val yearMonth: String,             // "2026-09", "2026-10"
    val grossSpend: Double,
    val refundTotal: Double,
    val netSpend: Double,
    val transactionCount: Int
)

data class ApprovalQueueGroup(
    val suggestedKeyword: String,
    val suggestedMerchantName: String,
    val sampleRawDescription: String,
    val sampleCleanedDescription: String,
    val transactions: List<TransactionEntity>,
    val totalAmount: Double
)

