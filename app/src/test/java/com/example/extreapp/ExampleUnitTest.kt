package com.example.extreapp

import com.example.extreapp.data.local.InMemoryPersistenceStore
import com.example.extreapp.data.local.entity.PeriodFilterMode
import com.example.extreapp.data.local.entity.TransactionSortOption
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.data.local.entity.TransactionWithDetails
import com.example.extreapp.data.repository.ExtreRepository
import com.example.extreapp.domain.engine.RuleSimilarityAnalyzer
import com.example.extreapp.domain.parser.StatementParserDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    private fun loadHesapOzetiBytes(): ByteArray {
        val candidates = listOf(
            File("HESAP_OZETI.csv"),
            File("../HESAP_OZETI.csv"),
            File("src/main/assets/HESAP_OZETI.csv"),
            File("app/src/main/assets/HESAP_OZETI.csv")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("HESAP_OZETI.csv bulunamadı")
        return file.readBytes()
    }

    @Test
    fun hesapOzetiCsvFullAnalysisAndDeduplicationTest() {
        val bytes = loadHesapOzetiBytes()
        val parsed = StatementParserDispatcher.parseAndNormalize(bytes, "HESAP_OZETI.csv")

        // 1. Verify Windows-1254 encoding detection and statement header metadata
        assertEquals("windows-1254", parsed.detectedCharset)
        assertEquals("Axess Gold MC / **** **** **** 1795", parsed.metadata.cardInfo)
        assertEquals("30.10.2026", parsed.metadata.statementDate)
        assertEquals("2026-10", parsed.metadata.statementYearMonth)
        assertEquals("84.951,86 TL", parsed.metadata.statementDebt)

        // 2. Verify GYMFIT same-day duplicate preservation (2 transactions on 11.10.2026 + 2 on other days = 4 total)
        val gymfitTxs = parsed.transactions.filter { it.rawDescription.contains("GYMFIT") }
        assertEquals(4, gymfitTxs.size)

        // 3. Verify foreign currency 0,00 TL row was attached to STEAMGAMES.COM
        val steamTx = parsed.transactions.firstOrNull { it.rawDescription.contains("STEAMGAMES.COM") }
        assertNotNull(steamTx)
        assertEquals(2280.64, steamTx!!.amount, 0.01)
        assertEquals("44.48 USD", steamTx.foreignCurrencyNote)

        // 4. Verify debt payment (-56.602,83 TL) is classified as PAYMENT
        val paymentTxs = parsed.transactions.filter { it.type == TransactionType.PAYMENT }
        assertEquals(1, paymentTxs.size)
        assertEquals(56602.83, paymentTxs.first().amount, 0.01)

        // 5. Verify Amazon installment refunds (-966,50 TL, 01/06 and 02/06)
        val refundTxs = parsed.transactions.filter { it.type == TransactionType.REFUND }
        assertEquals(2, refundTxs.size)
        assertEquals(1, refundTxs[0].installmentCurrent)
        assertEquals(6, refundTxs[0].installmentTotal)
        assertEquals(2, refundTxs[1].installmentCurrent)
        assertEquals(6, refundTxs[1].installmentTotal)

        // 6. Verify deduplication when importing the same statement twice
        val repo = ExtreRepository(InMemoryPersistenceStore())
        val firstReport = repo.importParsedStatement(parsed)
        assertEquals(parsed.transactions.size, firstReport.newlyInsertedCount)
        assertEquals(0, firstReport.duplicateSkippedCount)

        val secondReport = repo.importParsedStatement(parsed)
        assertEquals(0, secondReport.newlyInsertedCount)
        assertEquals(parsed.transactions.size, secondReport.duplicateSkippedCount)
    }

    @Test
    fun ruleMatchingPriorityAndMerchantAnalyticsTest() {
        val bytes = loadHesapOzetiBytes()
        val parsed = StatementParserDispatcher.parseAndNormalize(bytes, "HESAP_OZETI.csv")
        val repo = ExtreRepository(InMemoryPersistenceStore())
        repo.importParsedStatement(parsed)

        // Approve Amazon group -> all 6 Amazon transactions (including refunds and uppercase AMAZON.COM) should match
        val amazonMatchedCount = repo.approveAndCreateRule(
            keyword = "AMAZON.COM",
            merchantDisplayName = "Amazon",
            categoryId = 4L, // E-Ticaret & Alışveriş
            priority = 10
        )
        assertEquals(6, amazonMatchedCount)

        // Verify Month-over-Month analytics for Amazon (September 2026 vs October 2026)
        val amazonMerchant = repo.computeMerchantSummaries().first { it.merchant.displayName == "Amazon" }
        val amazonDetail = repo.computeMerchantDetail(amazonMerchant.merchant.id)
        assertNotNull(amazonDetail)
        assertEquals(2, amazonDetail!!.monthlyTotals.size)
        val sepTotal = amazonDetail.monthlyTotals.first { it.yearMonth == "2026-09" }
        val octTotal = amazonDetail.monthlyTotals.first { it.yearMonth == "2026-10" }
        // In 2026-09: 5406.00 expense - 966.50 refund = 4439.50 net
        assertEquals(4439.50, sepTotal.netSpend, 0.01)
        // In 2026-10: (1151.96 + 24.90 + 925.52) - 966.50 = 1135.88 net
        assertEquals(1135.88, octTotal.netSpend, 0.01)

        // Multi-keyword merchant: Starbucks ("STARBUCKS" + "SBUX")
        repo.approveAndCreateRule(keyword = "STARBUCKS", merchantDisplayName = "Starbucks", categoryId = 2L, priority = 10)
        repo.approveAndCreateRule(keyword = "SBUX", merchantDisplayName = "Starbucks", categoryId = 2L, priority = 10)
        val sbuxSummary = repo.computeMerchantSummaries().first { it.merchant.displayName == "Starbucks" }
        assertEquals(2, sbuxSummary.transactionCount)
        assertEquals(920.00, sbuxSummary.netSpend, 0.01)

        // Rule Priority: Specific "IKEA ANKARA RESTORAN" (priority 20 -> Restoran 2L) vs general "IKEA" (priority 10 -> Ev 7L)
        repo.approveAndCreateRule(keyword = "IKEA", merchantDisplayName = "IKEA Mağaza", categoryId = 7L, priority = 10)
        repo.approveAndCreateRule(
            keyword = "IKEA ANKARA RESTORAN",
            merchantDisplayName = "IKEA Restoran",
            categoryId = 2L,
            priority = 20,
            applyToPastApprovedTransactions = true
        )
        repo.reapplyAllRules(overrideAlreadyApproved = true)

        val ikeaStore = repo.computeMerchantSummaries().first { it.merchant.displayName == "IKEA Mağaza" }
        val ikeaRest = repo.computeMerchantSummaries().first { it.merchant.displayName == "IKEA Restoran" }
        assertEquals(6065.92, ikeaStore.netSpend, 0.01)
        assertEquals(455.00, ikeaRest.netSpend, 0.01)
    }

    @Test
    fun backupExportAndRestoreRoundTripTest() {
        val bytes = loadHesapOzetiBytes()
        val parsed = StatementParserDispatcher.parseAndNormalize(bytes, "HESAP_OZETI.csv")
        val repo = ExtreRepository(InMemoryPersistenceStore())
        repo.importParsedStatement(parsed)
        repo.approveAndCreateRule(keyword = "YEMEKSEPET", merchantDisplayName = "Yemeksepeti", categoryId = 2L)

        val overviewBefore = repo.computeMonthlyOverview("2026-10", PeriodFilterMode.STATEMENT_PERIOD)
        val json = repo.exportBackupJson()
        assertTrue(json.isNotBlank())

        repo.clearAllData()
        assertEquals(0, repo.snapshot.value.transactions.size)

        val restoreReport = repo.restoreBackupJson(json)
        assertEquals(parsed.transactions.size, restoreReport.transactionsCount)
        assertEquals(1, restoreReport.rulesCount)

        val overviewAfter = repo.computeMonthlyOverview("2026-10", PeriodFilterMode.STATEMENT_PERIOD)
        assertEquals(overviewBefore.netExpenseTotal, overviewAfter.netExpenseTotal, 0.01)
        assertEquals(overviewBefore.unapprovedCount, overviewAfter.unapprovedCount)
    }

    @Test
    fun ruleSimilaritySuggestionsAndRuleEditingTest() {
        val bytes = loadHesapOzetiBytes()
        val parsed = StatementParserDispatcher.parseAndNormalize(bytes, "HESAP_OZETI.csv")
        val repo = ExtreRepository(InMemoryPersistenceStore())
        repo.importParsedStatement(parsed)

        // 1. Create a rule for "METRO GROSMARKET" -> "Metro Market" and another rule for "STARBUCKS" -> "Starbucks Coffee"
        repo.approveAndCreateRule(
            keyword = "METRO GROSMARKET",
            merchantDisplayName = "Metro Market",
            categoryId = 1L
        )
        repo.approveAndCreateRule(
            keyword = "STARBUCKS",
            merchantDisplayName = "Starbucks Coffee",
            categoryId = 2L
        )
        // Also create a separate rule for "ERYAMAN PETROL" -> "Eryaman Petrol A" and "ERYAMAN P" -> "Halköde Eryaman B"
        repo.approveAndCreateRule(
            keyword = "ERYAMAN PETROL",
            merchantDisplayName = "Eryaman Petrol A",
            categoryId = 3L
        )
        repo.approveAndCreateRule(
            keyword = "ERYAMAN P",
            merchantDisplayName = "Halköde Eryaman B",
            categoryId = 3L
        )

        // 2. When clicking on the "STARBUCKS" rule in the Rules tab, findCandidatesForRule should find "SBUX"
        val snap1 = repo.snapshot.value
        val queueGroups = repo.computeApprovalQueueGroups(snap1)
        val starbucksRule = snap1.rules.first { it.keyword == "STARBUCKS" }
        val starbucksCandidates = RuleSimilarityAnalyzer.findCandidatesForRule(
            anchorRule = starbucksRule,
            allRules = snap1.rules,
            merchants = snap1.merchants,
            categories = snap1.categories,
            queueGroups = queueGroups
        )
        assertTrue(starbucksCandidates.any { it.suggestedKeywordToBind.contains("SBUX") })

        // Approve All candidates for the Starbucks rule ("Hepsini Onayla")
        val approvedCount = repo.applyAllCandidatesForRule(starbucksRule.id, starbucksCandidates)
        assertTrue(approvedCount >= 1)

        // 3. When clicking on the "ERYAMAN PETROL" rule, findCandidatesForRule should find the saved "ERYAMAN P" rule
        val snap2 = repo.snapshot.value
        val eryamanRule = snap2.rules.first { it.keyword == "ERYAMAN PETROL" }
        val eryamanCandidates = RuleSimilarityAnalyzer.findCandidatesForRule(
            anchorRule = eryamanRule,
            allRules = snap2.rules,
            merchants = snap2.merchants,
            categories = snap2.categories,
            queueGroups = repo.computeApprovalQueueGroups(snap2)
        )
        assertTrue(eryamanCandidates.isNotEmpty())
        repo.applyAllCandidatesForRule(eryamanRule.id, eryamanCandidates)

        // 4. Test Editing an Existing Rule
        repo.editRule(
            ruleId = starbucksRule.id,
            newKeyword = "STARBUCKS",
            newMerchantDisplayName = "Starbucks Türkiye",
            newCategoryId = 2L,
            newPriority = 25
        )
        val updatedRule = repo.snapshot.value.rules.first { it.id == starbucksRule.id }
        assertEquals(25, updatedRule.priority)
        val updatedMerchant = repo.snapshot.value.merchants.first { it.id == updatedRule.merchantId }
        assertEquals("Starbucks Türkiye", updatedMerchant.displayName)

        // 5. Test Editing a Merchant from the Merchant Detail screen (rename & change category)
        val editedMerchantId = repo.editMerchant(
            merchantId = updatedMerchant.id,
            newDisplayName = "Starbucks Coffee TR",
            newCategoryId = 4L
        )
        assertEquals(updatedMerchant.id, editedMerchantId)
        val renamedMerchant = repo.snapshot.value.merchants.first { it.id == editedMerchantId }
        assertEquals("Starbucks Coffee TR", renamedMerchant.displayName)
        assertEquals(4L, renamedMerchant.defaultCategoryId)
        assertTrue(
            repo.snapshot.value.transactions
                .filter { it.merchantId == editedMerchantId }
                .all { it.categoryId == 4L }
        )
    }

    @Test
    fun privacyModeMaskingTest() {
        // Normal formatting
        val normalTry = com.example.extreapp.ui.theme.Formatters.formatCurrency(1250.50, isMasked = false)
        assertTrue(normalTry.contains("1.250,50") && normalTry.contains("TL"))

        // Privacy mode formatting (all currency amounts should be masked as ***)
        val maskedPositive = com.example.extreapp.ui.theme.Formatters.formatCurrency(1250.50, isMasked = true)
        assertEquals("***", maskedPositive)

        val maskedNegative = com.example.extreapp.ui.theme.Formatters.formatCurrency(-56602.83, isMasked = true)
        assertEquals("***", maskedNegative)

        val maskedUsd = com.example.extreapp.ui.theme.Formatters.formatCurrency(44.48, currency = "USD", isMasked = true)
        assertEquals("***", maskedUsd)
    }

    @Test
    fun transactionSortingByDateAndPriceTest() {
        val bytes = loadHesapOzetiBytes()
        val parsed = StatementParserDispatcher.parseAndNormalize(bytes, "HESAP_OZETI.csv")
        val repo = ExtreRepository(InMemoryPersistenceStore())
        repo.importParsedStatement(parsed)

        val overview = repo.computeMonthlyOverview("2026-10", PeriodFilterMode.STATEMENT_PERIOD)
        val txs = overview.transactions
        assertTrue(txs.isNotEmpty())

        // 1. Sort by Date Descending (En Yeni)
        val dateDesc = txs.sortedWith(
            compareByDescending<TransactionWithDetails> { it.transaction.dateEpochDay }
                .thenByDescending { it.transaction.id }
        )
        assertTrue(dateDesc.first().transaction.dateEpochDay >= dateDesc.last().transaction.dateEpochDay)

        // 2. Sort by Date Ascending (En Eski)
        val dateAsc = txs.sortedWith(
            compareBy<TransactionWithDetails> { it.transaction.dateEpochDay }
                .thenBy { it.transaction.id }
        )
        assertTrue(dateAsc.first().transaction.dateEpochDay <= dateAsc.last().transaction.dateEpochDay)

        // 3. Sort by Amount/Price Descending (Fiyat En Yüksek)
        val amountDesc = txs.sortedWith(
            compareByDescending<TransactionWithDetails> { it.transaction.amount }
                .thenByDescending { it.transaction.dateEpochDay }
        )
        assertEquals(56602.83, amountDesc.first().transaction.amount, 0.01)
        assertTrue(amountDesc.first().transaction.amount >= amountDesc.last().transaction.amount)

        // 4. Sort by Amount/Price Ascending (Fiyat En Düşük)
        val amountAsc = txs.sortedWith(
            compareBy<TransactionWithDetails> { it.transaction.amount }
                .thenByDescending { it.transaction.dateEpochDay }
        )
        assertEquals(0.0, amountAsc.first().transaction.amount, 0.01)
        assertTrue(amountAsc.first().transaction.amount <= amountAsc.last().transaction.amount)
    }

    @Test
    fun transactionSortOptionToggleLogicTest() {
        // Initial state is DATE_DESC
        var currentOption = TransactionSortOption.DATE_DESC

        fun toggleDate(current: TransactionSortOption): TransactionSortOption {
            return if (current == TransactionSortOption.DATE_DESC) {
                TransactionSortOption.DATE_ASC
            } else {
                TransactionSortOption.DATE_DESC
            }
        }

        fun toggleAmount(current: TransactionSortOption): TransactionSortOption {
            return if (current == TransactionSortOption.AMOUNT_DESC) {
                TransactionSortOption.AMOUNT_ASC
            } else {
                TransactionSortOption.AMOUNT_DESC
            }
        }

        // 1. Tapping Date when on DATE_DESC toggles to DATE_ASC
        currentOption = toggleDate(currentOption)
        assertEquals(TransactionSortOption.DATE_ASC, currentOption)
        assertTrue(currentOption.isDate)
        assertFalse(currentOption.isAmount)

        // 2. Tapping Date again toggles back to DATE_DESC
        currentOption = toggleDate(currentOption)
        assertEquals(TransactionSortOption.DATE_DESC, currentOption)

        // 3. Tapping Amount switches to AMOUNT_DESC (first click highest price)
        currentOption = toggleAmount(currentOption)
        assertEquals(TransactionSortOption.AMOUNT_DESC, currentOption)
        assertTrue(currentOption.isAmount)
        assertFalse(currentOption.isDate)

        // 4. Tapping Amount again switches to AMOUNT_ASC (second click lowest price)
        currentOption = toggleAmount(currentOption)
        assertEquals(TransactionSortOption.AMOUNT_ASC, currentOption)

        // 5. Tapping Date switches back to DATE_DESC (first click newest date)
        currentOption = toggleDate(currentOption)
        assertEquals(TransactionSortOption.DATE_DESC, currentOption)
    }

    @Test
    fun loadedStatementsManagementTest() {
        val bytes = loadHesapOzetiBytes()
        val parsed = StatementParserDispatcher.parseAndNormalize(bytes, "HESAP_OZETI.csv")
        val repo = ExtreRepository(InMemoryPersistenceStore())
        repo.importParsedStatement(parsed)

        // 1. Verify loaded statements summary
        val statements = repo.computeLoadedStatements()
        assertEquals(1, statements.size)
        val stmt = statements.first()
        assertEquals("HESAP_OZETI.csv", stmt.fileName)
        assertEquals(parsed.transactions.size, stmt.transactionCount)
        assertEquals("2026-10", stmt.primaryPeriod)
        assertTrue(stmt.grossExpenseTotal > 0.0)

        // 2. Test renaming statement
        val renameCount = repo.renameStatement("HESAP_OZETI.csv", "Ekim_2026_Asil_Kart.csv")
        assertEquals(stmt.transactionCount, renameCount)
        val renamedStatements = repo.computeLoadedStatements()
        assertEquals(1, renamedStatements.size)
        assertEquals("Ekim_2026_Asil_Kart.csv", renamedStatements.first().fileName)

        // 3. Test deleting statement
        val deletedCount = repo.deleteStatement("Ekim_2026_Asil_Kart.csv")
        assertEquals(stmt.transactionCount, deletedCount)
        val emptyStatements = repo.computeLoadedStatements()
        assertTrue(emptyStatements.isEmpty())
        assertTrue(repo.snapshot.value.transactions.isEmpty())
    }
}
