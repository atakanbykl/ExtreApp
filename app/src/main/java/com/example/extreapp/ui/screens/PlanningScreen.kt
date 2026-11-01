package com.example.extreapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extreapp.data.local.entity.ActiveInstallmentPlan
import com.example.extreapp.data.local.entity.InstallmentCashflowProjection
import com.example.extreapp.data.local.entity.MonthlyInstallmentForecast
import com.example.extreapp.data.local.entity.RecurringPaymentItem
import com.example.extreapp.data.local.entity.RecurringPaymentType
import com.example.extreapp.data.local.entity.RecurringPaymentsSummary
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.PlanningTabMode
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon

@Composable
fun PlanningScreen(
    uiState: ExtreUiState,
    onSelectPlanningTabMode: (PlanningTabMode) -> Unit,
    onSelectForecastMonth: (String?) -> Unit,
    onSelectRecurringTabFilter: (RecurringPaymentType?) -> Unit,
    onPickStatementFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Apple-style 2-Segmented Switcher: Taksit Takvimi vs Tekrarlanan Ödemeler
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isInstallments = uiState.planningTabMode == PlanningTabMode.INSTALLMENTS
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInstallments) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isInstallments) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .applePressScale {
                                if (!isInstallments) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectPlanningTabMode(PlanningTabMode.INSTALLMENTS)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isInstallments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Taksit Takvimi",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isInstallments) FontWeight.Bold else FontWeight.Medium,
                                color = if (isInstallments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isRecurring = uiState.planningTabMode == PlanningTabMode.RECURRING
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRecurring) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isRecurring) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .applePressScale {
                                if (!isRecurring) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectPlanningTabMode(PlanningTabMode.RECURRING)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tekrarlanan Ödemeler",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isRecurring) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        when (uiState.planningTabMode) {
            PlanningTabMode.INSTALLMENTS -> {
                val projection = uiState.installmentProjection
                if (projection != null && (projection.totalActivePlansCount > 0 || projection.completedPlans.isNotEmpty())) {
                    // 1. Hero Installment Summary Card
                    item {
                        InstallmentHeroCard(
                            projection = projection,
                            isPrivacyMode = uiState.isPrivacyModeEnabled
                        )
                    }

                    // 2. Monthly Timeline Bar
                    if (projection.monthlyForecasts.isNotEmpty()) {
                        item {
                            MonthlyInstallmentTimeline(
                                forecasts = projection.monthlyForecasts,
                                selectedMonth = uiState.selectedForecastMonth,
                                onSelectMonth = onSelectForecastMonth,
                                isPrivacyMode = uiState.isPrivacyModeEnabled
                            )
                        }
                    }

                    // 3. Active Plans Header + Selected Month filter badge if any
                    val filteredActivePlans = if (uiState.selectedForecastMonth == null) {
                        projection.activePlans
                    } else {
                        val keysInMonth = projection.monthlyForecasts
                            .firstOrNull { it.yearMonth == uiState.selectedForecastMonth }
                            ?.occurrences
                            ?.map { it.planKey }
                            ?.toSet()
                            ?: emptySet()
                        projection.activePlans.filter { it.planKey in keysInMonth }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Devam Eden Taksit Planları (${filteredActivePlans.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (uiState.selectedForecastMonth != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.applePressScale {
                                        onSelectForecastMonth(null)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${Formatters.formatYearMonthDisplay(uiState.selectedForecastMonth)} ✕",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = filteredActivePlans,
                        key = { "inst_plan_${it.planKey}" }
                    ) { plan ->
                        ActiveInstallmentPlanCard(
                            plan = plan,
                            isPrivacyMode = uiState.isPrivacyModeEnabled
                        )
                    }

                    // 4. Completed Plans Section (if any)
                    if (projection.completedPlans.isNotEmpty() && uiState.selectedForecastMonth == null) {
                        item {
                            Text(
                                text = "Bu Dönem Tamamlanan Taksitler (${projection.completedPlans.size})",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(
                            items = projection.completedPlans,
                            key = { "completed_plan_${it.planKey}" }
                        ) { compPlan ->
                            CompletedInstallmentPlanCard(
                                plan = compPlan,
                                isPrivacyMode = uiState.isPrivacyModeEnabled
                            )
                        }
                    }
                } else {
                    item {
                        InstallmentEmptyCard(onPickStatementFile = onPickStatementFile)
                    }
                }
            }

            PlanningTabMode.RECURRING -> {
                val radar = uiState.subscriptionRadar
                if (radar != null && radar.totalActiveCount > 0) {
                    // 1. Hero Recurring Payments KPI Card
                    item {
                        RecurringPaymentsHeroCard(
                            radar = radar,
                            isPrivacyMode = uiState.isPrivacyModeEnabled
                        )
                    }

                    // 2. Nearest Renewal / Upcoming Pill
                    if (radar.nextUpcomingPayment != null) {
                        item {
                            SubscriptionUpcomingCard(
                                nextUpcoming = radar.nextUpcomingPayment,
                                isPrivacyMode = uiState.isPrivacyModeEnabled
                            )
                        }
                    }

                    // 3. Category Filter Tabs: Tümü / Abonelikler / Tekrarlı Harcamalar
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = uiState.selectedRecurringTabFilter == null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRecurringTabFilter(null)
                                },
                                label = { Text("Tümü (${radar.totalActiveCount})") }
                            )

                            FilterChip(
                                selected = uiState.selectedRecurringTabFilter == RecurringPaymentType.SUBSCRIPTION,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRecurringTabFilter(RecurringPaymentType.SUBSCRIPTION)
                                },
                                label = { Text("Abonelikler (${radar.subscriptionsCount})") }
                            )

                            FilterChip(
                                selected = uiState.selectedRecurringTabFilter == RecurringPaymentType.RECURRING_EXPENSE,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRecurringTabFilter(RecurringPaymentType.RECURRING_EXPENSE)
                                },
                                label = { Text("Tekrarlı Harcamalar (${radar.recurringExpensesCount})") }
                            )
                        }
                    }

                    // 4. Filtered List of Recurring Payments
                    if (uiState.selectedRecurringTabFilter == null) {
                        // Section A: Sabit Günlü Abonelikler
                        if (radar.subscriptions.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Abonelikler (${radar.subscriptionsCount})",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Her ayın sabit gününde çekilen dijital ve servis abonelikleri.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(
                                items = radar.subscriptions,
                                key = { "sub_${it.id}" }
                            ) { subItem ->
                                RecurringPaymentCard(
                                    item = subItem,
                                    isPrivacyMode = uiState.isPrivacyModeEnabled
                                )
                            }
                        }

                        // Section B: Tekrarlı Harcamalar (Değişken Günlü)
                        if (radar.recurringExpenses.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Tekrarlı Harcamalar (${radar.recurringExpensesCount})",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Her ayın aynı gününde olmak zorunda olmayan, periyodik/sık tekrarlanan harcamalar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(
                                items = radar.recurringExpenses,
                                key = { "exp_${it.id}" }
                            ) { expItem ->
                                RecurringPaymentCard(
                                    item = expItem,
                                    isPrivacyMode = uiState.isPrivacyModeEnabled
                                )
                            }
                        }
                    } else if (uiState.selectedRecurringTabFilter == RecurringPaymentType.SUBSCRIPTION) {
                        item {
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Abonelikler (${radar.subscriptionsCount})",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Her ayın sabit gününde çekilen dijital ve servis abonelikleri.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            items = radar.subscriptions,
                            key = { "sub_${it.id}" }
                        ) { subItem ->
                            RecurringPaymentCard(
                                item = subItem,
                                isPrivacyMode = uiState.isPrivacyModeEnabled
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Tekrarlı Harcamalar (${radar.recurringExpensesCount})",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Her ayın aynı gününde olmak zorunda olmayan, periyodik/sık tekrarlanan harcamalar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            items = radar.recurringExpenses,
                            key = { "exp_${it.id}" }
                        ) { expItem ->
                            RecurringPaymentCard(
                                item = expItem,
                                isPrivacyMode = uiState.isPrivacyModeEnabled
                            )
                        }
                    }
                } else {
                    item {
                        RecurringPaymentsEmptyCard(
                            onPickStatementFile = onPickStatementFile
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentHeroCard(
    projection: InstallmentCashflowProjection,
    isPrivacyMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "GELECEK TAKSİT YÜKÜ & NAKİT AKIŞI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = Formatters.formatCurrency(projection.totalFutureDebt, isMasked = isPrivacyMode),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mevcut ${projection.totalActivePlansCount} taksit tamamlanana kadarki net toplam nakit çıkışı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComparisonMiniStat(
                    title = "Önümüzdeki Ay",
                    value = Formatters.formatCurrency(projection.nextMonthCommitment, isMasked = isPrivacyMode)
                )
                ComparisonMiniStat(
                    title = "Aktif Plan",
                    value = "${projection.totalActivePlansCount} Adet"
                )
                ComparisonMiniStat(
                    title = "Bitiş Tarihi",
                    value = projection.maxEndYearMonth?.let { Formatters.formatYearMonthDisplay(it) } ?: "-"
                )
            }
        }
    }
}

@Composable
private fun MonthlyInstallmentTimeline(
    forecasts: List<MonthlyInstallmentForecast>,
    selectedMonth: String?,
    onSelectMonth: (String?) -> Unit,
    isPrivacyMode: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val maxCommitment = remember(forecasts) {
        forecasts.maxOfOrNull { it.netInstallmentCommitment }?.coerceAtLeast(1.0) ?: 1.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aylık Taksit Dağılımı",
                style = MaterialTheme.typography.titleLarge
            )
            if (selectedMonth != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.applePressScale {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMonth(null)
                    }
                ) {
                    Text(
                        text = "Filtreyi Temizle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            forecasts.forEach { forecast ->
                val isSelected = forecast.yearMonth == selectedMonth
                val loadRatio = (forecast.netInstallmentCommitment / maxCommitment).toFloat().coerceIn(0.08f, 1f)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .applePressScale {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectMonth(if (isSelected) null else forecast.yearMonth)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = forecast.monthLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = Formatters.formatCurrency(forecast.netInstallmentCommitment, isMasked = isPrivacyMode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${forecast.activePlansCount} plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (forecast.refundInstallmentTotal > 0.0) {
                                Text(
                                    text = "-${Formatters.formatCurrency(forecast.refundInstallmentTotal, isMasked = isPrivacyMode)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleGreenLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { loadRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveInstallmentPlanCard(
    plan: ActiveInstallmentPlan,
    isPrivacyMode: Boolean
) {
    val catColor = plan.category?.let { Color.fromHex(it.colorHex) }
        ?: MaterialTheme.colorScheme.primary
    val progress = (plan.latestCurrentInstallment.toFloat() / plan.totalInstallments.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Category Icon + Merchant Title & Card Info + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(plan.category?.iconName ?: "HelpOutline"),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plan.merchantDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = plan.cardInfo ?: "Bilinmeyen Kart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(plan.monthlyAmount, isMasked = isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.type == TransactionType.REFUND) AppleGreenLight else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (plan.type == TransactionType.REFUND) "(İade Taksiti)" else "/ ay",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (plan.type == TransactionType.REFUND) AppleGreenLight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Installment Progress Bar Row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${plan.latestCurrentInstallment} / ${plan.totalInstallments} Taksit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "%${(progress * 100).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = catColor,
                    trackColor = catColor.copy(alpha = 0.16f)
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )

            // Bottom Badges: Remaining Count, Remaining Balance, Completion Month
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${plan.remainingInstallments} kaldı",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Kalan: ${Formatters.formatCurrency(plan.remainingBalance, isMasked = isPrivacyMode)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Bitiş: ${Formatters.formatYearMonthDisplay(plan.endYearMonth)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompletedInstallmentPlanCard(
    plan: ActiveInstallmentPlan,
    isPrivacyMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AppleGreenLight,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = plan.merchantDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tamamlandı (${plan.totalInstallments}/${plan.totalInstallments} Taksit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGreenLight
                    )
                }
            }

            Text(
                text = Formatters.formatCurrency(plan.totalPlanAmount, isMasked = isPrivacyMode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstallmentEmptyCard(
    onPickStatementFile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "Aktif Taksit Planı Bulunamadı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Yüklenen ekstrelerde taksitli bir harcama (ör. 01/06) tespit edilemedi. Taksitli işlemler içeren bir ekstre yüklediğinizde, kalan taksitler ve aylık nakit akışı burada otomatik hesaplanacaktır.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onPickStatementFile,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekstre Yükle")
            }
        }
    }
}

@Composable
private fun RecurringPaymentsHeroCard(
    radar: RecurringPaymentsSummary,
    isPrivacyMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "TEKRARLANAN ÖDEMELER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = Formatters.formatCurrency(radar.totalMonthlyCommitment, isMasked = isPrivacyMode),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Aylık tespit edilen düzenli harcama ve servis ödemesi toplamı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sub-category Breakdown Badges (Abonelikler vs Tekrarlı Harcamalar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sabit Abonelik",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${radar.subscriptionsCount} adet",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = Formatters.formatCurrency(radar.subscriptionsMonthlyTotal, isMasked = isPrivacyMode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tekrarlı Harcama",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${radar.recurringExpensesCount} adet",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = Formatters.formatCurrency(radar.recurringExpensesMonthlyTotal, isMasked = isPrivacyMode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            val avgMonthly = if (radar.totalActiveCount > 0) radar.totalMonthlyCommitment / radar.totalActiveCount else 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComparisonMiniStat(
                    title = "Ortalama Tutar",
                    value = Formatters.formatCurrency(avgMonthly, isMasked = isPrivacyMode)
                )
                ComparisonMiniStat(
                    title = "Fiyat Artışı",
                    value = if (radar.priceIncreasedCount > 0) "${radar.priceIncreasedCount} Zam" else "Değişim Yok"
                )
                ComparisonMiniStat(
                    title = "Yıllık Projeksiyon",
                    value = Formatters.formatCurrency(radar.totalAnnualCommitment, isMasked = isPrivacyMode)
                )
            }
        }
    }
}

@Composable
private fun SubscriptionUpcomingCard(
    nextUpcoming: RecurringPaymentItem,
    isPrivacyMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "En Yakın: ${nextUpcoming.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (nextUpcoming.isFixedBillingDay && nextUpcoming.paymentDayOfMonth != null) {
                            "${Formatters.formatCurrency(nextUpcoming.monthlyAmount, isMasked = isPrivacyMode)} • Her ayın ${nextUpcoming.paymentDayOfMonth}'i"
                        } else {
                            "${Formatters.formatCurrency(nextUpcoming.monthlyAmount, isMasked = isPrivacyMode)} • Değişken gün"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (nextUpcoming.daysUntilNextBilling != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = when (nextUpcoming.daysUntilNextBilling) {
                            0 -> "Bugün"
                            1 -> "Yarın"
                            else -> "${nextUpcoming.daysUntilNextBilling} gün kaldı"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringPaymentCard(
    item: RecurringPaymentItem,
    isPrivacyMode: Boolean
) {
    val brandColor = item.brandColorHex?.let { Color(it) }
        ?: item.category?.let { Color.fromHex(it.colorHex) }
        ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Brand Icon + Title/Subtitle Column + Monthly Amount Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(brandColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(item.category?.iconName ?: if (item.paymentType == RecurringPaymentType.SUBSCRIPTION) "Subscriptions" else "Repeat"),
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.isFixedBillingDay && item.paymentDayOfMonth != null) {
                            "Her ayın ${item.paymentDayOfMonth}'i • Sabit Gün"
                        } else {
                            item.frequencyDescription
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(item.monthlyAmount, isMasked = isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.paymentType == RecurringPaymentType.SUBSCRIPTION) "aylık" else "aylık ort.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )

            // Row 2: Badges and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side tags: Category type badge + Zam badge + optional Card Info
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.paymentType == RecurringPaymentType.SUBSCRIPTION) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    ) {
                        Text(
                            text = if (item.paymentType == RecurringPaymentType.SUBSCRIPTION) "Abonelik" else "Tekrarlı",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.paymentType == RecurringPaymentType.SUBSCRIPTION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Price change badge
                    if (item.isPriceIncreased && item.priceChangePercent != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppleOrangeLight.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    tint = AppleOrangeLight,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "+%${"%.0f".format(item.priceChangePercent)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleOrangeLight
                                )
                            }
                        }
                    } else if (item.priceChangePercent != null && item.priceChangePercent < -0.5) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppleGreenLight.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "-%${"%.0f".format(kotlin.math.abs(item.priceChangePercent))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppleGreenLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Card info pill
                    if (!item.cardInfo.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = item.cardInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: Timing / Last action status
                val isUrgent = item.isFixedBillingDay && (item.daysUntilNextBilling ?: 99) <= 7
                val statusText = if (item.isFixedBillingDay && item.daysUntilNextBilling != null) {
                    when (item.daysUntilNextBilling) {
                        0 -> "Bugün yenileniyor"
                        1 -> "Yarın yenilenecek"
                        else -> "${item.daysUntilNextBilling} gün kaldı"
                    }
                } else {
                    "Son: ${Formatters.formatEpochDay(item.lastPaymentDateEpochDay)}"
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUrgent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUrgent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = if (isUrgent) 6.dp else 0.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringPaymentsEmptyCard(
    onPickStatementFile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "Tekrarlanan Ödeme Bulunamadı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Yüklenen ekstrelerde sabit abonelikler (Spotify, YouTube vb.) veya periyodik tekrarlı harcamalar tespit edilemedi. Yeni bir ekstre yükleyerek tekrarlanan ödemeler analizinizi başlatabilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onPickStatementFile,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekstre Yükle")
            }
        }
    }
}

@Composable
private fun ComparisonMiniStat(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
