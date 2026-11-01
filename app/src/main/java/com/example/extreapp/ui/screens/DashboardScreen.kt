package com.example.extreapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extreapp.data.local.entity.ActiveInstallmentPlan
import com.example.extreapp.data.local.entity.CategoryBreakdownItem
import com.example.extreapp.data.local.entity.CategoryComparisonItem
import com.example.extreapp.data.local.entity.DashboardAnalyticsMode
import com.example.extreapp.data.local.entity.InstallmentCashflowProjection
import com.example.extreapp.data.local.entity.MonthOverMonthComparison
import com.example.extreapp.data.local.entity.MonthlyInstallmentForecast
import com.example.extreapp.data.local.entity.PeriodFilterMode
import com.example.extreapp.data.local.entity.TransactionSortOption
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.data.local.entity.TransactionWithDetails
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleFluidMotion
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: ExtreUiState,
    onPickStatementFile: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onSelectPeriodMode: (PeriodFilterMode) -> Unit,
    onToggleCategoryFilter: (Long?) -> Unit,
    onToggleSortByDate: () -> Unit = {},
    onToggleSortByAmount: () -> Unit = {},
    onToggleComparisonMode: () -> Unit = {},
    onSelectDashboardMode: (DashboardAnalyticsMode) -> Unit = {},
    onSelectForecastMonth: (String?) -> Unit = {},
    onSelectComparisonTarget: (String) -> Unit = {},
    onSwapComparisonPeriods: () -> Unit = {},
    onOpenApprovalQueue: () -> Unit,
    onOpenMerchantDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val overview = uiState.monthlyOverview

    if (uiState.totalTransactionCount == 0 || overview == null) {
        EmptyDashboardOnboarding(
            onPickStatementFile = onPickStatementFile,
            modifier = modifier
        )
        return
    }

    val filteredTransactions = overview.transactions.filter { item ->
        if (uiState.selectedCategoryIdFilter == null) {
            true
        } else if (uiState.selectedCategoryIdFilter == -1L) {
            item.category == null && (item.transaction.type == TransactionType.EXPENSE || item.transaction.type == TransactionType.REFUND)
        } else {
            item.category?.id == uiState.selectedCategoryIdFilter
        }
    }

    val sortedTransactions = remember(filteredTransactions, uiState.transactionSortOption) {
        when (uiState.transactionSortOption) {
            TransactionSortOption.DATE_DESC -> filteredTransactions.sortedWith(
                compareByDescending<TransactionWithDetails> { it.transaction.dateEpochDay }
                    .thenByDescending { it.transaction.id }
            )
            TransactionSortOption.DATE_ASC -> filteredTransactions.sortedWith(
                compareBy<TransactionWithDetails> { it.transaction.dateEpochDay }
                    .thenBy { it.transaction.id }
            )
            TransactionSortOption.AMOUNT_DESC -> filteredTransactions.sortedWith(
                compareByDescending<TransactionWithDetails> { it.transaction.amount }
                    .thenByDescending { it.transaction.dateEpochDay }
            )
            TransactionSortOption.AMOUNT_ASC -> filteredTransactions.sortedWith(
                compareBy<TransactionWithDetails> { it.transaction.amount }
                    .thenByDescending { it.transaction.dateEpochDay }
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Statement Metadata & Period Mode Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.periodFilterMode == PeriodFilterMode.STATEMENT_PERIOD,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectPeriodMode(PeriodFilterMode.STATEMENT_PERIOD)
                            },
                            label = { Text("Ekstre Dönemi") }
                        )
                        FilterChip(
                            selected = uiState.periodFilterMode == PeriodFilterMode.TRANSACTION_MONTH,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectPeriodMode(PeriodFilterMode.TRANSACTION_MONTH)
                            },
                            label = { Text("İşlem Ayı") }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onPickStatementFile,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekstre Yükle", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Horizontal Period Pills
                if (uiState.availablePeriods.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availablePeriods.forEach { periodKey ->
                            val isSelected = periodKey == uiState.selectedPeriod
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                modifier = Modifier.applePressScale {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectPeriod(periodKey)
                                }
                            ) {
                                Text(
                                    text = Formatters.formatYearMonthDisplay(periodKey),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Mode switcher: "Dönem Özeti" vs "Aydan Aya Kıyasla" vs "Taksit Takvimi"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.dashboardMode == DashboardAnalyticsMode.OVERVIEW,
                        onClick = {
                            if (uiState.dashboardMode != DashboardAnalyticsMode.OVERVIEW) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectDashboardMode(DashboardAnalyticsMode.OVERVIEW)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DonutLarge,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Dönem Özeti") }
                    )

                    FilterChip(
                        selected = uiState.dashboardMode == DashboardAnalyticsMode.COMPARISON,
                        onClick = {
                            if (uiState.dashboardMode != DashboardAnalyticsMode.COMPARISON) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectDashboardMode(DashboardAnalyticsMode.COMPARISON)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Aydan Aya Kıyasla") }
                    )

                    FilterChip(
                        selected = uiState.dashboardMode == DashboardAnalyticsMode.INSTALLMENTS,
                        onClick = {
                            if (uiState.dashboardMode != DashboardAnalyticsMode.INSTALLMENTS) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectDashboardMode(DashboardAnalyticsMode.INSTALLMENTS)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Taksit Takvimi") }
                    )
                }
            }
        }

        when (uiState.dashboardMode) {
            DashboardAnalyticsMode.COMPARISON -> {
                val comparison = uiState.monthOverMonthComparison
                if (comparison != null) {
                    // 1. Comparison Period Selector & Swap Bar
                    item {
                        MonthOverMonthPeriodSelectorBar(
                            comparison = comparison,
                            availablePeriods = uiState.availablePeriods,
                            onSelectTarget = onSelectComparisonTarget,
                            onSwap = onSwapComparisonPeriods
                        )
                    }

                    // 2. Hero Month-over-Month Comparison Card
                    item {
                        MonthOverMonthHeroCard(
                            comparison = comparison,
                            isPrivacyMode = uiState.isPrivacyModeEnabled
                        )
                    }

                    // 3. Highlights: Top Increased & Top Savings Categories
                    if (comparison.topIncreasedCategory != null || comparison.topDecreasedCategory != null) {
                        item {
                            MonthOverMonthHighlightsCard(
                                comparison = comparison,
                                isPrivacyMode = uiState.isPrivacyModeEnabled
                            )
                        }
                    }

                    // 4. Category-by-Category Comparison Breakdown Header & Cards
                    item {
                        Text(
                            text = "Kategori Bazında Harcama Farkları",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(
                        items = comparison.categoryComparisons,
                        key = { "cmp_cat_${it.category.id}" }
                    ) { cmpItem ->
                        CategoryComparisonCard(
                            item = cmpItem,
                            isPrivacyMode = uiState.isPrivacyModeEnabled
                        )
                    }
                } else {
                    // Insufficient data (< 2 periods)
                    item {
                        ComparisonInsufficientDataCard(
                            onPickStatementFile = onPickStatementFile
                        )
                    }
                }
            }
            DashboardAnalyticsMode.INSTALLMENTS -> {
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
            DashboardAnalyticsMode.OVERVIEW -> {
            // 2. Unapproved Queue Call-to-Action Banner
            if (uiState.unapprovedTotalCount > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppleOrangeLight.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = AppleOrangeLight.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .applePressScale {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenApprovalQueue()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppleOrangeLight.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PendingActions,
                                    contentDescription = null,
                                    tint = AppleOrangeLight
                                )
                            }
                            Column {
                                Text(
                                    text = "${uiState.unapprovedTotalCount} Tanımsız İşlem Onay Bekliyor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Satıcı ve kategori atayarak otomatik kural oluşturmak için dokunun",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = AppleOrangeLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Hero Monthly Summary + Donut Chart Card
        item {
            MonthlyHeroSummaryCard(
                overview = overview,
                isPrivacyMode = uiState.isPrivacyModeEnabled,
                cardInfo = uiState.latestStatementMetadata?.cardInfo,
                statementDate = uiState.latestStatementMetadata?.statementDate,
                selectedCategoryId = uiState.selectedCategoryIdFilter,
                onSelectCategory = { catId ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleCategoryFilter(catId)
                }
            )
        }

        // 4. Category Breakdown Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aylık Kategori Dağılımı",
                    style = MaterialTheme.typography.titleLarge
                )
                if (uiState.selectedCategoryIdFilter != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.applePressScale {
                            onToggleCategoryFilter(null)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterAltOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Filtreyi Temizle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 5. Category Breakdown Cards
        items(
            items = overview.categoryBreakdowns,
            key = { "cat_${it.category.id}" }
        ) { item ->
            CategoryBreakdownRowCard(
                item = item,
                isPrivacyMode = uiState.isPrivacyModeEnabled,
                isSelected = (uiState.selectedCategoryIdFilter == item.category.id),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleCategoryFilter(item.category.id)
                }
            )
        }

        // 6. Period Transactions Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dönem İşlemleri (${sortedTransactions.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDateActive = uiState.transactionSortOption.isDate
                    val isDateAsc = uiState.transactionSortOption == TransactionSortOption.DATE_ASC
                    FilterChip(
                        selected = isDateActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSortByDate()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isDateActive && isDateAsc) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (isDateActive) {
                                    if (isDateAsc) "Tarih (En Eski)" else "Tarih (En Yeni)"
                                } else {
                                    "Tarih"
                                }
                            )
                        }
                    )

                    val isAmountActive = uiState.transactionSortOption.isAmount
                    val isAmountAsc = uiState.transactionSortOption == TransactionSortOption.AMOUNT_ASC
                    FilterChip(
                        selected = isAmountActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSortByAmount()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isAmountActive && isAmountAsc) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (isAmountActive) {
                                    if (isAmountAsc) "Fiyat (En Düşük)" else "Fiyat (En Yüksek)"
                                } else {
                                    "Fiyat"
                                }
                            )
                        }
                    )
                }
            }
        }

        items(
            items = sortedTransactions,
            key = { "tx_${it.transaction.id}" }
        ) { txDetail ->
            TransactionListItemCard(
                detail = txDetail,
                isPrivacyMode = uiState.isPrivacyModeEnabled,
                onClickMerchant = { merchantId ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenMerchantDetail(merchantId)
                },
                onClickUnapproved = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenApprovalQueue()
                }
            )
        }
        }
    }
    }
}

@Composable
private fun EmptyDashboardOnboarding(
    onPickStatementFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "ExtreApp'e Hoş Geldiniz",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Banka kredi kartı veya hesap ekstrenizi (CSV, Excel veya PDF) yükleyerek tamamen çevrimdışı harcama analizi yapın ve kural tabanlı otomatik sınıflandırma oluşturun.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onPickStatementFile,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekstre Dosyası Seç (CSV / XLSX / PDF)")
                }
            }
        }
    }
}

@Composable
private fun MonthlyHeroSummaryCard(
    overview: com.example.extreapp.data.local.entity.MonthlyOverviewSummary,
    isPrivacyMode: Boolean = false,
    cardInfo: String?,
    statementDate: String?,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit
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
            if (cardInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = cardInfo,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    statementDate?.let {
                        Text(
                            text = "Kesim: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "${Formatters.formatYearMonthDisplay(overview.periodKey)} Net Harcama",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPrivacyMode) "***" else Formatters.formatCurrency(overview.netExpenseTotal),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text(
                                text = "Brüt Harcama",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isPrivacyMode) "***" else Formatters.formatCurrency(overview.grossExpenseTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (overview.refundTotal > 0.0) {
                            Column {
                                Text(
                                    text = "İadeler",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleGreenLight
                                )
                                Text(
                                    text = if (isPrivacyMode) "***" else "-${Formatters.formatCurrency(overview.refundTotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleGreenLight
                                )
                            }
                        }
                    }
                }

                // Donut Chart
                if (overview.categoryBreakdowns.isNotEmpty()) {
                    Box(
                        modifier = Modifier.size(116.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryDonutChart(
                            items = overview.categoryBreakdowns,
                            selectedCategoryId = selectedCategoryId,
                            modifier = Modifier.size(104.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${overview.categoryBreakdowns.sumOf { it.transactionCount }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "işlem",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (overview.paymentTotal > 0.0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dönem İçi Ekstre Borç Ödemesi (Harcama Dışı)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "***" else Formatters.formatCurrency(overview.paymentTotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleGreenLight
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDonutChart(
    items: List<CategoryBreakdownItem>,
    selectedCategoryId: Long?,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = AppleFluidMotion.momentumSpring(),
        label = "donutReveal"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 16.dp.toPx()
        var startAngle = -90f
        val gapDegrees = if (items.size > 1) 3f else 0f

        items.forEach { slice ->
            val sweep = ((slice.percentageOfTotal / 100f) * 360f * progress - gapDegrees).coerceAtLeast(1.5f)
            val isHighlighted = selectedCategoryId == null || selectedCategoryId == slice.category.id
            val baseColor = Color.fromHex(slice.category.colorHex)
            drawArc(
                color = if (isHighlighted) baseColor else baseColor.copy(alpha = 0.25f),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = if (selectedCategoryId == slice.category.id) strokeWidth * 1.18f else strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += (slice.percentageOfTotal / 100f) * 360f * progress
        }
    }
}

@Composable
private fun CategoryBreakdownRowCard(
    item: CategoryBreakdownItem,
    isPrivacyMode: Boolean = false,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val catColor = Color.fromHex(item.category.colorHex)
    val animatedProgress by animateFloatAsState(
        targetValue = (item.percentageOfTotal / 100f).coerceIn(0f, 1f),
        animationSpec = AppleFluidMotion.criticallyDampedSpring(),
        label = "catBar"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                catColor.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, catColor, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .applePressScale(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(item.category.iconName),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.category.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = buildString {
                                append("${item.transactionCount} işlem • %")
                                append(String.format(Locale.forLanguageTag("tr-TR"), "%.1f", item.percentageOfTotal))
                                if (item.refundTotal > 0.0) {
                                    if (isPrivacyMode) {
                                        append(" (*** iade)")
                                    } else {
                                        append(" (${Formatters.formatCurrency(item.refundTotal)} iade düşüldü)")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = if (isPrivacyMode) "***" else Formatters.formatCurrency(item.netExpense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = catColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionListItemCard(
    detail: TransactionWithDetails,
    isPrivacyMode: Boolean = false,
    onClickMerchant: (Long) -> Unit,
    onClickUnapproved: () -> Unit
) {
    val tx = detail.transaction
    val catColor = detail.category?.let { Color.fromHex(it.colorHex) } ?: Color(0xFF94A3B8)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .applePressScale {
                when {
                    detail.merchant != null -> onClickMerchant(detail.merchant.id)
                    !tx.isApproved -> onClickUnapproved()
                }
            }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(detail.category?.iconName ?: "HelpOutline"),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = detail.merchant?.displayName ?: tx.cleanedDescription.ifEmpty { tx.rawDescription },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (detail.merchant != null) {
                        Text(
                            text = tx.rawDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Formatters.formatEpochDay(tx.dateEpochDay),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        detail.category?.let { cat ->
                            BadgePill(text = cat.name, color = catColor)
                        } ?: run {
                            if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.REFUND) {
                                BadgePill(text = "Onay Bekliyor", color = AppleOrangeLight)
                            }
                        }

                        if (tx.installmentCurrent != null && tx.installmentTotal != null) {
                            BadgePill(
                                text = "${tx.installmentCurrent}/${tx.installmentTotal} Taksit",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        tx.foreignCurrencyNote?.let { fx ->
                            BadgePill(
                                text = if (isPrivacyMode) "***" else fx,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (tx.type == TransactionType.REFUND) {
                            BadgePill(text = "İADE", color = AppleGreenLight)
                        } else if (tx.type == TransactionType.PAYMENT) {
                            BadgePill(text = "BORÇ ÖDEMESİ", color = AppleGreenLight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                val amountColor = when (tx.type) {
                    TransactionType.REFUND, TransactionType.PAYMENT -> AppleGreenLight
                    TransactionType.REWARD_INFO -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
                }
                val prefix = if (tx.type == TransactionType.REFUND || tx.type == TransactionType.PAYMENT) "-" else ""
                Text(
                    text = if (isPrivacyMode) "***" else "$prefix${Formatters.formatCurrency(tx.amount, tx.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                tx.rewardPointsText?.let { reward ->
                    Text(
                        text = if (isPrivacyMode) "+*** Chip" else "+$reward Chip",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGreenLight
                    )
                }
            }
        }
    }
}

@Composable
fun BadgePill(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MonthOverMonthPeriodSelectorBar(
    comparison: MonthOverMonthComparison,
    availablePeriods: List<String>,
    onSelectTarget: (String) -> Unit,
    onSwap: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Base Period Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Temel Dönem",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = Formatters.formatYearMonthDisplay(comparison.basePeriodKey),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Swap Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSwap()
                    },
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .applePressScale { onSwap() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "Dönemleri Değiştir",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Target Period Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Kıyaslanan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = Formatters.formatYearMonthDisplay(comparison.targetPeriodKey),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Target Selector Chips (if > 2 periods available)
            val otherTargets = availablePeriods.filter { it != comparison.basePeriodKey }
            if (otherTargets.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kıyasla:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    otherTargets.forEach { periodKey ->
                        val isSelected = periodKey == comparison.targetPeriodKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.applePressScale {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTarget(periodKey)
                            }
                        ) {
                            Text(
                                text = Formatters.formatYearMonthDisplay(periodKey),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthOverMonthHeroCard(
    comparison: MonthOverMonthComparison,
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
            Text(
                text = "NET HARCAMA KARŞILAŞTIRMASI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            // Side by Side Main Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = Formatters.formatYearMonthDisplay(comparison.basePeriodKey),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "***" else Formatters.formatCurrency(comparison.baseNetExpense),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatYearMonthDisplay(comparison.targetPeriodKey),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "***" else Formatters.formatCurrency(comparison.targetNetExpense),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Central Net Difference Pill
            val isIncrease = comparison.netExpenseDiff > 0.01
            val isDecrease = comparison.netExpenseDiff < -0.01
            val diffColor = when {
                isIncrease -> AppleOrangeLight
                isDecrease -> AppleGreenLight
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = diffColor.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isIncrease -> Icons.Outlined.ArrowUpward
                                isDecrease -> Icons.Outlined.ArrowDownward
                                else -> Icons.AutoMirrored.Outlined.TrendingFlat
                            },
                            contentDescription = null,
                            tint = diffColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when {
                                isIncrease -> "Net Harcama Artışı"
                                isDecrease -> "Net Harcama Tasarrufu"
                                else -> "Net Değişim Yok"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = diffColor
                        )
                    }

                    Text(
                        text = if (isPrivacyMode) "***" else {
                            val sign = if (isIncrease) "+" else ""
                            val pctText = comparison.netExpensePercentageChange?.let {
                                " (${if (it > 0) "+" else ""}${String.format(Locale.US, "%.1f", it)}%)"
                            } ?: ""
                            "$sign${Formatters.formatCurrency(comparison.netExpenseDiff)}$pctText"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Sub-metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComparisonMiniStat(
                    title = "Brüt Harcama",
                    value = if (isPrivacyMode) "***" else {
                        val diff = comparison.baseGrossExpense - comparison.targetGrossExpense
                        "${if (diff > 0) "+" else ""}${Formatters.formatCurrency(diff)}"
                    }
                )
                ComparisonMiniStat(
                    title = "İade Farkı",
                    value = if (isPrivacyMode) "***" else {
                        val diff = comparison.baseRefundTotal - comparison.targetRefundTotal
                        "${if (diff > 0) "+" else ""}${Formatters.formatCurrency(diff)}"
                    }
                )
                ComparisonMiniStat(
                    title = "İşlem Adedi",
                    value = "${comparison.baseTransactionCount} vs ${comparison.targetTransactionCount}"
                )
            }
        }
    }
}

@Composable
private fun MonthOverMonthHighlightsCard(
    comparison: MonthOverMonthComparison,
    isPrivacyMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Increased
        comparison.topIncreasedCategory?.let { item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppleOrangeLight.copy(alpha = 0.1f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = AppleOrangeLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "En Çok Artan",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleOrangeLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPrivacyMode) "***" else "+${Formatters.formatCurrency(item.diffAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleOrangeLight
                    )
                }
            }
        }

        // Top Decreased
        comparison.topDecreasedCategory?.let { item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppleGreenLight.copy(alpha = 0.1f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = AppleGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "En Çok Azalan",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPrivacyMode) "***" else Formatters.formatCurrency(item.diffAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleGreenLight
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryComparisonCard(
    item: CategoryComparisonItem,
    isPrivacyMode: Boolean
) {
    val catColor = Color.fromHex(item.category.colorHex)
    val isIncrease = item.diffAmount > 0.01
    val isDecrease = item.diffAmount < -0.01
    val badgeColor = when {
        isIncrease -> AppleOrangeLight
        isDecrease -> AppleGreenLight
        item.percentageChange == null && item.baseNetExpense > 0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Category Icon + Name + Change Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = catColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = resolveCategoryIcon(item.category.iconName),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Change Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isIncrease) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                        } else if (isDecrease) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = when {
                                item.percentageChange != null -> {
                                    val pct = item.percentageChange
                                    "${if (pct > 0) "+" else ""}${String.format(Locale.US, "%.1f", pct)}%"
                                }
                                item.baseNetExpense > 0 -> "Yeni"
                                item.targetNetExpense > 0 -> "Sıfırlandı"
                                else -> "Aynı"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Dual Progress Comparison Bars
            val maxSpend = maxOf(item.baseNetExpense, item.targetNetExpense, 1.0)
            val baseFraction = (item.baseNetExpense / maxSpend).toFloat().coerceIn(0f, 1f)
            val targetFraction = (item.targetNetExpense / maxSpend).toFloat().coerceIn(0f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Base Month Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Temel: ${if (isPrivacyMode) "***" else Formatters.formatCurrency(item.baseNetExpense)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.baseTransactionCount} işlem",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { baseFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Target Month Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Önceki: ${if (isPrivacyMode) "***" else Formatters.formatCurrency(item.targetNetExpense)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.targetTransactionCount} işlem",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { targetFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ComparisonInsufficientDataCard(
    onPickStatementFile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
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
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "Kıyaslama İçin 2. Dönem Gerekli",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aydan aya harcama karşılaştırması yapabilmek için sistemde en az iki farklı ekstre dönemi bulunmalıdır. Ekstreler sekmesinden veya aşağıdaki butonla bir ekstre daha yükleyerek aydan aya karşılaştırmayı hemen başlatabilirsiniz.",
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
                Text("İkinci Ekstreyi Yükle")
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
                    value = projection.maxEndYearMonth?.let { Formatters.formatYearMonthDisplay(it) } ?: "Tamamlandı"
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AppleGreenLight.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = AppleGreenLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Aylık taksitler tamamlandıkça (ör. uçak biletleri ve alışverişler), aylık bütçe yükünüz kademeli olarak hafifleyecektir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(plan.monthlyAmount, isMasked = isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.type == TransactionType.REFUND) AppleGreenLight else MaterialTheme.colorScheme.onSurface
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
                        text = "${plan.remainingInstallments} taksit kaldı",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Kalan: ${Formatters.formatCurrency(plan.remainingBalance, isMasked = isPrivacyMode)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Bitiş: ${Formatters.formatYearMonthDisplay(plan.endYearMonth)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "Taksitli Harcama Bulunamadı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Yüklenen ekstrelerde devam eden veya tamamlanan taksitli işlem (05/09, 02/06 vb.) tespit edilemedi. Taksitli işlemler içeren yeni bir ekstre yükleyerek taksit takviminizi görüntüleyebilirsiniz.",
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

