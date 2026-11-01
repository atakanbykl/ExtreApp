package com.example.extreapp.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.extreapp.data.local.ExtreSqliteHelper
import com.example.extreapp.data.local.entity.ApprovalQueueGroup
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.MerchantSummaryItem
import com.example.extreapp.data.local.entity.MonthlyOverviewSummary
import com.example.extreapp.data.local.entity.PeriodFilterMode
import com.example.extreapp.data.local.entity.RuleEntity
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
    DASHBOARD,
    APPROVAL_QUEUE,
    MERCHANTS,
    RULES_AND_BACKUP
}

data class ExtreUiState(
    val activeTab: AppTab = AppTab.DASHBOARD,
    val periodFilterMode: PeriodFilterMode = PeriodFilterMode.STATEMENT_PERIOD,
    val availablePeriods: List<String> = emptyList(),
    val selectedPeriod: String = "2026-10",
    val selectedCategoryIdFilter: Long? = null,
    val monthlyOverview: MonthlyOverviewSummary? = null,
    val approvalQueueGroups: List<ApprovalQueueGroup> = emptyList(),
    val unapprovedTotalCount: Int = 0,
    val merchantSearchQuery: String = "",
    val merchantSummaries: List<MerchantSummaryItem> = emptyList(),
    val selectedMerchantId: Long? = null,
    val selectedMerchantDetail: MerchantDetailData? = null,
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
    val activeTab: AppTab = AppTab.DASHBOARD,
    val periodFilterMode: PeriodFilterMode = PeriodFilterMode.STATEMENT_PERIOD,
    val selectedPeriod: String? = null,
    val selectedCategoryIdFilter: Long? = null,
    val merchantSearchQuery: String = "",
    val selectedMerchantId: Long? = null,
    val pendingWizardTable: RawStatementTable? = null,
    val lastImportReport: StatementImportReport? = null,
    val bannerMessage: String? = null
)

class ExtreViewModel(
    private val repository: ExtreRepository
) : ViewModel() {

    private val controlState = MutableStateFlow(UiControlState())

    val uiState: StateFlow<ExtreUiState> = combine(
        repository.snapshot,
        controlState
    ) { snap, ctrl ->
        val availablePeriods = repository.getAvailablePeriods(ctrl.periodFilterMode, snap)
        val effectivePeriod = ctrl.selectedPeriod?.takeIf { it in availablePeriods }
            ?: availablePeriods.firstOrNull()
            ?: "2026-10"

        val overview = repository.computeMonthlyOverview(
            periodKey = effectivePeriod,
            filterMode = ctrl.periodFilterMode,
            snap = snap
        )
        val queueGroups = repository.computeApprovalQueueGroups(snap)
        val merchantList = repository.computeMerchantSummaries(ctrl.merchantSearchQuery, snap)
        val merchantDetail = ctrl.selectedMerchantId?.let {
            repository.computeMerchantDetail(it, snap)
        }
        val candidatesByRuleId = RuleSimilarityAnalyzer.computeCandidatesMapForAllRules(
            allRules = snap.rules,
            merchants = snap.merchants,
            categories = snap.categories,
            queueGroups = queueGroups
        )

        ExtreUiState(
            activeTab = ctrl.activeTab,
            periodFilterMode = ctrl.periodFilterMode,
            availablePeriods = availablePeriods,
            selectedPeriod = effectivePeriod,
            selectedCategoryIdFilter = ctrl.selectedCategoryIdFilter,
            monthlyOverview = overview,
            approvalQueueGroups = queueGroups,
            unapprovedTotalCount = overview.unapprovedCount,
            merchantSearchQuery = ctrl.merchantSearchQuery,
            merchantSummaries = merchantList,
            selectedMerchantId = ctrl.selectedMerchantId,
            selectedMerchantDetail = merchantDetail,
            categories = snap.categories,
            merchants = snap.merchants,
            rules = snap.rules,
            ruleSimilarityCandidatesMap = candidatesByRuleId,
            totalTransactionCount = snap.transactions.size,
            latestStatementMetadata = snap.latestStatementMetadata,
            pendingWizardTable = ctrl.pendingWizardTable,
            lastImportReport = ctrl.lastImportReport,
            bannerMessage = ctrl.bannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExtreUiState()
    )

    fun selectTab(tab: AppTab) {
        controlState.value = controlState.value.copy(
            activeTab = tab,
            selectedMerchantId = if (tab != AppTab.MERCHANTS) null else controlState.value.selectedMerchantId
        )
    }

    fun selectPeriod(periodKey: String) {
        controlState.value = controlState.value.copy(
            selectedPeriod = periodKey,
            selectedCategoryIdFilter = null
        )
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

    fun updateMerchantSearch(query: String) {
        controlState.value = controlState.value.copy(merchantSearchQuery = query)
    }

    fun openMerchantDetail(merchantId: Long) {
        controlState.value = controlState.value.copy(
            activeTab = AppTab.MERCHANTS,
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

    fun importBundledHesapOzeti(context: Context) {
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.assets.open("HESAP_OZETI.csv").use { it.readBytes() }
                }
                processRawStatementBytes(bytes, "HESAP_OZETI.csv", context)
            }.onFailure { err ->
                controlState.value = controlState.value.copy(
                    bannerMessage = "Örnek ekstre yüklenemedi: ${err.localizedMessage}"
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

    fun approveAllCandidatesForRule(anchorRuleId: Long) {
        val candidates = uiState.value.ruleSimilarityCandidatesMap[anchorRuleId].orEmpty()
        if (candidates.isEmpty()) return
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                repository.applyAllCandidatesForRule(anchorRuleId, candidates)
            }
            controlState.value = controlState.value.copy(
                bannerMessage = "${candidates.size} eşleşme önerisinin tamamı onaylandı ($count işlem güncellendi)."
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
                    val sqliteHelper = ExtreSqliteHelper(context.applicationContext)
                    val repository = ExtreRepository(sqliteHelper)
                    return ExtreViewModel(repository) as T
                }
            }
        }
    }
}

