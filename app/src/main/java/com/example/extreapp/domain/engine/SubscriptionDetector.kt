package com.example.extreapp.domain.engine

import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.RecurringPaymentItem
import com.example.extreapp.data.local.entity.RecurringPaymentType
import com.example.extreapp.data.local.entity.RecurringPaymentsSummary
import com.example.extreapp.data.local.entity.SubscriptionBillingFrequency
import com.example.extreapp.data.local.entity.SubscriptionPriceHistory
import com.example.extreapp.data.local.entity.TransactionEntity
import com.example.extreapp.data.local.entity.TransactionType
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

object SubscriptionDetector {

    private data class KnownServiceDefinition(
        val id: String,
        val displayName: String,
        val brandColorHex: Long,
        val categoryHint: String,
        val isMatch: (normalizedDesc: String, amount: Double) -> Boolean
    )

    // Curated catalog of strictly fixed-day digital subscriptions
    private val KNOWN_SUBSCRIPTION_SERVICES = listOf(
        KnownServiceDefinition(
            id = "spotify",
            displayName = "Spotify",
            brandColorHex = 0xFF1DB954,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("spotify") }
        ),
        KnownServiceDefinition(
            id = "netflix",
            displayName = "Netflix",
            brandColorHex = 0xFFE50914,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("netflix") }
        ),
        KnownServiceDefinition(
            id = "youtube_member",
            displayName = "YouTube Katıl",
            brandColorHex = 0xFFFF0000,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("youtube") && norm.contains("member") }
        ),
        KnownServiceDefinition(
            id = "youtube_premium",
            displayName = "YouTube Premium",
            brandColorHex = 0xFFFF0000,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("youtube") && !norm.contains("member") }
        ),
        KnownServiceDefinition(
            id = "google_one",
            displayName = "Google One",
            brandColorHex = 0xFF4285F4,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("google one") || (norm.contains("google") && norm.contains("one")) }
        ),
        KnownServiceDefinition(
            id = "amazon_prime",
            displayName = "Amazon Prime",
            brandColorHex = 0xFFFF9900,
            categoryHint = "E-Ticaret & Alışveriş",
            isMatch = { norm, amount ->
                (norm.contains("amazon") && norm.contains("prime")) ||
                    (norm.contains("amazon.com") && amount in 15.0..60.0)
            }
        ),
        KnownServiceDefinition(
            id = "disney_plus",
            displayName = "Disney+",
            brandColorHex = 0xFF113CCF,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("disney") }
        ),
        KnownServiceDefinition(
            id = "apple_services",
            displayName = "Apple Hizmetleri (iCloud / Music)",
            brandColorHex = 0xFFA2AAAD,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("apple.com/bill") || norm.contains("itunes.com/bill") }
        ),
        KnownServiceDefinition(
            id = "chatgpt_plus",
            displayName = "OpenAI ChatGPT Plus",
            brandColorHex = 0xFF10A37F,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("openai") || norm.contains("chatgpt") }
        ),
        KnownServiceDefinition(
            id = "midjourney",
            displayName = "Midjourney",
            brandColorHex = 0xFF5865F2,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("midjourney") }
        ),
        KnownServiceDefinition(
            id = "blutv",
            displayName = "BluTV",
            brandColorHex = 0xFF00A0E9,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("blutv") }
        ),
        KnownServiceDefinition(
            id = "exxen",
            displayName = "Exxen",
            brandColorHex = 0xFFFFCC00,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("exxen") }
        ),
        KnownServiceDefinition(
            id = "gain_medya",
            displayName = "GAİN",
            brandColorHex = 0xFFFF3366,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("gain medya") || norm.startsWith("gain ") }
        ),
        KnownServiceDefinition(
            id = "ssport_plus",
            displayName = "S Sport Plus",
            brandColorHex = 0xFFD20A11,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("ssport") || norm.contains("s sport") }
        ),
        KnownServiceDefinition(
            id = "tod_tv",
            displayName = "TOD / beIN Connect",
            brandColorHex = 0xFF6C1CD1,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("tod tv") || norm.contains("todtv") || norm.contains("bein connect") }
        ),
        KnownServiceDefinition(
            id = "playstation_plus",
            displayName = "PlayStation Plus",
            brandColorHex = 0xFF003791,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("playstation") || norm.contains("playstation network") || norm.contains("sony interactive") }
        ),
        KnownServiceDefinition(
            id = "xbox_gamepass",
            displayName = "Xbox Game Pass",
            brandColorHex = 0xFF107C10,
            categoryHint = "Eğlence",
            isMatch = { norm, _ -> norm.contains("xbox") || norm.contains("msft *xbox") }
        ),
        KnownServiceDefinition(
            id = "linkedin_premium",
            displayName = "LinkedIn Premium",
            brandColorHex = 0xFF0A66C2,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("linkedin") }
        ),
        KnownServiceDefinition(
            id = "duolingo",
            displayName = "Duolingo Plus",
            brandColorHex = 0xFF58CC02,
            categoryHint = "Eğitim",
            isMatch = { norm, _ -> norm.contains("duolingo") }
        ),
        KnownServiceDefinition(
            id = "canva",
            displayName = "Canva Pro",
            brandColorHex = 0xFF00C4CC,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("canva") }
        ),
        KnownServiceDefinition(
            id = "github",
            displayName = "GitHub Pro / Copilot",
            brandColorHex = 0xFF24292E,
            categoryHint = "Dijital Servisler",
            isMatch = { norm, _ -> norm.contains("github") }
        )
    )

    fun detectSubscriptions(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity> = emptyList()
    ): RecurringPaymentsSummary {
        val expenseTxs = transactions.filter {
            it.type == TransactionType.EXPENSE && (it.installmentTotal == null || it.installmentTotal <= 1) && it.amount > 0.0
        }

        if (expenseTxs.isEmpty()) {
            return RecurringPaymentsSummary(
                totalMonthlyCommitment = 0.0,
                totalAnnualCommitment = 0.0,
                subscriptionsMonthlyTotal = 0.0,
                recurringExpensesMonthlyTotal = 0.0,
                subscriptionsCount = 0,
                recurringExpensesCount = 0,
                totalActiveCount = 0,
                priceIncreasedCount = 0,
                nextUpcomingPayment = null,
                subscriptions = emptyList(),
                recurringExpenses = emptyList(),
                allItems = emptyList()
            )
        }

        val categoriesById = categories.associateBy { it.id }
        val maxDbDate = transactions.maxOfOrNull { it.dateEpochDay } ?: LocalDate.now().toEpochDay()
        val referenceDate = if (LocalDate.now().toEpochDay() > maxDbDate) LocalDate.now() else LocalDate.ofEpochDay(maxDbDate)

        val subscriptionsList = mutableListOf<RecurringPaymentItem>()
        val recurringExpensesList = mutableListOf<RecurringPaymentItem>()
        val matchedTxIds = mutableSetOf<Long>()

        // -------------------------------------------------------------
        // KATEGORİ 1: ABONELİKLER (Sabit Günlü Dijital ve Servis Abonelikleri)
        // -------------------------------------------------------------
        for (known in KNOWN_SUBSCRIPTION_SERVICES) {
            val matchingTxs = expenseTxs.filter { tx ->
                val norm = TurkishTextNormalizer.normalize(tx.rawDescription)
                known.isMatch(norm, tx.amount)
            }

            if (matchingTxs.isNotEmpty()) {
                val subGroups = if (matchingTxs.map { it.amount }.distinct().size > 1 && matchingTxs.size > 2) {
                    matchingTxs.groupBy { "${known.id}_${String.format(Locale.ROOT, "%.2f", it.amount)}" }
                } else {
                    mapOf(known.id to matchingTxs)
                }

                for ((subId, groupTxs) in subGroups) {
                    groupTxs.forEach { matchedTxIds.add(it.id) }
                    val sortedTxs = groupTxs.sortedWith(
                        compareBy<TransactionEntity> { it.dateEpochDay }.thenBy { it.id }
                    )
                    val latestTx = sortedTxs.last()
                    val history = sortedTxs.map {
                        SubscriptionPriceHistory(
                            dateEpochDay = it.dateEpochDay,
                            yearMonth = it.yearMonth,
                            amount = it.amount
                        )
                    }

                    val latestAmount = latestTx.amount
                    val previousAmount = if (sortedTxs.size >= 2) sortedTxs[sortedTxs.size - 2].amount else null
                    val diff = if (previousAmount != null) latestAmount - previousAmount else 0.0
                    val isPriceIncreased = diff > 0.50
                    val priceChangePercent = if (previousAmount != null && previousAmount > 0.0 && abs(diff) > 0.50) {
                        ((diff / previousAmount) * 100.0)
                    } else {
                        null
                    }

                    val lastDate = LocalDate.ofEpochDay(latestTx.dateEpochDay)
                    val paymentDay = lastDate.dayOfMonth

                    val isYearly = sortedTxs.size >= 2 && sortedTxs.zipWithNext { a, b -> b.dateEpochDay - a.dateEpochDay }.any { it in 340..390 }
                    val frequency = if (isYearly) SubscriptionBillingFrequency.YEARLY else SubscriptionBillingFrequency.MONTHLY

                    var nextDate = if (frequency == SubscriptionBillingFrequency.YEARLY) lastDate.plusYears(1) else lastDate.plusMonths(1)
                    while (nextDate.isBefore(referenceDate)) {
                        nextDate = if (frequency == SubscriptionBillingFrequency.YEARLY) nextDate.plusYears(1) else nextDate.plusMonths(1)
                    }
                    val daysUntil = (nextDate.toEpochDay() - referenceDate.toEpochDay()).toInt().coerceAtLeast(0)

                    val category = latestTx.categoryId?.let { categoriesById[it] }
                        ?: categories.firstOrNull { TurkishTextNormalizer.normalize(it.name).contains(TurkishTextNormalizer.normalize(known.categoryHint)) }

                    val monthlyAmount = if (frequency == SubscriptionBillingFrequency.YEARLY) latestAmount / 12.0 else latestAmount
                    val suffix = if (subGroups.size > 1) " (${String.format(Locale.ROOT, "%.2f", latestAmount)} TL)" else ""

                    subscriptionsList.add(
                        RecurringPaymentItem(
                            id = subId,
                            name = "${known.displayName}$suffix",
                            rawMerchant = latestTx.rawDescription,
                            category = category,
                            paymentType = RecurringPaymentType.SUBSCRIPTION,
                            monthlyAmount = monthlyAmount,
                            latestAmount = latestAmount,
                            previousAmount = previousAmount,
                            priceChangePercent = priceChangePercent,
                            isPriceIncreased = isPriceIncreased,
                            billingFrequency = frequency,
                            isFixedBillingDay = true,
                            paymentDayOfMonth = paymentDay,
                            lastPaymentDateEpochDay = latestTx.dateEpochDay,
                            nextExpectedDateEpochDay = nextDate.toEpochDay(),
                            daysUntilNextBilling = daysUntil,
                            paymentCount = sortedTxs.size,
                            frequencyDescription = "Her ayın ${paymentDay}'i (Sabit Gün)",
                            history = history,
                            cardInfo = latestTx.cardInfo,
                            isKnownService = true,
                            brandColorHex = known.brandColorHex,
                            confidenceScore = 1.0f
                        )
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // KATEGORİ 2: TEKRARLI HARCAMALAR & DEĞİŞKEN GÜNLÜ ÖDEMELER
        // (Abonelik olmayıp periyodik/tekrarlı yapılan harcamalar;
        // her ayın aynı gününde ödenmek zorunda değildir!)
        // -------------------------------------------------------------
        val remainingTxs = expenseTxs.filter { it.id !in matchedTxIds }
        val merchantGroups = remainingTxs.groupBy { tx ->
            val suggestion = SmartKeywordExtractor.analyze(tx.rawDescription)
            TurkishTextNormalizer.normalize(suggestion.suggestedMerchantName)
        }

        for ((normMerchant, txList) in merchantGroups) {
            // Tekrarlı kabul edilme kriteri: en az 2 işlem bulunması
            if (txList.size >= 2) {
                val sortedTxs = txList.sortedWith(
                    compareBy<TransactionEntity> { it.dateEpochDay }.thenBy { it.id }
                )
                val distinctMonths = txList.map { it.yearMonth }.distinct()
                val daysOfMonth = sortedTxs.map { LocalDate.ofEpochDay(it.dateEpochDay).dayOfMonth }
                val dayRange = daysOfMonth.maxOrNull()!! - daysOfMonth.minOrNull()!!

                // Eğer birden fazla ayda TAM OLARAK AYNI GÜNDE (±1 gün) geliyorsa bu bir sabit aboneliktir
                val isFixedDaySubscription = distinctMonths.size >= 2 && dayRange <= 1

                val latestTx = sortedTxs.last()
                val latestAmount = latestTx.amount
                val previousAmount = sortedTxs[sortedTxs.size - 2].amount
                val diff = latestAmount - previousAmount
                val isPriceIncreased = diff > 0.50
                val priceChangePercent = if (previousAmount > 0.0 && abs(diff) > 0.50) {
                    ((diff / previousAmount) * 100.0)
                } else {
                    null
                }

                val lastDate = LocalDate.ofEpochDay(latestTx.dateEpochDay)
                val suggestion = SmartKeywordExtractor.analyze(latestTx.rawDescription)
                val displayName = suggestion.suggestedMerchantName.ifBlank { latestTx.cleanedDescription }
                val category = latestTx.categoryId?.let { categoriesById[it] }

                val history = sortedTxs.map {
                    SubscriptionPriceHistory(
                        dateEpochDay = it.dateEpochDay,
                        yearMonth = it.yearMonth,
                        amount = it.amount
                    )
                }

                if (isFixedDaySubscription) {
                    // Sabit günlü özel abonelik
                    val paymentDay = daysOfMonth.first()
                    var nextDate = lastDate.plusMonths(1)
                    while (nextDate.isBefore(referenceDate)) {
                        nextDate = nextDate.plusMonths(1)
                    }
                    val daysUntil = (nextDate.toEpochDay() - referenceDate.toEpochDay()).toInt().coerceAtLeast(0)

                    subscriptionsList.add(
                        RecurringPaymentItem(
                            id = "fixed_sub_${normMerchant.replace(" ", "_")}",
                            name = displayName,
                            rawMerchant = latestTx.rawDescription,
                            category = category,
                            paymentType = RecurringPaymentType.SUBSCRIPTION,
                            monthlyAmount = latestAmount,
                            latestAmount = latestAmount,
                            previousAmount = previousAmount,
                            priceChangePercent = priceChangePercent,
                            isPriceIncreased = isPriceIncreased,
                            billingFrequency = SubscriptionBillingFrequency.MONTHLY,
                            isFixedBillingDay = true,
                            paymentDayOfMonth = paymentDay,
                            lastPaymentDateEpochDay = latestTx.dateEpochDay,
                            nextExpectedDateEpochDay = nextDate.toEpochDay(),
                            daysUntilNextBilling = daysUntil,
                            paymentCount = sortedTxs.size,
                            frequencyDescription = "Her ayın ${paymentDay}'i (Sabit Gün)",
                            history = history,
                            cardInfo = latestTx.cardInfo,
                            isKnownService = false,
                            brandColorHex = null,
                            confidenceScore = 0.90f
                        )
                    )
                } else {
                    // Değişken günlü tekrarlı harcama (Abonelik değil)
                    val intervals = sortedTxs.zipWithNext { a, b -> b.dateEpochDay - a.dateEpochDay }
                    val avgIntervalDays = if (intervals.isNotEmpty()) intervals.average().toInt().coerceAtLeast(1) else 30

                    var nextEstimatedDate = lastDate.plusDays(avgIntervalDays.toLong())
                    if (nextEstimatedDate.isBefore(referenceDate)) {
                        nextEstimatedDate = referenceDate.plusDays(avgIntervalDays.toLong().coerceAtMost(30))
                    }
                    val daysUntil = (nextEstimatedDate.toEpochDay() - referenceDate.toEpochDay()).toInt().coerceAtLeast(0)

                    val totalSpent = sortedTxs.sumOf { it.amount }
                    val monthCount = distinctMonths.size.coerceAtLeast(1)
                    val avgMonthlyAmount = totalSpent / monthCount

                    val freqDesc = if (sortedTxs.size >= 3) {
                        "Ayda ~${(sortedTxs.size.toDouble() / monthCount).toInt().coerceAtLeast(1)} kez • Değişken günler"
                    } else {
                        "Periyodik • Değişken günler"
                    }

                    recurringExpensesList.add(
                        RecurringPaymentItem(
                            id = "recurring_exp_${normMerchant.replace(" ", "_")}",
                            name = displayName,
                            rawMerchant = latestTx.rawDescription,
                            category = category,
                            paymentType = RecurringPaymentType.RECURRING_EXPENSE,
                            monthlyAmount = avgMonthlyAmount,
                            latestAmount = latestAmount,
                            previousAmount = previousAmount,
                            priceChangePercent = priceChangePercent,
                            isPriceIncreased = isPriceIncreased,
                            billingFrequency = SubscriptionBillingFrequency.VARIABLE,
                            isFixedBillingDay = false,
                            paymentDayOfMonth = null,
                            lastPaymentDateEpochDay = latestTx.dateEpochDay,
                            nextExpectedDateEpochDay = nextEstimatedDate.toEpochDay(),
                            daysUntilNextBilling = daysUntil,
                            paymentCount = sortedTxs.size,
                            frequencyDescription = freqDesc,
                            history = history,
                            cardInfo = latestTx.cardInfo,
                            isKnownService = false,
                            brandColorHex = null,
                            confidenceScore = 0.85f
                        )
                    )
                }
            }
        }

        val sortedSubscriptions = subscriptionsList.sortedWith(
            compareByDescending<RecurringPaymentItem> { it.isPriceIncreased }
                .thenByDescending { it.monthlyAmount }
        )

        val sortedRecurringExpenses = recurringExpensesList.sortedWith(
            compareByDescending<RecurringPaymentItem> { it.isPriceIncreased }
                .thenByDescending { it.monthlyAmount }
        )

        val allItems = sortedSubscriptions + sortedRecurringExpenses

        val subTotal = sortedSubscriptions.sumOf { it.monthlyAmount }
        val expTotal = sortedRecurringExpenses.sumOf { it.monthlyAmount }
        val totalMonthly = subTotal + expTotal
        val totalAnnual = totalMonthly * 12.0

        val priceIncreasedCount = allItems.count { it.isPriceIncreased }
        val nextUpcoming = allItems.filter { it.daysUntilNextBilling != null }.minByOrNull { it.daysUntilNextBilling!! }

        return RecurringPaymentsSummary(
            totalMonthlyCommitment = totalMonthly,
            totalAnnualCommitment = totalAnnual,
            subscriptionsMonthlyTotal = subTotal,
            recurringExpensesMonthlyTotal = expTotal,
            subscriptionsCount = sortedSubscriptions.size,
            recurringExpensesCount = sortedRecurringExpenses.size,
            totalActiveCount = allItems.size,
            priceIncreasedCount = priceIncreasedCount,
            nextUpcomingPayment = nextUpcoming,
            subscriptions = sortedSubscriptions,
            recurringExpenses = sortedRecurringExpenses,
            allItems = allItems
        )
    }
}
