package com.example.extreapp.data.local.entity

enum class PeriodFilterMode {
    STATEMENT_PERIOD,   // Hesap Kesim Tarihine göre (statementYearMonth)
    TRANSACTION_MONTH   // İşlem Tarihine göre (yearMonth)
}

enum class DashboardAnalyticsMode {
    OVERVIEW,           // Dönem Özeti (Kategori pasta grafiği, harcamalar)
    COMPARISON,         // Aydan Aya Kıyaslama Modu
    INSTALLMENTS,       // Gelecek Taksit Projeksiyonu & Taksit Takvimi
    RECURRING_PAYMENTS; // Tekrarlanan Ödemeler (Abonelikler & Tekrarlı Harcamalar)

    companion object {
        @JvmField
        val SUBSCRIPTIONS = RECURRING_PAYMENTS
    }
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

enum class RecurringPaymentType(val title: String) {
    SUBSCRIPTION("Abonelikler"),               // Sabit günlü abonelikler (Spotify, Netflix, YouTube...)
    RECURRING_EXPENSE("Tekrarlı Harcamalar")   // Değişken günlü tekrarlanan ödemeler (akaryakıt, spor, market, faturalar...)
}

enum class SubscriptionBillingFrequency(val title: String) {
    MONTHLY("Aylık"),
    YEARLY("Yıllık"),
    WEEKLY("Haftalık"),
    VARIABLE("Değişken / Fatura")
}

data class SubscriptionPriceHistory(
    val dateEpochDay: Long,
    val yearMonth: String,
    val amount: Double
)

data class RecurringPaymentItem(
    val id: String,
    val name: String,
    val rawMerchant: String,
    val category: CategoryEntity?,
    val paymentType: RecurringPaymentType,
    val monthlyAmount: Double,
    val latestAmount: Double,
    val previousAmount: Double?,
    val priceChangePercent: Double?,
    val isPriceIncreased: Boolean,
    val billingFrequency: SubscriptionBillingFrequency,
    val isFixedBillingDay: Boolean,         // true: Abonelik (sabit gün), false: Tekrarlı harcama (değişken gün)
    val paymentDayOfMonth: Int?,            // Abonelikler için sabit gün (örn: 25)
    val lastPaymentDateEpochDay: Long,
    val nextExpectedDateEpochDay: Long?,
    val daysUntilNextBilling: Int?,
    val paymentCount: Int,
    val frequencyDescription: String,       // "Her ayın 25'i (Sabit)" veya "Ayda ~3 kez (Değişken günler)"
    val history: List<SubscriptionPriceHistory>,
    val cardInfo: String?,
    val isKnownService: Boolean,
    val brandColorHex: Long? = null,
    val confidenceScore: Float
)

typealias RecurringSubscriptionItem = RecurringPaymentItem

data class RecurringPaymentsSummary(
    val totalMonthlyCommitment: Double,             // Toplam aylık tekrarlanan harcamalar
    val totalAnnualCommitment: Double,              // Yıllık projeksiyon
    val subscriptionsMonthlyTotal: Double,          // Yalnızca sabit aboneliklerin aylık toplamı
    val recurringExpensesMonthlyTotal: Double,      // Değişken tekrarlı harcamaların aylık toplamı
    val subscriptionsCount: Int,                    // Abonelik sayısı
    val recurringExpensesCount: Int,                // Tekrarlı harcama sayısı
    val totalActiveCount: Int,                      // Toplam aktif kayıt sayısı
    val priceIncreasedCount: Int,                   // Fiyatı artanlar sayısı
    val nextUpcomingPayment: RecurringPaymentItem?, // En yakın beklenen ödeme
    val subscriptions: List<RecurringPaymentItem>,  // Sabit günlü abonelikler listesi
    val recurringExpenses: List<RecurringPaymentItem>, // Değişken günlü tekrarlı harcamalar listesi
    val allItems: List<RecurringPaymentItem>        // Tümü
) {
    val activeSubscriptionsCount: Int get() = totalActiveCount
    val upcomingThisWeekCount: Int get() = allItems.count { it.daysUntilNextBilling != null && it.daysUntilNextBilling in 0..7 }
    val nextUpcomingSubscription: RecurringPaymentItem? get() = nextUpcomingPayment
}

typealias SubscriptionRadarSummary = RecurringPaymentsSummary



