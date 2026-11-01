package com.example.extreapp.domain.engine

import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.data.local.entity.TransactionEntity
import com.example.extreapp.data.local.entity.TransactionType

class RuleMatchingEngine {

    /**
     * Finds the highest-priority rule whose keyword is contained in [rawDescription]
     * using Turkish-aware case-insensitive normalization.
     *
     * Rules are sorted by:
     * 1. [RuleEntity.priority] descending (higher priority wins)
     * 2. Normalized keyword length descending (more specific keyword wins on tie,
     *    e.g., "IKEA ANKARA RESTORAN" beats "IKEA")
     */
    fun findMatchingRule(rawDescription: String, rules: List<RuleEntity>): RuleEntity? {
        if (rawDescription.isBlank() || rules.isEmpty()) return null
        val normalizedDesc = TurkishTextNormalizer.normalize(rawDescription)
        if (normalizedDesc.isEmpty()) return null

        val sortedRules = rules
            .map { rule -> rule to TurkishTextNormalizer.normalize(rule.keyword) }
            .filter { (_, normKeyword) -> normKeyword.isNotEmpty() }
            .sortedWith(
                compareByDescending<Pair<RuleEntity, String>> { it.first.priority }
                    .thenByDescending { it.second.length }
                    .thenBy { it.first.id }
            )

        return sortedRules.firstOrNull { (_, normKeyword) ->
            normalizedDesc.contains(normKeyword)
        }?.first
    }

    /**
     * Applies [rules] to a list of candidate [TransactionEntity] items.
     * - PAYMENT and REWARD_INFO rows are automatically marked [isApproved = true] so they don't clog the approval queue.
     * - EXPENSE and REFUND rows are matched against [rules]; if matched, [merchantId], [categoryId],
     *   [matchedRuleId] and [isApproved = true] are populated.
     */
    fun applyRulesToTransactions(
        transactions: List<TransactionEntity>,
        rules: List<RuleEntity>,
        overrideAlreadyApproved: Boolean = false
    ): List<TransactionEntity> {
        return transactions.map { tx ->
            when (tx.type) {
                TransactionType.PAYMENT, TransactionType.REWARD_INFO -> {
                    tx.copy(isApproved = true)
                }
                TransactionType.EXPENSE, TransactionType.REFUND -> {
                    if (tx.isApproved && !overrideAlreadyApproved && tx.merchantId != null && tx.categoryId != null) {
                        tx
                    } else {
                        val matchedRule = findMatchingRule(tx.rawDescription, rules)
                        if (matchedRule != null) {
                            tx.copy(
                                merchantId = matchedRule.merchantId,
                                categoryId = matchedRule.categoryId,
                                matchedRuleId = matchedRule.id,
                                isApproved = true
                            )
                        } else {
                            tx
                        }
                    }
                }
            }
        }
    }
}

