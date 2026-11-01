package com.example.extreapp.data.local.entity

enum class PeriodFilterMode {
    STATEMENT_PERIOD,   // Hesap Kesim Tarihine göre (statementYearMonth)
    TRANSACTION_MONTH   // İşlem Tarihine göre (yearMonth)
}

enum class DashboardAnalyticsMode {
    OVERVIEW,           // Dönem Özeti (Kategori pasta grafiği, harcamalar)
    COMPARISON,         // Aydan Aya Kıyaslama Modu
    INSTALLMENTS        // Gelecek Taksit Projeksiyonu & Taksit Takvimi
}

enum class TransactionSortOption(val title: String) {
    DATE_DESC("Tarih (En Yeni)"),
    DATE_ASC("Tarih (En Eski)"),
    AMOUNT_DESC("Fiyat (En Yüksek)"),
    AMOUNT_ASC("Fiyat (En Düşük)");

    val isDate: Boolean get() = this == DATE_DESC || this == DATE_ASC
    val isAmount: Boolean get() = this == AMOUNT_DESC || this == AMOUNT_ASC
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

data class LoadedStatementSummary(
    val fileName: String,
    val transactionCount: Int,
    val periodKeys: List<String>,
    val primaryPeriod: String,
    val grossExpenseTotal: Double,
    val refundTotal: Double,
    val netExpenseTotal: Double,
    val paymentTotal: Double,
    val unapprovedCount: Int,
    val approvedCount: Int,
    val cardInfos: List<String>,
    val minDateEpochDay: Long,
    val maxDateEpochDay: Long
)

data class CategoryComparisonItem(
    val category: CategoryEntity,
    val baseNetExpense: Double,
    val targetNetExpense: Double,
    val diffAmount: Double,                     // base - target (pozitif ise artış, negatif ise azalış/tasarruf)
    val percentageChange: Double?,              // null if targetNetExpense == 0.0
    val baseTransactionCount: Int,
    val targetTransactionCount: Int
)

data class MonthOverMonthComparison(
    val basePeriodKey: String,                 // "2026-10"
    val targetPeriodKey: String,               // "2026-09"
    val filterMode: PeriodFilterMode,
    val baseNetExpense: Double,
    val targetNetExpense: Double,
    val netExpenseDiff: Double,                // base - target
    val netExpensePercentageChange: Double?,
    val baseGrossExpense: Double,
    val targetGrossExpense: Double,
    val baseRefundTotal: Double,
    val targetRefundTotal: Double,
    val basePaymentTotal: Double,
    val targetPaymentTotal: Double,
    val baseTransactionCount: Int,
    val targetTransactionCount: Int,
    val categoryComparisons: List<CategoryComparisonItem>,
    val topIncreasedCategory: CategoryComparisonItem?,
    val topDecreasedCategory: CategoryComparisonItem?
)

data class FutureInstallmentOccurrence(
    val planKey: String,
    val merchantDisplayName: String,
    val category: CategoryEntity?,
    val cardInfo: String?,
    val type: TransactionType,
    val amount: Double,
    val installmentNumber: Int,
    val totalInstallments: Int,
    val yearMonth: String
)

data class ActiveInstallmentPlan(
    val planKey: String,
    val merchantDisplayName: String,
    val rawDescription: String,
    val category: CategoryEntity?,
    val cardInfo: String?,
    val type: TransactionType,
    val monthlyAmount: Double,
    val latestCurrentInstallment: Int,
    val totalInstallments: Int,
    val remainingInstallments: Int,
    val remainingBalance: Double,
    val totalPlanAmount: Double,
    val latestTransactionEpochDay: Long,
    val latestStatementYearMonth: String,
    val endYearMonth: String,
    val isCompleted: Boolean
)

data class MonthlyInstallmentForecast(
    val yearMonth: String,
    val monthLabel: String,
    val grossInstallmentExpense: Double,
    val refundInstallmentTotal: Double,
    val netInstallmentCommitment: Double,
    val activePlansCount: Int,
    val occurrences: List<FutureInstallmentOccurrence>
)

data class InstallmentCashflowProjection(
    val baseStatementYearMonth: String,
    val totalActivePlansCount: Int,
    val totalFutureDebt: Double,
    val nextMonthCommitment: Double,
    val maxEndYearMonth: String?,
    val monthlyForecasts: List<MonthlyInstallmentForecast>,
    val activePlans: List<ActiveInstallmentPlan>,
    val completedPlans: List<ActiveInstallmentPlan>
)



