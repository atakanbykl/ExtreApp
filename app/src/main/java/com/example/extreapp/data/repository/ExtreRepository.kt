package com.example.extreapp.data.repository

import com.example.extreapp.data.local.DefaultCategories
import com.example.extreapp.data.local.ExtrePersistenceStore
import com.example.extreapp.data.local.entity.ApprovalQueueGroup
import com.example.extreapp.data.local.entity.CategoryBreakdownItem
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.MerchantMonthlyTotal
import com.example.extreapp.data.local.entity.MerchantSummaryItem
import com.example.extreapp.data.local.entity.MonthlyOverviewSummary
import com.example.extreapp.data.local.entity.PeriodFilterMode
import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.data.local.entity.TransactionEntity
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.data.local.entity.TransactionWithDetails
import com.example.extreapp.domain.backup.BackupManager
import com.example.extreapp.domain.backup.BackupRestoreReport
import com.example.extreapp.domain.backup.ExtreBackupPayload
import com.example.extreapp.domain.engine.RuleMatchCandidate
import com.example.extreapp.domain.engine.RuleMatchCandidateType
import com.example.extreapp.domain.engine.RuleMatchingEngine
import com.example.extreapp.domain.engine.SmartKeywordExtractor
import com.example.extreapp.domain.engine.TurkishTextNormalizer
import com.example.extreapp.domain.parser.StatementHeaderMetadata
import com.example.extreapp.domain.parser.StatementImportReport
import com.example.extreapp.domain.parser.StatementParseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DatabaseSnapshot(
    val categories: List<CategoryEntity> = DefaultCategories.INITIAL_CATEGORIES,
    val merchants: List<MerchantEntity> = emptyList(),
    val rules: List<RuleEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val latestStatementMetadata: StatementHeaderMetadata? = null
)

data class MerchantDetailData(
    val summary: MerchantSummaryItem,
    val monthlyTotals: List<MerchantMonthlyTotal>,
    val rules: List<RuleEntity>,
    val transactions: List<TransactionWithDetails>
)

class ExtreRepository(
    private val store: ExtrePersistenceStore,
    private val ruleEngine: RuleMatchingEngine = RuleMatchingEngine()
) {
    private val _snapshot = MutableStateFlow(
        DatabaseSnapshot(
            categories = store.loadCategories().ifEmpty { DefaultCategories.INITIAL_CATEGORIES },
            merchants = store.loadMerchants(),
            rules = store.loadRules(),
            transactions = store.loadTransactions()
        )
    )
    val snapshot: StateFlow<DatabaseSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    private fun updateAndPersist(
        categories: List<CategoryEntity> = _snapshot.value.categories,
        merchants: List<MerchantEntity> = _snapshot.value.merchants,
        rules: List<RuleEntity> = _snapshot.value.rules,
        transactions: List<TransactionEntity> = _snapshot.value.transactions,
        latestMetadata: StatementHeaderMetadata? = _snapshot.value.latestStatementMetadata
    ) {
        store.saveAll(categories, merchants, rules, transactions)
        _snapshot.value = DatabaseSnapshot(
            categories = categories,
            merchants = merchants,
            rules = rules,
            transactions = transactions,
            latestStatementMetadata = latestMetadata
        )
    }

    @Synchronized
    fun importParsedStatement(parseResult: StatementParseResult): StatementImportReport {
        val current = _snapshot.value
        val existingHashes = current.transactions.mapTo(HashSet()) { it.dedupHash }

        val uniqueNewCandidates = mutableListOf<TransactionEntity>()
        var duplicateCount = 0

        for (candidate in parseResult.transactions) {
            if (candidate.dedupHash in existingHashes) {
                duplicateCount++
            } else {
                existingHashes.add(candidate.dedupHash)
                uniqueNewCandidates.add(candidate)
            }
        }

        val nextIdStart = (current.transactions.maxOfOrNull { it.id } ?: 0L) + 1L
        val matchedNewTransactions = ruleEngine.applyRulesToTransactions(
            transactions = uniqueNewCandidates,
            rules = current.rules,
            overrideAlreadyApproved = false
        ).mapIndexed { idx, tx ->
            tx.copy(id = nextIdStart + idx)
        }

        val combinedTransactions = (current.transactions + matchedNewTransactions)
            .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.id })

        updateAndPersist(
            transactions = combinedTransactions,
            latestMetadata = parseResult.metadata
        )

        val expenseOrRefundNew = matchedNewTransactions.filter {
            it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND
        }
        val autoMatched = expenseOrRefundNew.count { it.isApproved }
        val queued = expenseOrRefundNew.count { !it.isApproved }
        val paymentAndReward = matchedNewTransactions.count {
            it.type == TransactionType.PAYMENT || it.type == TransactionType.REWARD_INFO
        }

        return StatementImportReport(
            fileName = parseResult.fileName,
            detectedCharset = parseResult.detectedCharset,
            metadata = parseResult.metadata,
            totalParsedCount = parseResult.transactions.size,
            newlyInsertedCount = matchedNewTransactions.size,
            autoMatchedCount = autoMatched,
            queuedForApprovalCount = queued,
            duplicateSkippedCount = duplicateCount,
            paymentAndRewardCount = paymentAndReward
        )
    }

    /**
     * Approves a transaction or group of transactions from the Approval Queue,
     * saves the user's chosen Merchant and Category as a Rule, and automatically
     * applies the rule to any other matching unapproved (or past) transactions.
     */
    @Synchronized
    fun approveAndCreateRule(
        keyword: String,
        merchantDisplayName: String,
        categoryId: Long,
        priority: Int = 10,
        explicitTransactionIds: Set<Long> = emptySet(),
        applyToPastApprovedTransactions: Boolean = false
    ): Int {
        val current = _snapshot.value
        val cleanMerchantName = merchantDisplayName.trim().ifEmpty { "Bilinmeyen Satıcı" }
        val normalizedMerchant = TurkishTextNormalizer.normalize(cleanMerchantName)
        val cleanKeyword = keyword.trim().ifEmpty { cleanMerchantName }

        // 1. Find or create Merchant
        val updatedMerchants = current.merchants.toMutableList()
        var merchant = updatedMerchants.firstOrNull { it.normalizedName == normalizedMerchant }
        if (merchant == null) {
            val nextMerchantId = (updatedMerchants.maxOfOrNull { it.id } ?: 0L) + 1L
            merchant = MerchantEntity(
                id = nextMerchantId,
                displayName = cleanMerchantName,
                normalizedName = normalizedMerchant,
                defaultCategoryId = categoryId
            )
            updatedMerchants.add(merchant)
        } else if (merchant.defaultCategoryId != categoryId) {
            val idx = updatedMerchants.indexOfFirst { it.id == merchant!!.id }
            merchant = merchant.copy(defaultCategoryId = categoryId)
            updatedMerchants[idx] = merchant
        }

        // 2. Find or create Rule
        val updatedRules = current.rules.toMutableList()
        val normalizedKeyword = TurkishTextNormalizer.normalize(cleanKeyword)
        val existingRuleIdx = updatedRules.indexOfFirst {
            TurkishTextNormalizer.normalize(it.keyword) == normalizedKeyword
        }

        val activeRule: RuleEntity
        if (existingRuleIdx >= 0) {
            activeRule = updatedRules[existingRuleIdx].copy(
                keyword = cleanKeyword,
                merchantId = merchant.id,
                categoryId = categoryId,
                priority = priority
            )
            updatedRules[existingRuleIdx] = activeRule
        } else {
            val nextRuleId = (updatedRules.maxOfOrNull { it.id } ?: 0L) + 1L
            activeRule = RuleEntity(
                id = nextRuleId,
                keyword = cleanKeyword,
                merchantId = merchant.id,
                categoryId = categoryId,
                priority = priority
            )
            updatedRules.add(activeRule)
        }

        // 3. Apply rule to explicit transactions + all matching transactions
        var affectedCount = 0
        val updatedTransactions = current.transactions.map { tx ->
            if (tx.type != TransactionType.EXPENSE && tx.type != TransactionType.REFUND) {
                tx
            } else if (tx.id in explicitTransactionIds) {
                affectedCount++
                tx.copy(
                    merchantId = merchant.id,
                    categoryId = categoryId,
                    matchedRuleId = activeRule.id,
                    isApproved = true
                )
            } else if (!tx.isApproved || applyToPastApprovedTransactions) {
                val normDesc = TurkishTextNormalizer.normalize(tx.rawDescription)
                if (normalizedKeyword.isNotEmpty() && normDesc.contains(normalizedKeyword)) {
                    affectedCount++
                    tx.copy(
                        merchantId = merchant.id,
                        categoryId = categoryId,
                        matchedRuleId = activeRule.id,
                        isApproved = true
                    )
                } else {
                    tx
                }
            } else {
                tx
            }
        }

        updateAndPersist(
            merchants = updatedMerchants,
            rules = updatedRules,
            transactions = updatedTransactions
        )
        return affectedCount
    }

    @Synchronized
    fun editRule(
        ruleId: Long,
        newKeyword: String,
        newMerchantDisplayName: String,
        newCategoryId: Long,
        newPriority: Int,
        reapplyToTransactions: Boolean = true
    ): Int {
        val current = _snapshot.value
        val existingRule = current.rules.firstOrNull { it.id == ruleId } ?: return 0
        val cleanMerchantName = newMerchantDisplayName.trim().ifEmpty { "Bilinmeyen Satıcı" }
        val normalizedMerchant = TurkishTextNormalizer.normalize(cleanMerchantName)
        val cleanKeyword = newKeyword.trim().ifEmpty { existingRule.keyword }

        // 1. Find or create Merchant
        val updatedMerchants = current.merchants.toMutableList()
        var merchant = updatedMerchants.firstOrNull { it.normalizedName == normalizedMerchant }
        if (merchant == null) {
            val nextMerchantId = (updatedMerchants.maxOfOrNull { it.id } ?: 0L) + 1L
            merchant = MerchantEntity(
                id = nextMerchantId,
                displayName = cleanMerchantName,
                normalizedName = normalizedMerchant,
                defaultCategoryId = newCategoryId
            )
            updatedMerchants.add(merchant)
        } else {
            val idx = updatedMerchants.indexOfFirst { it.id == merchant!!.id }
            merchant = merchant.copy(
                displayName = cleanMerchantName,
                defaultCategoryId = newCategoryId
            )
            updatedMerchants[idx] = merchant
        }

        // 2. Update the Rule
        val updatedRule = existingRule.copy(
            keyword = cleanKeyword,
            merchantId = merchant.id,
            categoryId = newCategoryId,
            priority = newPriority
        )
        val updatedRules = current.rules.map { if (it.id == ruleId) updatedRule else it }

        // 3. Re-evaluate transactions
        val updatedTransactions = if (reapplyToTransactions) {
            // Reset transactions that were previously matched by this rule so if keyword narrowed, they are re-checked
            val resetTxs = current.transactions.map { tx ->
                if (tx.matchedRuleId == ruleId) {
                    tx.copy(merchantId = null, categoryId = null, matchedRuleId = null, isApproved = false)
                } else {
                    tx
                }
            }
            ruleEngine.applyRulesToTransactions(
                transactions = resetTxs,
                rules = updatedRules,
                overrideAlreadyApproved = true
            )
        } else {
            current.transactions
        }

        val affectedCount = updatedTransactions.count { it.matchedRuleId == ruleId }
        updateAndPersist(
            merchants = updatedMerchants,
            rules = updatedRules,
            transactions = updatedTransactions
        )
        return affectedCount
    }

    @Synchronized
    fun editMerchant(
        merchantId: Long,
        newDisplayName: String,
        newCategoryId: Long
    ): Long {
        val current = _snapshot.value
        val existingMerchant = current.merchants.firstOrNull { it.id == merchantId } ?: return merchantId
        val cleanName = newDisplayName.trim().ifEmpty { existingMerchant.displayName }
        val normalizedName = TurkishTextNormalizer.normalize(cleanName)

        // Check if another merchant already has this normalizedName (merge into existing merchant)
        val collisionMerchant = current.merchants.firstOrNull {
            it.id != merchantId && it.normalizedName == normalizedName
        }

        val targetMerchantId: Long
        val updatedMerchants: List<MerchantEntity>

        if (collisionMerchant != null) {
            targetMerchantId = collisionMerchant.id
            updatedMerchants = current.merchants
                .filterNot { it.id == merchantId }
                .map { m ->
                    if (m.id == targetMerchantId) {
                        m.copy(displayName = cleanName, defaultCategoryId = newCategoryId)
                    } else {
                        m
                    }
                }
        } else {
            targetMerchantId = merchantId
            updatedMerchants = current.merchants.map { m ->
                if (m.id == merchantId) {
                    m.copy(
                        displayName = cleanName,
                        normalizedName = normalizedName,
                        defaultCategoryId = newCategoryId
                    )
                } else {
                    m
                }
            }
        }

        val updatedRules = current.rules.map { rule ->
            if (rule.merchantId == merchantId || rule.merchantId == targetMerchantId) {
                rule.copy(
                    merchantId = targetMerchantId,
                    categoryId = newCategoryId
                )
            } else {
                rule
            }
        }

        val updatedTransactions = current.transactions.map { tx ->
            if (tx.merchantId == merchantId || tx.merchantId == targetMerchantId) {
                tx.copy(
                    merchantId = targetMerchantId,
                    categoryId = newCategoryId
                )
            } else {
                tx
            }
        }

        updateAndPersist(
            merchants = updatedMerchants,
            rules = updatedRules,
            transactions = updatedTransactions
        )
        return targetMerchantId
    }

    @Synchronized
    fun applyCandidateForRule(
        anchorRuleId: Long,
        candidate: RuleMatchCandidate
    ): Int {
        val current = _snapshot.value
        val anchorRule = current.rules.firstOrNull { it.id == anchorRuleId } ?: return 0
        val anchorMerchant = current.merchants.firstOrNull { it.id == anchorRule.merchantId }
        val anchorMerchantName = anchorMerchant?.displayName ?: anchorRule.keyword

        return when (candidate.type) {
            RuleMatchCandidateType.SIMILAR_SAVED_RULE -> {
                val otherRule = candidate.matchedOtherRule ?: return 0
                val updatedRules = current.rules.map { r ->
                    if (r.id == otherRule.id) {
                        r.copy(
                            merchantId = anchorRule.merchantId,
                            categoryId = anchorRule.categoryId
                        )
                    } else {
                        r
                    }
                }
                val updatedTransactions = ruleEngine.applyRulesToTransactions(
                    transactions = current.transactions,
                    rules = updatedRules,
                    overrideAlreadyApproved = true
                )
                val affectedCount = updatedTransactions.count { it.merchantId == anchorRule.merchantId }
                updateAndPersist(
                    rules = updatedRules,
                    transactions = updatedTransactions
                )
                affectedCount
            }

            RuleMatchCandidateType.SIMILAR_UNAPPROVED_TX -> {
                val explicitIds = candidate.matchedQueueGroup?.transactions?.mapTo(HashSet()) { it.id }.orEmpty()
                approveAndCreateRule(
                    keyword = candidate.suggestedKeywordToBind,
                    merchantDisplayName = anchorMerchantName,
                    categoryId = anchorRule.categoryId,
                    priority = anchorRule.priority,
                    explicitTransactionIds = explicitIds,
                    applyToPastApprovedTransactions = false
                )
            }
        }
    }

    @Synchronized
    fun applyAllCandidatesForRule(
        anchorRuleId: Long,
        candidates: List<RuleMatchCandidate>
    ): Int {
        var total = 0
        for (c in candidates) {
            total += applyCandidateForRule(anchorRuleId, c)
        }
        return total
    }

    @Synchronized
    fun deleteRule(ruleId: Long) {
        val current = _snapshot.value
        val updatedRules = current.rules.filterNot { it.id == ruleId }
        updateAndPersist(rules = updatedRules)
    }

    @Synchronized
    fun updateRulePriority(ruleId: Long, newPriority: Int, reapplyToAll: Boolean = true) {
        val current = _snapshot.value
        val updatedRules = current.rules.map {
            if (it.id == ruleId) it.copy(priority = newPriority) else it
        }
        val updatedTransactions = if (reapplyToAll) {
            ruleEngine.applyRulesToTransactions(
                transactions = current.transactions,
                rules = updatedRules,
                overrideAlreadyApproved = true
            )
        } else {
            current.transactions
        }
        updateAndPersist(rules = updatedRules, transactions = updatedTransactions)
    }

    @Synchronized
    fun reapplyAllRules(overrideAlreadyApproved: Boolean = true): Int {
        val current = _snapshot.value
        val updatedTransactions = ruleEngine.applyRulesToTransactions(
            transactions = current.transactions,
            rules = current.rules,
            overrideAlreadyApproved = overrideAlreadyApproved
        )
        val matchedCount = updatedTransactions.count {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND) && it.isApproved
        }
        updateAndPersist(transactions = updatedTransactions)
        return matchedCount
    }

    @Synchronized
    fun addCategory(name: String, iconName: String, colorHex: Long): CategoryEntity {
        val current = _snapshot.value
        val cleanName = name.trim()
        val existing = current.categories.firstOrNull {
            TurkishTextNormalizer.normalize(it.name) == TurkishTextNormalizer.normalize(cleanName)
        }
        if (existing != null) return existing

        val nextId = (current.categories.maxOfOrNull { it.id } ?: 0L) + 1L
        val newCategory = CategoryEntity(
            id = nextId,
            name = cleanName,
            iconName = iconName,
            colorHex = colorHex,
            isDefault = false
        )
        updateAndPersist(categories = current.categories + newCategory)
        return newCategory
    }

    @Synchronized
    fun updateTransactionManualAssignment(
        transactionId: Long,
        merchantDisplayName: String,
        categoryId: Long
    ) {
        val current = _snapshot.value
        val cleanMerchantName = merchantDisplayName.trim().ifEmpty { "Genel Harcama" }
        val normMerchant = TurkishTextNormalizer.normalize(cleanMerchantName)
        val updatedMerchants = current.merchants.toMutableList()
        var merchant = updatedMerchants.firstOrNull { it.normalizedName == normMerchant }
        if (merchant == null) {
            val nextId = (updatedMerchants.maxOfOrNull { it.id } ?: 0L) + 1L
            merchant = MerchantEntity(
                id = nextId,
                displayName = cleanMerchantName,
                normalizedName = normMerchant,
                defaultCategoryId = categoryId
            )
            updatedMerchants.add(merchant)
        }

        val updatedTransactions = current.transactions.map { tx ->
            if (tx.id == transactionId) {
                tx.copy(
                    merchantId = merchant.id,
                    categoryId = categoryId,
                    isApproved = true
                )
            } else {
                tx
            }
        }
        updateAndPersist(merchants = updatedMerchants, transactions = updatedTransactions)
    }

    @Synchronized
    fun clearAllData() {
        updateAndPersist(
            categories = DefaultCategories.INITIAL_CATEGORIES,
            merchants = emptyList(),
            rules = emptyList(),
            transactions = emptyList(),
            latestMetadata = null
        )
    }

    fun getAvailablePeriods(filterMode: PeriodFilterMode, snap: DatabaseSnapshot = _snapshot.value): List<String> {
        val periods = snap.transactions.map {
            when (filterMode) {
                PeriodFilterMode.STATEMENT_PERIOD -> it.statementYearMonth
                PeriodFilterMode.TRANSACTION_MONTH -> it.yearMonth
            }
        }.distinct().sortedDescending()
        return periods
    }

    fun computeMonthlyOverview(
        periodKey: String?,
        filterMode: PeriodFilterMode,
        snap: DatabaseSnapshot = _snapshot.value
    ): MonthlyOverviewSummary {
        val availablePeriods = getAvailablePeriods(filterMode, snap)
        val effectivePeriod = periodKey?.takeIf { it in availablePeriods }
            ?: availablePeriods.firstOrNull()
            ?: "2026-10"

        val categoriesById = snap.categories.associateBy { it.id }
        val merchantsById = snap.merchants.associateBy { it.id }
        val rulesById = snap.rules.associateBy { it.id }
        val unapprovedTotalCount = snap.transactions.count {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND) && !it.isApproved
        }

        val periodTransactions = snap.transactions.filter { tx ->
            val key = when (filterMode) {
                PeriodFilterMode.STATEMENT_PERIOD -> tx.statementYearMonth
                PeriodFilterMode.TRANSACTION_MONTH -> tx.yearMonth
            }
            key == effectivePeriod
        }

        val grossExpense = periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val refundTotal = periodTransactions
            .filter { it.type == TransactionType.REFUND }
            .sumOf { it.amount }

        val netExpense = (grossExpense - refundTotal).coerceAtLeast(0.0)

        val paymentTotal = periodTransactions
            .filter { it.type == TransactionType.PAYMENT }
            .sumOf { it.amount }

        val uncategorizedFallback = CategoryEntity(
            id = -1L,
            name = "Onay Bekleyen / Tanımsız",
            iconName = "HelpOutline",
            colorHex = 0xFF94A3B8,
            isDefault = false
        )

        val expenseAndRefundTxs = periodTransactions.filter {
            it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND
        }

        val groupedByCategory = expenseAndRefundTxs.groupBy { tx ->
            tx.categoryId?.let { categoriesById[it] } ?: uncategorizedFallback
        }

        val rawBreakdown = groupedByCategory.map { (cat, txs) ->
            val catGross = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val catRefund = txs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
            val catNet = (catGross - catRefund).coerceAtLeast(0.0)
            CategoryBreakdownItem(
                category = cat,
                grossExpense = catGross,
                refundTotal = catRefund,
                netExpense = catNet,
                transactionCount = txs.size,
                percentageOfTotal = 0f
            )
        }.sortedByDescending { it.netExpense }

        val totalPositiveNet = rawBreakdown.sumOf { it.netExpense }
        val finalBreakdown = rawBreakdown.map { item ->
            val pct = if (totalPositiveNet > 0.0) {
                ((item.netExpense / totalPositiveNet) * 100.0).toFloat()
            } else {
                0f
            }
            item.copy(percentageOfTotal = pct)
        }

        val detailedTransactions = periodTransactions.map { tx ->
            TransactionWithDetails(
                transaction = tx,
                merchant = tx.merchantId?.let { merchantsById[it] },
                category = tx.categoryId?.let { categoriesById[it] },
                matchedRule = tx.matchedRuleId?.let { rulesById[it] }
            )
        }

        return MonthlyOverviewSummary(
            periodKey = effectivePeriod,
            filterMode = filterMode,
            grossExpenseTotal = grossExpense,
            refundTotal = refundTotal,
            netExpenseTotal = netExpense,
            paymentTotal = paymentTotal,
            rewardPointsTotalText = snap.latestStatementMetadata?.totalRewardPoints,
            unapprovedCount = unapprovedTotalCount,
            categoryBreakdowns = finalBreakdown,
            transactions = detailedTransactions
        )
    }

    fun computeApprovalQueueGroups(snap: DatabaseSnapshot = _snapshot.value): List<ApprovalQueueGroup> {
        val unapproved = snap.transactions.filter {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND) && !it.isApproved
        }
        if (unapproved.isEmpty()) return emptyList()

        return unapproved
            .groupBy { tx ->
                val suggestion = SmartKeywordExtractor.analyze(tx.rawDescription)
                TurkishTextNormalizer.normalize(suggestion.suggestedKeyword)
            }
            .values
            .map { groupTxs ->
                val sample = groupTxs.first()
                val suggestion = SmartKeywordExtractor.analyze(sample.rawDescription)
                val netAmount = groupTxs.sumOf {
                    if (it.type == TransactionType.REFUND) -it.amount else it.amount
                }
                ApprovalQueueGroup(
                    suggestedKeyword = suggestion.suggestedKeyword,
                    suggestedMerchantName = suggestion.suggestedMerchantName,
                    sampleRawDescription = sample.rawDescription,
                    sampleCleanedDescription = suggestion.cleanedDescription,
                    transactions = groupTxs.sortedByDescending { it.dateEpochDay },
                    totalAmount = netAmount
                )
            }
            .sortedWith(
                compareByDescending<ApprovalQueueGroup> { it.transactions.size }
                    .thenByDescending { kotlin.math.abs(it.totalAmount) }
            )
    }

    fun computeMerchantSummaries(
        searchQuery: String = "",
        snap: DatabaseSnapshot = _snapshot.value
    ): List<MerchantSummaryItem> {
        val categoriesById = snap.categories.associateBy { it.id }
        val rulesByMerchant = snap.rules.groupBy { it.merchantId }
        val txsByMerchant = snap.transactions
            .filter { (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND) && it.merchantId != null }
            .groupBy { it.merchantId!! }

        val normQuery = TurkishTextNormalizer.normalize(searchQuery)

        return snap.merchants
            .mapNotNull { merchant ->
                val txs = txsByMerchant[merchant.id].orEmpty()
                val rules = rulesByMerchant[merchant.id].orEmpty()
                if (txs.isEmpty() && rules.isEmpty()) return@mapNotNull null

                if (normQuery.isNotEmpty()) {
                    val matchesName = merchant.normalizedName.contains(normQuery)
                    val matchesKeyword = rules.any { TurkishTextNormalizer.normalize(it.keyword).contains(normQuery) }
                    if (!matchesName && !matchesKeyword) return@mapNotNull null
                }

                val gross = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val refunds = txs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
                val net = (gross - refunds).coerceAtLeast(0.0)
                val lastEpoch = txs.maxOfOrNull { it.dateEpochDay } ?: 0L
                val category = merchant.defaultCategoryId?.let { categoriesById[it] }
                    ?: txs.firstNotNullOfOrNull { it.categoryId }?.let { categoriesById[it] }

                MerchantSummaryItem(
                    merchant = merchant,
                    category = category,
                    netSpend = net,
                    grossSpend = gross,
                    refundTotal = refunds,
                    transactionCount = txs.size,
                    lastTransactionEpochDay = lastEpoch,
                    ruleKeywords = rules.map { it.keyword }
                )
            }
            .sortedByDescending { it.netSpend }
    }

    fun computeMerchantDetail(
        merchantId: Long,
        snap: DatabaseSnapshot = _snapshot.value
    ): MerchantDetailData? {
        val summary = computeMerchantSummaries("", snap).firstOrNull { it.merchant.id == merchantId }
            ?: return null
        val categoriesById = snap.categories.associateBy { it.id }
        val rulesById = snap.rules.associateBy { it.id }
        val merchantRules = snap.rules.filter { it.merchantId == merchantId }

        val merchantTxs = snap.transactions
            .filter { it.merchantId == merchantId && (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND) }
            .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.id })

        val monthlyTotals = merchantTxs
            .groupBy { it.yearMonth }
            .map { (ym, txs) ->
                val gross = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val refund = txs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
                val net = (gross - refund).coerceAtLeast(0.0)
                MerchantMonthlyTotal(
                    yearMonth = ym,
                    grossSpend = gross,
                    refundTotal = refund,
                    netSpend = net,
                    transactionCount = txs.size
                )
            }
            .sortedBy { it.yearMonth }

        val detailedTxs = merchantTxs.map { tx ->
            TransactionWithDetails(
                transaction = tx,
                merchant = summary.merchant,
                category = tx.categoryId?.let { categoriesById[it] } ?: summary.category,
                matchedRule = tx.matchedRuleId?.let { rulesById[it] }
            )
        }

        return MerchantDetailData(
            summary = summary,
            monthlyTotals = monthlyTotals,
            rules = merchantRules,
            transactions = detailedTxs
        )
    }

    fun exportBackupJson(): String {
        val snap = _snapshot.value
        val payload = ExtreBackupPayload(
            categories = snap.categories,
            merchants = snap.merchants,
            rules = snap.rules,
            transactions = snap.transactions
        )
        return BackupManager.serializeToJson(payload)
    }

    @Synchronized
    fun restoreBackupJson(jsonString: String): BackupRestoreReport {
        val payload = BackupManager.deserializeFromJson(jsonString)
        val categories = payload.categories.ifEmpty { DefaultCategories.INITIAL_CATEGORIES }
        updateAndPersist(
            categories = categories,
            merchants = payload.merchants,
            rules = payload.rules,
            transactions = payload.transactions
        )
        return BackupRestoreReport(
            categoriesCount = categories.size,
            merchantsCount = payload.merchants.size,
            rulesCount = payload.rules.size,
            transactionsCount = payload.transactions.size
        )
    }
}

