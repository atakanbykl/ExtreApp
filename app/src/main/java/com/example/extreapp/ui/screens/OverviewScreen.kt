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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extreapp.data.local.entity.CategoryBreakdownItem
import com.example.extreapp.data.local.entity.MonthlyOverviewSummary
import com.example.extreapp.data.local.entity.PeriodFilterMode
import com.example.extreapp.data.local.entity.TransactionSortOption
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.data.local.entity.TransactionWithDetails
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon

@Composable
fun OverviewScreen(
    uiState: ExtreUiState,
    onPickStatementFile: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onSelectPeriodMode: (PeriodFilterMode) -> Unit,
    onToggleCategoryFilter: (Long?) -> Unit,
    onToggleSortByDate: () -> Unit = {},
    onToggleSortByAmount: () -> Unit = {},
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
        // 1. Top Period Mode Selector & Ekstre Yükle Action
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
            }
        }

        // 2. Unapproved Queue Call-to-Action Banner (if any)
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

@Composable
internal fun EmptyDashboardOnboarding(
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
    overview: MonthlyOverviewSummary,
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
    val totalAmount = remember(items) { items.sumOf { it.netExpense } }
    if (totalAmount <= 0.0) return

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

        var startAngle = -90f
        items.forEach { item ->
            val sweep = ((item.netExpense / totalAmount) * 360f).toFloat()
            val baseColor = Color.fromHex(item.category.colorHex)
            val isSelected = selectedCategoryId == item.category.id
            val color = if (selectedCategoryId != null && !isSelected) {
                baseColor.copy(alpha = 0.25f)
            } else {
                baseColor
            }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = if (isSelected) strokeWidth * 1.25f else strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryBreakdownRowCard(
    item: CategoryBreakdownItem,
    isPrivacyMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val catColor = Color.fromHex(item.category.colorHex)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                catColor.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) catColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .applePressScale(onClick = onClick)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(item.category.iconName),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${item.transactionCount} işlem • %${String.format(java.util.Locale.US, "%.1f", item.percentageOfTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = if (isPrivacyMode) "***" else Formatters.formatCurrency(item.netExpense),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionListItemCard(
    detail: TransactionWithDetails,
    isPrivacyMode: Boolean = false,
    onClickMerchant: (Long) -> Unit,
    onClickUnapproved: () -> Unit
) {
    val tx = detail.transaction
    val cat = detail.category
    val merchant = detail.merchant

    val catColor = cat?.let { Color.fromHex(it.colorHex) } ?: MaterialTheme.colorScheme.outline

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .applePressScale {
                if (merchant != null) {
                    onClickMerchant(merchant.id)
                } else if (!tx.isApproved) {
                    onClickUnapproved()
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
                        imageVector = cat?.let { resolveCategoryIcon(it.iconName) }
                            ?: Icons.Outlined.Description,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = merchant?.displayName ?: tx.cleanedDescription,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatEpochDay(tx.dateEpochDay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        cat?.let {
                            Text(
                                text = "•  ${it.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Installment / Refund / Foreign Currency Badges
                    if (tx.installmentTotal != null || tx.type == TransactionType.REFUND || tx.foreignCurrencyNote != null || !tx.isApproved) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tx.installmentTotal != null && tx.installmentTotal > 1) {
                                BadgePill(
                                    text = "${tx.installmentCurrent}/${tx.installmentTotal} Taksit",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (tx.type == TransactionType.REFUND) {
                                BadgePill(
                                    text = "İade",
                                    color = AppleGreenLight
                                )
                            }
                            if (tx.foreignCurrencyNote != null) {
                                BadgePill(
                                    text = tx.foreignCurrencyNote,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (!tx.isApproved) {
                                BadgePill(
                                    text = "Onay Bekliyor",
                                    color = AppleOrangeLight
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                val isRefund = tx.type == TransactionType.REFUND
                val isPayment = tx.type == TransactionType.PAYMENT
                val amountColor = when {
                    isRefund || isPayment -> AppleGreenLight
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val prefix = when {
                    isRefund -> "-"
                    isPayment -> "-"
                    else -> ""
                }
                Text(
                    text = if (isPrivacyMode) "***" else "$prefix${Formatters.formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun BadgePill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
