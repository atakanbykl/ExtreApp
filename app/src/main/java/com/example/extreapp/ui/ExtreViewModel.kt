package com.example.extreapp.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.extreapp.data.local.ExtreSqliteHelper
import com.example.extreapp.data.local.SecurityPreferences
import com.example.extreapp.data.local.SharedPreferencesSecurityPreferences
import com.example.extreapp.data.local.entity.ApprovalQueueGroup
import com.example.extreapp.data.local.entity.BiometricAvailability
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.DashboardAnalyticsMode
import com.example.extreapp.data.local.entity.InstallmentCashflowProjection
import com.example.extreapp.data.local.entity.LoadedStatementSummary
import com.example.extreapp.data.local.entity.LockTimeout
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.MerchantSummaryItem
import com.example.extreapp.data.local.entity.MonthOverMonthComparison
import com.example.extreapp.data.local.entity.MonthlyOverviewSummary
import com.example.extreapp.data.local.entity.PeriodFilterMode
import com.example.extreapp.data.local.entity.RecurringPaymentType
import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.data.local.entity.SubscriptionRadarSummary
import com.example.extreapp.data.local.entity.TransactionSortOption
import com.example.extreapp.data.repository.ExtreRepository
import com.example.extreapp.data.repository.MerchantDetailData
import com.example.extreapp.domain.engine.RuleMatchCandidate
import com.example.extreapp.domain.engine.RuleSimilarityAnalyzer
import com.example.extreapp.domain.parser.ColumnMapping
import com.example.extreapp.domain.parser.RawStatementTable
import com.example.extreapp.domain.parser.StatementHeaderMetadata
import com.example.extreapp.domain.parser.StatementImportReport
import com.example.extreapp.domain.parser.StatementParserDispatcher
import com.example.extreapp.domain.parser.TransactionNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
    OVERVIEW,       // 📊 Genel Bakış (Dönem Özeti, Donut, Kategoriler, İşlemler)
    PLANNING,       // 📅 Gelecek & Planlama (Taksit Takvimi, Tekrarlanan Ödemeler & Abonelikler)
    INSIGHTS,       // 📈 Trend & Satıcılar (Aydan Aya Kıyasla, Satıcı Analitiği)
    MANAGEMENT      // ⚙️ Yönetim (Ekstre Yönetimi, Onay Kuyruğu, Kurallar, Kategoriler, Yedek, Güvenlik)
}

enum class PlanningTabMode {
    INSTALLMENTS,   // Taksit Takvimi
    RECURRING       // Tekrarlanan Ödemeler & Abonelikler
}

enum class InsightsTabMode {
    COMPARISON,     // Aydan Aya Kıyasla
    MERCHANTS       // Satıcı Analitiği
}

enum class ManagementSection {
    STATEMENTS,     // Ekstre Dosyaları Yönetimi
    APPROVAL_QUEUE, // Tanımsız İşlemleri Onaylama Kuyruğu
    RULES,          // Kategori Kuralları
    CATEGORIES,     // Kategori Tanımları
    SECURITY,       // Biyometri ve Ekran Güvenliği
    BACKUP          // JSON Dışa/İçe Aktar ve Sıfırla
}

data class ExtreUiState(
    val activeTab: AppTab = AppTab.OVERVIEW,
    val planningTabMode: PlanningTabMode = PlanningTabMode.INSTALLMENTS,
    val insightsTabMode: InsightsTabMode = InsightsTabMode.COMPARISON,
    val managementSection: ManagementSection? = null,
    val isPrivacyModeEnabled: Boolean = false,
    val isBiometricLockEnabled: Boolean = false,
    val isScreenProtectionEnabled: Boolean = true,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATE,
    val isAppLocked: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    val dashboardMode: DashboardAnalyticsMode = DashboardAnalyticsMode.OVERVIEW,
    val periodFilterMode: PeriodFilterMode = PeriodFilterMode.STATEMENT_PERIOD,
    val availablePeriods: List<String> = emptyList(),
    val selectedPeriod: String = "2026-10",
    val selectedCategoryIdFilter: Long? = null,
    val transactionSortOption: TransactionSortOption = TransactionSortOption.DATE_DESC,
    val monthlyOverview: MonthlyOverviewSummary? = null,
    val isComparisonModeActive: Boolean = false,
    val comparisonTargetPeriod: String? = null,
    val monthOverMonthComparison: MonthOverMonthComparison? = null,
    val selectedForecastMonth: String? = null,
    val installmentProjection: InstallmentCashflowProjection? = null,
    val subscriptionRadar: SubscriptionRadarSummary? = null,
    val selectedRecurringTabFilter: RecurringPaymentType? = null,
    val approvalQueueGroups: List<ApprovalQueueGroup> = emptyList(),
    val unapprovedTotalCount: Int = 0,
    val merchantSearchQuery: String = "",
    val merchantSummaries: List<MerchantSummaryItem> = emptyList(),
    val selectedMerchantId: Long? = null,
    val selectedMerchantDetail: MerchantDetailData? = null,
    val statementSearchQuery: String = "",
    val loadedStatements: List<LoadedStatementSummary> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val merchants: List<MerchantEntity> = emptyList(),
    val rules: List<RuleEntity> = emptyList(),
    val ruleSimilarityCandidatesMap: Map<Long, List<RuleMatchCandidate>> = emptyMap(),
    val totalTransactionCount: Int = 0,
    val latestStatementMetadata: StatementHeaderMetadata? = null,
    val pendingWizardTable: RawStatementTable? = null,
    val lastImportReport: StatementImportReport? = null,
    val bannerMessage: String? = null
)

private data class UiControlState(
    val activeTab: AppTab = AppTab.OVERVIEW,
    val planningTabMode: PlanningTabMode = PlanningTabMode.INSTALLMENTS,
    val insightsTabMode: InsightsTabMode = InsightsTabMode.COMPARISON,
    val managementSection: ManagementSection? = null,
    val isPrivacyModeEnabled: Boolean = false,
    val isBiometricLockEnabled: Boolean = false,
    val isScreenProtectionEnabled: Boolean = true,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATE,
    val isAppLocked: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    val dashboardMode: DashboardAnalyticsMode = DashboardAnalyticsMode.OVERVIEW,
    val periodFilterMode: PeriodFilterMode = PeriodFilterMode.STATEMENT_PERIOD,
    val selectedPeriod: String? = null,
    val selectedCategoryIdFilter: Long? = null,
    val transactionSortOption: TransactionSortOption = TransactionSortOption.DATE_DESC,
    val isComparisonModeActive: Boolean = false,
    val comparisonTargetPeriod: String? = null,
    val selectedForecastMonth: String? = null,
    val selectedRecurringTabFilter: RecurringPaymentType? = null,
    val merchantSearchQuery: String = "",
    val selectedMerchantId: Long? = null,
    val statementSearchQuery: String = "",
    val pendingWizardTable: RawStatementTable? = null,
    val lastImportReport: StatementImportReport? = null,
    val bannerMessage: String? = null
)

class ExtreViewModel(
    private val repository: ExtreRepository,
    private val securityPreferences: SecurityPreferences? = null
) : ViewModel() {

    private val controlState = MutableStateFlow(
        UiControlState(
            isPrivacyModeEnabled = securityPreferences?.isPrivacyModeEnabled ?: false,
            isBiometricLockEnabled = securityPreferences?.isBiometricLockEnabled ?: false,
            isScreenProtectionEnabled = securityPreferences?.isScreenProtectionEnabled ?: true,
            lockTimeout = securityPreferences?.lockTimeout ?: LockTimeout.IMMEDIATE,
            isAppLocked = securityPreferences?.isBiometricLockEnabled ?: false
        )
    )

    // Cache references to avoid recomputing when underlying data has not changed
    @Volatile private var cachedSnapshot: com.example.extreapp.data.repository.DatabaseSnapshot? = null
    @Volatile private var cachedPeriodMode: PeriodFilterMode? = null
    @Volatile private var cachedPeriods: List<String> = emptyList()
    @Volatile private var cachedPeriodKey: String? = null
    @Volatile private var cachedOverview: MonthlyOverviewSummary? = null
    @Volatile private var cachedQueueGroups: List<ApprovalQueueGroup> = emptyList()
    @Volatile private var cachedSearchQuery: String = ""
    @Volatile private var cachedMerchantSummaries: List<MerchantSummaryItem> = emptyList()
    @Volatile private var cachedSelectedMerchantId: Long? = null
    @Volatile private var cachedMerchantDetail: MerchantDetailData? = null
    @Volatile private var cachedStatements: List<LoadedStatementSummary>? = null
    @Volatile private var cachedStatementSearch: String = ""
    @Volatile private var cachedFilteredStatements: List<LoadedStatementSummary> = emptyList()
    @Volatile private var cachedInstallmentProjection: InstallmentCashflowProjection? = null
    @Volatile private var cachedSubscriptionRadar: SubscriptionRadarSummary? = null

    val uiState: StateFlow<ExtreUiState> = combine(
        repository.snapshot,
        controlState
    ) { snap, ctrl ->
        val snapChanged = snap !== cachedSnapshot
        if (snapChanged) {
            cachedSnapshot = snap
        }

        val periods = if (snapChanged || ctrl.periodFilterMode != cachedPeriodMode) {
            cachedPeriodMode = ctrl.periodFilterMode
            repository.getAvailablePeriods(ctrl.periodFilterMode, snap).also {
                cachedPeriods = it
            }
        } else {
            cachedPeriods
        }

        val effectivePeriod = ctrl.selectedPeriod?.takeIf { it in periods }
            ?: periods.firstOrNull()
            ?: "2026-10"

        val overview = if (snapChanged || ctrl.periodFilterMode != cachedPeriodMode || effectivePeriod != cachedPeriodKey || cachedOverview == null) {
            cachedPeriodKey = effectivePeriod
            repository.computeMonthlyOverview(
                periodKey = effectivePeriod,
                filterMode = ctrl.periodFilterMode,
                snap = snap
            ).also { cachedOverview = it }
        } else {
            cachedOverview!!
        }

        val queueGroups = if (snapChanged || (cachedQueueGroups.isEmpty() && snap.transactions.isNotEmpty())) {
            repository.computeApprovalQueueGroups(snap).also { cachedQueueGroups = it }
        } else {
            cachedQueueGroups
        }

        val merchantList = if (snapChanged || ctrl.merchantSearchQuery != cachedSearchQuery) {
            cachedSearchQuery = ctrl.merchantSearchQuery
            repository.computeMerchantSummaries(ctrl.merchantSearchQuery, snap).also {
                cachedMerchantSummaries = it
            }
        } else {
            cachedMerchantSummaries
        }

        val merchantDetail = if (ctrl.selectedMerchantId == null) {
            cachedSelectedMerchantId = null
            cachedMerchantDetail = null
            null
        } else if (snapChanged || ctrl.selectedMerchantId != cachedSelectedMerchantId) {
            cachedSelectedMerchantId = ctrl.selectedMerchantId
            repository.computeMerchantDetail(ctrl.selectedMerchantId, snap).also {
                cachedMerchantDetail = it
            }
        } else {
            cachedMerchantDetail
        }

        val allStatements = if (snapChanged || cachedStatements == null) {
            repository.computeLoadedStatements(snap).also { cachedStatements = it }
        } else {
            cachedStatements!!
        }

        val statements = if (snapChanged || ctrl.statementSearchQuery != cachedStatementSearch || cachedFilteredStatements.isEmpty()) {
            cachedStatementSearch = ctrl.statementSearchQuery
            val q = ctrl.statementSearchQuery.trim().lowercase()
            if (q.isEmpty()) {
                allStatements
            } else {
                allStatements.filter {
                    it.fileName.lowercase().contains(q) ||
                    it.periodKeys.any { p -> p.contains(q) } ||
                    it.cardInfos.any { c -> c.lowercase().contains(q) }
                }
            }.also { cachedFilteredStatements = it }
        } else {
            cachedFilteredStatements
        }

        val otherPeriods = periods.filter { it != effectivePeriod }
        val effectiveTargetPeriod = ctrl.comparisonTargetPeriod?.takeIf { it in otherPeriods }
            ?: otherPeriods.firstOrNull()

        val comparison = if (effectiveTargetPeriod != null && periods.size >= 2) {
            repository.computeMonthOverMonthComparison(
                basePeriodKey = effectivePeriod,
                targetPeriodKey = effectiveTargetPeriod,
                filterMode = ctrl.periodFilterMode,
                snap = snap
            )
        } else {
            null
        }

        val installmentProjection = if (snapChanged || cachedInstallmentProjection == null) {
            repository.computeInstallmentProjection(snap).also {
                cachedInstallmentProjection = it
            }
        } else {
            cachedInstallmentProjection
        }

        val subscriptionRadar = if (snapChanged || cachedSubscriptionRadar == null) {
            repository.computeSubscriptionRadar(snap).also {
                cachedSubscriptionRadar = it
            }
        } else {
            cachedSubscriptionRadar
        }

        ExtreUiState(
            activeTab = ctrl.activeTab,
            planningTabMode = ctrl.planningTabMode,
            insightsTabMode = ctrl.insightsTabMode,
            managementSection = ctrl.managementSection,
            isPrivacyModeEnabled = ctrl.isPrivacyModeEnabled,
            isBiometricLockEnabled = ctrl.isBiometricLockEnabled,
            isScreenProtectionEnabled = ctrl.isScreenProtectionEnabled,
            lockTimeout = ctrl.lockTimeout,
            isAppLocked = ctrl.isAppLocked,
            biometricAvailability = ctrl.biometricAvailability,
            dashboardMode = ctrl.dashboardMode,
            periodFilterMode = ctrl.periodFilterMode,
            availablePeriods = periods,
            selectedPeriod = effectivePeriod,
            selectedCategoryIdFilter = ctrl.selectedCategoryIdFilter,
            transactionSortOption = ctrl.transactionSortOption,
            monthlyOverview = overview,
            isComparisonModeActive = (ctrl.insightsTabMode == InsightsTabMode.COMPARISON),
            comparisonTargetPeriod = effectiveTargetPeriod,
            monthOverMonthComparison = comparison,
            selectedForecastMonth = ctrl.selectedForecastMonth,
            installmentProjection = installmentProjection,
            subscriptionRadar = subscriptionRadar,
            selectedRecurringTabFilter = ctrl.selectedRecurringTabFilter,
            approvalQueueGroups = queueGroups,
            unapprovedTotalCount = overview.unapprovedCount,
            merchantSearchQuery = ctrl.merchantSearchQuery,
            merchantSummaries = merchantList,
            selectedMerchantId = ctrl.selectedMerchantId,
            selectedMerchantDetail = merchantDetail,
            statementSearchQuery = ctrl.statementSearchQuery,
            loadedStatements = statements,
            categories = snap.categories,
            merchants = snap.merchants,
            rules = snap.rules,
            ruleSimilarityCandidatesMap = emptyMap(),
            totalTransactionCount = snap.transactions.size,
            latestStatementMetadata = snap.latestStatementMetadata,
            pendingWizardTable = ctrl.pendingWizardTable,
            lastImportReport = ctrl.lastImportReport,
            bannerMessage = ctrl.bannerMessage
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExtreUiState(
            isPrivacyModeEnabled = securityPreferences?.isPrivacyModeEnabled ?: false,
            isBiometricLockEnabled = securityPreferences?.isBiometricLockEnabled ?: false,
            isScreenProtectionEnabled = securityPreferences?.isScreenProtectionEnabled ?: true,
            lockTimeout = securityPreferences?.lockTimeout ?: LockTimeout.IMMEDIATE,
            isAppLocked = securityPreferences?.isBiometricLockEnabled ?: false
        )
    )

    private var lastBackgroundEpochMs: Long = 0L

    fun togglePrivacyMode() {
        val next = !controlState.value.isPrivacyModeEnabled
        securityPreferences?.isPrivacyModeEnabled = next
        controlState.value = controlState.value.copy(
            isPrivacyModeEnabled = next
        )
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        securityPreferences?.isBiometricLockEnabled = enabled
        controlState.value = controlState.value.copy(
            isBiometricLockEnabled = enabled,
            bannerMessage = if (enabled) "Biyometrik kilit aktif edildi." else "Biyometrik kilit kapatıldı."
        )
    }

    fun setScreenProtectionEnabled(enabled: Boolean) {
        securityPreferences?.isScreenProtectionEnabled = enabled
        controlState.value = controlState.value.copy(
            isScreenProtectionEnabled = enabled,
            bannerMessage = if (enabled) "Ekran koruması (FLAG_SECURE) aktif." else "Ekran koruması kapatıldı."
        )
    }

    fun setLockTimeout(timeout: LockTimeout) {
        securityPreferences?.lockTimeout = timeout
        controlState.value = controlState.value.copy(lockTimeout = timeout)
    }

    val isAppLocked: Boolean get() = controlState.value.isAppLocked
    val isPrivacyModeEnabled: Boolean get() = controlState.value.isPrivacyModeEnabled
    val isScreenProtectionEnabled: Boolean get() = controlState.value.isScreenProtectionEnabled
    val currentLockTimeout: LockTimeout get() = controlState.value.lockTimeout

    fun setAppLocked(locked: Boolean) {
        controlState.value = controlState.value.copy(isAppLocked = locked)
    }

    fun updateBiometricAvailability(availability: BiometricAvailability) {
        controlState.value = controlState.value.copy(biometricAvailability = availability)
    }

    fun onAppBackgrounded() {
        lastBackgroundEpochMs = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (controlState.value.isBiometricLockEnabled) {
            val elapsed = System.currentTimeMillis() - lastBackgroundEpochMs
            val timeout = controlState.value.lockTimeout.durationMs
            if (elapsed >= timeout) {
                controlState.value = controlState.value.copy(isAppLocked = true)
            }
        }
    }

    fun lockAppNow() {
        if (controlState.value.isBiometricLockEnabled) {
            controlState.value = controlState.value.copy(isAppLocked = true)
        }
    }

    fun selectTab(tab: AppTab) {
        controlState.value = controlState.value.copy(
            activeTab = tab,
            selectedMerchantId = if (tab != AppTab.INSIGHTS) null else controlState.value.selectedMerchantId
        )
    }

    fun setPlanningTabMode(mode: PlanningTabMode) {
        controlState.value = controlState.value.copy(planningTabMode = mode)
    }

    fun setInsightsTabMode(mode: InsightsTabMode) {
        controlState.value = controlState.value.copy(insightsTabMode = mode)
    }

    fun setManagementSection(section: ManagementSection?) {
        controlState.value = controlState.value.copy(managementSection = section)
    }

    fun openApprovalQueue() {
        controlState.value = controlState.value.copy(
            activeTab = AppTab.MANAGEMENT,
            managementSection = ManagementSection.APPROVAL_QUEUE
        )
    }

    fun openStatementsManagement() {
        controlState.value = controlState.value.copy(
            activeTab = AppTab.MANAGEMENT,
            managementSection = ManagementSection.STATEMENTS
        )
    }

    fun selectPeriod(periodKey: String) {
        controlState.value = controlState.value.copy(
            selectedPeriod = periodKey,
            selectedCategoryIdFilter = null
        )
    }

    fun setDashboardMode(mode: DashboardAnalyticsMode) {
        controlState.value = controlState.value.copy(
            dashboardMode = mode,
            isComparisonModeActive = (mode == DashboardAnalyticsMode.COMPARISON)
        )
    }

    fun selectForecastMonth(yearMonth: String?) {
        controlState.value = controlState.value.copy(
            selectedForecastMonth = yearMonth
        )
    }

    fun selectRecurringTabFilter(type: RecurringPaymentType?) {
        controlState.value = controlState.value.copy(
            selectedRecurringTabFilter = type
        )
    }

    fun toggleComparisonMode() {
        val nextActive = !controlState.value.isComparisonModeActive
        controlState.value = controlState.value.copy(
            isComparisonModeActive = nextActive,
            dashboardMode = if (nextActive) DashboardAnalyticsMode.COMPARISON else DashboardAnalyticsMode.OVERVIEW
        )
    }

    fun setComparisonTargetPeriod(targetPeriodKey: String) {
        controlState.value = controlState.value.copy(
            comparisonTargetPeriod = targetPeriodKey
        )
    }

    fun swapComparisonPeriods() {
        val currentBase = controlState.value.selectedPeriod ?: uiState.value.selectedPeriod
        val currentTarget = uiState.value.comparisonTargetPeriod
        if (currentTarget != null) {
            controlState.value = controlState.value.copy(
                selectedPeriod = currentTarget,
                comparisonTargetPeriod = currentBase
            )
        }
    }

    fun setPeriodFilterMode(mode: PeriodFilterMode) {
        controlState.value = controlState.value.copy(
            periodFilterMode = mode,
            selectedPeriod = null,
            selectedCategoryIdFilter = null
        )
    }

    fun toggleCategoryFilter(categoryId: Long?) {
        val current = controlState.value.selectedCategoryIdFilter
        controlState.value = controlState.value.copy(
            selectedCategoryIdFilter = if (current == categoryId) null else categoryId
        )
    }

    fun selectTransactionSortOption(option: TransactionSortOption) {
        controlState.value = controlState.value.copy(transactionSortOption = option)
    }

    fun toggleSortByDate() {
        val current = controlState.value.transactionSortOption
        val next = when (current) {
            TransactionSortOption.DATE_DESC -> TransactionSortOption.DATE_ASC
            TransactionSortOption.DATE_ASC -> TransactionSortOption.DATE_DESC
            else -> TransactionSortOption.DATE_DESC
        }
        controlState.value = controlState.value.copy(transactionSortOption = next)
    }

    fun toggleSortByAmount() {
        val current = controlState.value.transactionSortOption
        val next = when (current) {
            TransactionSortOption.AMOUNT_DESC -> TransactionSortOption.AMOUNT_ASC
            TransactionSortOption.AMOUNT_ASC -> TransactionSortOption.AMOUNT_DESC
            else -> TransactionSortOption.AMOUNT_DESC
        }
        controlState.value = controlState.value.copy(transactionSortOption = next)
    }

    fun updateMerchantSearch(query: String) {
        controlState.value = controlState.value.copy(merchantSearchQuery = query)
    }

    fun openMerchantDetail(merchantId: Long) {
        controlState.value = controlState.value.copy(
            activeTab = AppTab.INSIGHTS,
            insightsTabMode = InsightsTabMode.MERCHANTS,
            selectedMerchantId = merchantId
        )
    }

    fun closeMerchantDetail() {
        controlState.value = controlState.value.copy(selectedMerchantId = null)
    }

    fun dismissImportReport() {
        controlState.value = controlState.value.copy(lastImportReport = null)
    }

    fun dismissBannerMessage() {
        controlState.value = controlState.value.copy(bannerMessage = null)
    }

    fun dismissWizard() {
        controlState.value = controlState.value.copy(pendingWizardTable = null)
    }

    fun importStatementFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val (fileName, bytes) = withContext(Dispatchers.IO) {
                    val name = resolveFileName(context, uri) ?: "EKSTRE.csv"
                    val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Dosya okunamadı")
                    name to data
                }
                processRawStatementBytes(bytes, fileName, context)
            }.onFailure { err ->
                controlState.value = controlState.value.copy(
                    bannerMessage = "Ekstre içe aktarma hatası: ${err.localizedMessage ?: "Bilinmeyen hata"}"
                )
            }
        }
    }

    private suspend fun processRawStatementBytes(
        bytes: ByteArray,
        fileName: String,
        context: Context,
        forceColumnWizard: Boolean = false
    ) {
        val rawTable = withContext(Dispatchers.Default) {
            StatementParserDispatcher.parseRawStatement(bytes, fileName, context)
        }
        if ((rawTable.needsColumnMappingWizard || forceColumnWizard) && rawTable.dataRows.isNotEmpty()) {
            controlState.value = controlState.value.copy(pendingWizardTable = rawTable)
        } else {
            val mapping = rawTable.autoDetectedMapping ?: ColumnMapping(0, 1, 2)
            val parsed = withContext(Dispatchers.Default) {
                TransactionNormalizer.normalizeTable(rawTable, mapping)
            }
            val report = withContext(Dispatchers.IO) {
                repository.importParsedStatement(parsed)
            }
            controlState.value = controlState.value.copy(
                pendingWizardTable = null,
                lastImportReport = report,
                selectedPeriod = parsed.metadata.statementYearMonth ?: controlState.value.selectedPeriod
            )
        }
    }

    fun confirmWizardColumnMapping(mapping: ColumnMapping) {
        val rawTable = controlState.value.pendingWizardTable ?: return
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.Default) {
                TransactionNormalizer.normalizeTable(rawTable, mapping)
            }
            val report = withContext(Dispatchers.IO) {
                repository.importParsedStatement(parsed)
            }
            controlState.value = controlState.value.copy(
                pendingWizardTable = null,
                lastImportReport = report
            )
        }
    }

    fun approveQueueItemAndSaveRule(
        group: ApprovalQueueGroup,
        keyword: String,
        merchantDisplayName: String,
        categoryId: Long,
        priority: Int = 10,
        applyToPastApproved: Boolean = false
    ) {
        viewModelScope.launch {
            val explicitIds = group.transactions.mapTo(HashSet()) { it.id }
            val count = withContext(Dispatchers.IO) {
                repository.approveAndCreateRule(
                    keyword = keyword,
                    merchantDisplayName = merchantDisplayName,
                    categoryId = categoryId,
                    priority = priority,
                    explicitTransactionIds = explicitIds,
                    applyToPastApprovedTransactions = applyToPastApproved
                )
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "\"$merchantDisplayName\" kuralı kaydedildi ve $count işlem otomatik sınıflandırıldı."
            )
        }
    }

    fun createOrUpdateRule(
        keyword: String,
        merchantDisplayName: String,
        categoryId: Long,
        priority: Int = 10,
        applyRetroactively: Boolean = true
    ) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                repository.approveAndCreateRule(
                    keyword = keyword,
                    merchantDisplayName = merchantDisplayName,
                    categoryId = categoryId,
                    priority = priority,
                    explicitTransactionIds = emptySet(),
                    applyToPastApprovedTransactions = applyRetroactively
                )
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "Kural kaydedildi ($count işlem eşleşti)."
            )
        }
    }

    fun editExistingRule(
        ruleId: Long,
        newKeyword: String,
        newMerchantDisplayName: String,
        newCategoryId: Long,
        newPriority: Int
    ) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                repository.editRule(
                    ruleId = ruleId,
                    newKeyword = newKeyword,
                    newMerchantDisplayName = newMerchantDisplayName,
                    newCategoryId = newCategoryId,
                    newPriority = newPriority,
                    reapplyToTransactions = true
                )
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "Kural güncellendi ($count işlem bu kuralla eşleşti)."
            )
        }
    }

    fun editExistingMerchant(
        merchantId: Long,
        newDisplayName: String,
        newCategoryId: Long
    ) {
        viewModelScope.launch {
            val targetMerchantId = withContext(Dispatchers.IO) {
                repository.editMerchant(
                    merchantId = merchantId,
                    newDisplayName = newDisplayName,
                    newCategoryId = newCategoryId
                )
            }
            controlState.value = controlState.value.copy(
                selectedMerchantId = targetMerchantId,
                bannerMessage = "Satıcı bilgileri güncellendi: \"${newDisplayName.trim()}\""
            )
        }
    }

    fun approveCandidateForRule(
        anchorRuleId: Long,
        candidate: RuleMatchCandidate
    ) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                repository.applyCandidateForRule(anchorRuleId, candidate)
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "Eşleşme önerisi onaylandı ($count işlem güncellendi)."
            )
        }
    }

    fun approveAllCandidatesForRule(anchorRuleId: Long, candidates: List<RuleMatchCandidate> = emptyList()) {
        viewModelScope.launch {
            val listToApprove = if (candidates.isNotEmpty()) {
                candidates
            } else {
                val snap = repository.snapshot.value
                val rule = snap.rules.firstOrNull { it.id == anchorRuleId } ?: return@launch
                RuleSimilarityAnalyzer.findCandidatesForRule(
                    anchorRule = rule,
                    allRules = snap.rules,
                    merchants = snap.merchants,
                    categories = snap.categories,
                    queueGroups = repository.computeApprovalQueueGroups(snap)
                )
            }
            if (listToApprove.isEmpty()) return@launch
            val count = withContext(Dispatchers.IO) {
                repository.applyAllCandidatesForRule(anchorRuleId, listToApprove)
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "${listToApprove.size} eşleşme önerisinin tamamı onaylandı ($count işlem güncellendi)."
            )
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(ruleId)
        }
    }

    fun updateRulePriority(ruleId: Long, newPriority: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRulePriority(ruleId, newPriority, reapplyToAll = true)
        }
    }

    fun reapplyAllRules() {
        viewModelScope.launch {
            val matched = withContext(Dispatchers.IO) {
                repository.reapplyAllRules(overrideAlreadyApproved = true)
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "Tüm kurallar yeniden çalıştırıldı ($matched işlem eşleşti)."
            )
        }
    }

    fun addCustomCategory(name: String, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addCategory(name, iconName, colorHex)
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "\"$name\" kategorisi eklendi."
            )
        }
    }

    fun exportDatabaseBackup(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = repository.exportBackupJson()
                    context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Dosya yazılamadı")
                }
                controlState.value = controlState.value.copy(
                    bannerMessage = "Veritabanı yedeği başarıyla dışa aktarıldı."
                )
            }.onFailure { err ->
                controlState.value = controlState.value.copy(
                    bannerMessage = "Yedekleme hatası: ${err.localizedMessage}"
                )
            }
        }
    }

    fun restoreDatabaseBackup(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            runCatching {
                val report = withContext(Dispatchers.IO) {
                    val json = context.contentResolver.openInputStream(sourceUri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("Yedek dosyası okunamadı")
                    repository.restoreBackupJson(json)
                }
                controlState.value = controlState.value.copy(
                    bannerMessage = "Yedek geri yüklendi: ${report.transactionsCount} işlem, ${report.rulesCount} kural, ${report.merchantsCount} satıcı."
                )
            }.onFailure { err ->
                controlState.value = controlState.value.copy(
                    bannerMessage = "Geri yükleme hatası: ${err.localizedMessage}"
                )
            }
        }
    }

    fun updateStatementSearch(query: String) {
        controlState.value = controlState.value.copy(statementSearchQuery = query)
    }

    fun deleteLoadedStatement(fileName: String) {
        viewModelScope.launch {
            val count = repository.deleteStatement(fileName)
            controlState.value = controlState.value.copy(
                bannerMessage = "$fileName kaldırıldı ($count işlem silindi)"
            )
        }
    }

    fun renameLoadedStatement(oldFileName: String, newFileName: String) {
        viewModelScope.launch {
            val count = repository.renameStatement(oldFileName, newFileName)
            controlState.value = controlState.value.copy(
                bannerMessage = "$count işlem için ekstre adı '$newFileName' olarak güncellendi"
            )
        }
    }

    fun reapplyRulesToStatement(fileName: String) {
        viewModelScope.launch {
            val count = repository.reapplyRulesToStatement(fileName)
            controlState.value = controlState.value.copy(
                bannerMessage = if (count > 0) {
                    "$fileName için $count işlem yeni kurallarla eşleşti ve onaylandı!"
                } else {
                    "$fileName için yeni eşleşen işlem bulunamadı."
                }
            )
        }
    }

    fun clearAllStatements() {
        viewModelScope.launch {
            val count = repository.deleteAllStatements()
            controlState.value = controlState.value.copy(
                bannerMessage = "Tüm ekstreler ve $count işlem temizlendi."
            )
        }
    }

    fun jumpToPeriod(periodKey: String) {
        controlState.value = controlState.value.copy(
            activeTab = AppTab.OVERVIEW,
            selectedPeriod = periodKey
        )
    }

    fun clearAllDatabase() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAllData()
            }
            controlState.value = controlState.value.copy(
                selectedMerchantId = null,
                selectedCategoryIdFilter = null,
                bannerMessage = "Tüm yerel veritabanı sıfırlandı."
            )
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        result = cursor.getString(idx)
                    }
                }
            }
        }
        return result ?: uri.lastPathSegment
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val sqliteHelper = ExtreSqliteHelper(appContext)
                    val repository = ExtreRepository(sqliteHelper)
                    val securityPreferences = SharedPreferencesSecurityPreferences(appContext)
                    return ExtreViewModel(repository, securityPreferences) as T
                }
            }
        }
    }
}

