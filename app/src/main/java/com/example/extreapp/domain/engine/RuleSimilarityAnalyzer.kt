package com.example.extreapp.domain.engine

import com.example.extreapp.data.local.entity.ApprovalQueueGroup
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.RuleEntity
import kotlin.math.max
import kotlin.math.min

enum class RuleMatchCandidateType {
    SIMILAR_SAVED_RULE,     // Başka bir kayıtlı kural (farklı satıcı/kategoride olan)
    SIMILAR_UNAPPROVED_TX   // Onay bekleyen tanımsız işlem grubu
}

data class RuleMatchCandidate(
    val id: String,
    val anchorRuleId: Long,
    val type: RuleMatchCandidateType,
    val displayKeywordOrDesc: String,
    val currentStatusText: String,
    val reason: String,
    val similarityPercent: Int,
    val matchedOtherRule: RuleEntity? = null,
    val matchedQueueGroup: ApprovalQueueGroup? = null,
    val suggestedKeywordToBind: String
)

object RuleSimilarityAnalyzer {

    private val stopWords = setOf(
        "tic", "san", "ltd", "sti", "as", "gida", "tur", "tr", "gbr", "usa",
        "ankara", "istanbul", "izmir", "konya", "eskisehir", "antalya", "kocaeli",
        "london", "bellevue", "iyzico", "paycell", "tkpay", "halkode", "od",
        "elekse", "google", "com", "net", "org", "magaza", "restoran", "kafe",
        "cafe", "market", "sube", "standart", "pendik", "batikent"
    )

    // Semantic alias clusters that refer to the same brand even when spelled differently on POS slips
    private val aliasClusters: List<Pair<String, Set<String>>> = listOf(
        "Starbucks" to setOf("starbucks", "sbux"),
        "Petrol Ofisi" to setOf("petrol ofisi", "poas", "po-caliskan", "po caliskan"),
        "Metro Market" to setOf("metro grosmarket", "metro-batikent", "metro market", "metro"),
        "IKEA" to setOf("ikea", "ikea-mapa", "mapa mob"),
        "Eryaman Petrol" to setOf("eryaman petrol", "eryaman p"),
        "YouTube" to setOf("youtube", "youtube member"),
        "Amazon" to setOf("amazon", "amazon.com", "amazon.com.tr"),
        "Hepsiburada" to setOf("hepsiburada", "hepsipay", "hepsibu")
    )

    /**
     * Given a selected [anchorRule], finds all other saved rules (that currently point to a different
     * merchant or category) AND all unapproved transaction groups that look similar and might refer
     * to the same merchant/category as [anchorRule].
     */
    fun findCandidatesForRule(
        anchorRule: RuleEntity,
        allRules: List<RuleEntity>,
        merchants: List<MerchantEntity>,
        categories: List<CategoryEntity>,
        queueGroups: List<ApprovalQueueGroup>
    ): List<RuleMatchCandidate> {
        val merchantsById = merchants.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }
        val anchorMerchant = merchantsById[anchorRule.merchantId]
        val anchorMerchantName = anchorMerchant?.displayName ?: anchorRule.keyword

        val candidates = mutableListOf<RuleMatchCandidate>()

        // 1. Check other saved rules that point to a different merchant or different category
        for (otherRule in allRules) {
            if (otherRule.id == anchorRule.id) continue
            // If they already point to the exact same merchant and category, no need to suggest
            if (otherRule.merchantId == anchorRule.merchantId && otherRule.categoryId == anchorRule.categoryId) {
                continue
            }

            val otherMerchantName = merchantsById[otherRule.merchantId]?.displayName ?: "Bilinmeyen Satıcı"
            val otherCategoryName = categoriesById[otherRule.categoryId]?.name ?: "Diğer"

            val sim = compareTexts(
                keywordA = anchorRule.keyword,
                merchantA = anchorMerchantName,
                keywordB = otherRule.keyword,
                merchantB = otherMerchantName
            )

            if (sim != null) {
                candidates.add(
                    RuleMatchCandidate(
                        id = "rule_${anchorRule.id}_other_${otherRule.id}",
                        anchorRuleId = anchorRule.id,
                        type = RuleMatchCandidateType.SIMILAR_SAVED_RULE,
                        displayKeywordOrDesc = "Kural: \"${otherRule.keyword}\"",
                        currentStatusText = "Mevcut: $otherMerchantName • $otherCategoryName",
                        reason = sim.reason,
                        similarityPercent = sim.score,
                        matchedOtherRule = otherRule,
                        suggestedKeywordToBind = otherRule.keyword
                    )
                )
            }
        }

        // 2. Check unapproved transaction groups in the queue that look similar to this rule
        for (group in queueGroups) {
            val sim = compareTexts(
                keywordA = anchorRule.keyword,
                merchantA = anchorMerchantName,
                keywordB = group.suggestedKeyword,
                merchantB = group.suggestedMerchantName,
                rawContextB = group.sampleCleanedDescription
            )
            if (sim != null) {
                candidates.add(
                    RuleMatchCandidate(
                        id = "rule_${anchorRule.id}_queue_${TurkishTextNormalizer.normalize(group.suggestedKeyword)}",
                        anchorRuleId = anchorRule.id,
                        type = RuleMatchCandidateType.SIMILAR_UNAPPROVED_TX,
                        displayKeywordOrDesc = group.sampleRawDescription,
                        currentStatusText = "Onay Bekleyen • ${group.transactions.size} işlem",
                        reason = sim.reason,
                        similarityPercent = sim.score,
                        matchedQueueGroup = group,
                        suggestedKeywordToBind = group.suggestedKeyword
                    )
                )
            }
        }

        return candidates.sortedByDescending { it.similarityPercent }
    }

    /**
     * Computes a map of ruleId -> list of similarity candidates for all saved rules,
     * so each RuleItemCard can display how many similarity suggestions are available when clicked.
     */
    fun computeCandidatesMapForAllRules(
        allRules: List<RuleEntity>,
        merchants: List<MerchantEntity>,
        categories: List<CategoryEntity>,
        queueGroups: List<ApprovalQueueGroup>
    ): Map<Long, List<RuleMatchCandidate>> {
        if (allRules.isEmpty()) return emptyMap()
        return allRules.associate { rule ->
            rule.id to findCandidatesForRule(
                anchorRule = rule,
                allRules = allRules,
                merchants = merchants,
                categories = categories,
                queueGroups = queueGroups
            )
        }
    }

    private data class SimilarityDetail(
        val score: Int,
        val reason: String,
        val commonKeyword: String? = null
    )

    private fun compareTexts(
        keywordA: String,
        merchantA: String,
        keywordB: String,
        merchantB: String,
        rawContextB: String = ""
    ): SimilarityDetail? {
        val normKeyA = TurkishTextNormalizer.normalize(keywordA)
        val normKeyB = TurkishTextNormalizer.normalize(keywordB)
        val normMerchA = TurkishTextNormalizer.normalize(merchantA)
        val normMerchB = TurkishTextNormalizer.normalize(merchantB)
        val normRawB = TurkishTextNormalizer.normalize(rawContextB)

        // 1. Check known semantic brand alias clusters (e.g., STARBUCKS <-> SBUX, PETROL OFISI <-> POAŞ)
        for ((brandName, aliases) in aliasClusters) {
            val aInCluster = aliases.any { alias ->
                normKeyA.contains(alias) || normMerchA.contains(alias)
            }
            val bInCluster = aliases.any { alias ->
                normKeyB.contains(alias) || normMerchB.contains(alias) || normRawB.contains(alias)
            }
            if (aInCluster && bInCluster) {
                return SimilarityDetail(
                    score = 94,
                    reason = "Aynı markayı ($brandName) işaret ediyor olabilir",
                    commonKeyword = null
                )
            }
        }

        // 2. Check substring containment between keywords
        if (normKeyA.length >= 3 && normKeyB.length >= 3) {
            if (normKeyA.contains(normKeyB) || normKeyB.contains(normKeyA)) {
                val shorter = if (normKeyA.length <= normKeyB.length) keywordA else keywordB
                return SimilarityDetail(
                    score = 92,
                    reason = "Ortak anahtar kelime içeriyor (\"$shorter\")",
                    commonKeyword = shorter
                )
            }
        }

        // 3. Check shared significant tokens (e.g. "METRO GROSMARKET" & "METRO-BATIKENT" -> "METRO")
        val tokensA = extractSignificantTokens("$normKeyA $normMerchA")
        val tokensB = extractSignificantTokens("$normKeyB $normMerchB $normRawB")
        val sharedTokens = tokensA.intersect(tokensB)
        val longestShared = sharedTokens.maxByOrNull { it.length }
        if (longestShared != null && longestShared.length >= 3) {
            val sharedUpper = longestShared.uppercase(java.util.Locale.forLanguageTag("tr-TR"))
            return SimilarityDetail(
                score = 88,
                reason = "Ortak kök kelime tespit edildi: \"$sharedUpper\"",
                commonKeyword = sharedUpper
            )
        }

        // 4. Check prefix match on first word (e.g. "YEMEKSEPETI" vs "YEMEKSEPET")
        val firstTokenA = tokensA.firstOrNull()
        val firstTokenB = tokensB.firstOrNull()
        if (firstTokenA != null && firstTokenB != null && firstTokenA.length >= 4 && firstTokenB.length >= 4) {
            val commonPrefix = firstTokenA.commonPrefixWith(firstTokenB)
            if (commonPrefix.length >= 4) {
                val prefixUpper = commonPrefix.uppercase(java.util.Locale.forLanguageTag("tr-TR"))
                return SimilarityDetail(
                    score = 82,
                    reason = "Benzer kelime kökü: \"$prefixUpper...\"",
                    commonKeyword = prefixUpper
                )
            }
        }

        // 5. Check normalized string similarity (Levenshtein ratio >= 0.68)
        val keySimilarity = stringSimilarityRatio(normKeyA, normKeyB)
        val merchSimilarity = stringSimilarityRatio(normMerchA, normMerchB)
        val bestRatio = max(keySimilarity, merchSimilarity)
        if (bestRatio >= 0.68) {
            val pct = (bestRatio * 100).toInt().coerceIn(68, 95)
            return SimilarityDetail(
                score = pct,
                reason = "Yazım benzerliği yüksek (%$pct)",
                commonKeyword = null
            )
        }

        return null
    }

    private fun extractSignificantTokens(normalizedText: String): List<String> {
        return normalizedText
            .split(Regex("""[^a-z0-9]+"""))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords && !it.all { ch -> ch.isDigit() } }
            .distinct()
    }

    private fun stringSimilarityRatio(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val maxLen = max(a.length, b.length)
        if (maxLen < 4) return 0.0
        val dist = levenshteinDistance(a, b)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1)

        for (i in 1..rhsLen) {
            newCost[0] = i
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }
}
