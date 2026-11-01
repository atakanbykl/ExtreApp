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
import androidx.compose.material.icons.outlined.AddCircleOutline
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.extreapp.data.local.entity.CategoryBreakdownItem
import com.example.extreapp.data.local.entity.PeriodFilterMode
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
    onImportSampleHesapOzeti: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onSelectPeriodMode: (PeriodFilterMode) -> Unit,
    onToggleCategoryFilter: (Long?) -> Unit,
    onOpenApprovalQueue: () -> Unit,
    onOpenMerchantDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val overview = uiState.monthlyOverview

    if (uiState.totalTransactionCount == 0 || overview == null) {
        EmptyDashboardOnboarding(
            onPickStatementFile = onPickStatementFile,
            onImportSampleHesapOzeti = onImportSampleHesapOzeti,
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
            }
        }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dönem İşlemleri (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        items(
            items = filteredTransactions,
            key = { "tx_${it.transaction.id}" }
        ) { txDetail ->
            TransactionListItemCard(
                detail = txDetail,
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
private fun EmptyDashboardOnboarding(
    onPickStatementFile: () -> Unit,
    onImportSampleHesapOzeti: () -> Unit,
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

                OutlinedButton(
                    onClick = onImportSampleHesapOzeti,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HESAP_OZETI.csv Örneğini Yükle")
                }
            }
        }
    }
}

@Composable
private fun MonthlyHeroSummaryCard(
    overview: com.example.extreapp.data.local.entity.MonthlyOverviewSummary,
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
                        text = Formatters.formatCurrency(overview.netExpenseTotal),
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
                                text = Formatters.formatCurrency(overview.grossExpenseTotal),
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
                                    text = "-${Formatters.formatCurrency(overview.refundTotal)}",
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
                        text = Formatters.formatCurrency(overview.paymentTotal),
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
                                    append(" (${Formatters.formatCurrency(item.refundTotal)} iade düşüldü)")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = Formatters.formatCurrency(item.netExpense),
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

@Composable
fun TransactionListItemCard(
    detail: TransactionWithDetails,
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

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            BadgePill(text = fx, color = MaterialTheme.colorScheme.secondary)
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
                    text = "$prefix${Formatters.formatCurrency(tx.amount, tx.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                tx.rewardPointsText?.let { reward ->
                    Text(
                        text = "+$reward Chip",
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

