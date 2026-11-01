package com.example.extreapp.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extreapp.data.local.entity.CategoryComparisonItem
import com.example.extreapp.data.local.entity.MonthOverMonthComparison
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.InsightsTabMode
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon
import java.util.Locale

@Composable
fun InsightsScreen(
    uiState: ExtreUiState,
    onSelectInsightsTabMode: (InsightsTabMode) -> Unit,
    onSelectComparisonTarget: (String) -> Unit,
    onSwapComparisonPeriods: () -> Unit,
    onPickStatementFile: () -> Unit,
    onSearchMerchantQueryChange: (String) -> Unit,
    onSelectMerchant: (Long) -> Unit,
    onBackFromMerchantDetail: () -> Unit,
    onEditMerchant: (merchantId: Long, newDisplayName: String, newCategoryId: Long) -> Unit,
    onAddKeywordRuleForMerchant: (keyword: String, merchantName: String, categoryId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // If viewing a specific merchant's detail, show detail directly (with back button)
    if (uiState.selectedMerchantDetail != null) {
        MerchantsScreen(
            uiState = uiState,
            onSearchQueryChange = onSearchMerchantQueryChange,
            onSelectMerchant = onSelectMerchant,
            onBackFromDetail = onBackFromMerchantDetail,
            onEditMerchant = onEditMerchant,
            onAddKeywordRuleForMerchant = onAddKeywordRuleForMerchant,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 1. Top Apple-style 2-Segmented Switcher: Aydan Aya Kıyasla vs Satıcı Analitiği
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isComparison = uiState.insightsTabMode == InsightsTabMode.COMPARISON
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isComparison) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isComparison) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .applePressScale {
                            if (!isComparison) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectInsightsTabMode(InsightsTabMode.COMPARISON)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isComparison) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aydan Aya Kıyasla",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isComparison) FontWeight.Bold else FontWeight.Medium,
                            color = if (isComparison) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val isMerchants = uiState.insightsTabMode == InsightsTabMode.MERCHANTS
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMerchants) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isMerchants) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .applePressScale {
                            if (!isMerchants) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectInsightsTabMode(InsightsTabMode.MERCHANTS)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isMerchants) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Satıcı Analitiği",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isMerchants) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMerchants) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (uiState.insightsTabMode) {
            InsightsTabMode.COMPARISON -> {
                val comparison = uiState.monthOverMonthComparison
                if (comparison != null && uiState.availablePeriods.size >= 2) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Period Selector & Swap Bar
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
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ComparisonInsufficientDataCard(onPickStatementFile = onPickStatementFile)
                    }
                }
            }

            InsightsTabMode.MERCHANTS -> {
                MerchantsScreen(
                    uiState = uiState,
                    onSearchQueryChange = onSearchMerchantQueryChange,
                    onSelectMerchant = onSelectMerchant,
                    onBackFromDetail = onBackFromMerchantDetail,
                    onEditMerchant = onEditMerchant,
                    onAddKeywordRuleForMerchant = onAddKeywordRuleForMerchant,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
                Column {
                    Text(
                        text = "Brüt Harcama",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val diff = comparison.baseGrossExpense - comparison.targetGrossExpense
                    Text(
                        text = if (isPrivacyMode) "***" else "${if (diff > 0) "+" else ""}${Formatters.formatCurrency(diff)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "İade Farkı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val diff = comparison.baseRefundTotal - comparison.targetRefundTotal
                    Text(
                        text = if (isPrivacyMode) "***" else "${if (diff > 0) "+" else ""}${Formatters.formatCurrency(diff)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "İşlem Adedi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${comparison.baseTransactionCount} vs ${comparison.targetTransactionCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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

                Spacer(modifier = Modifier.width(8.dp))

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
                text = "Aydan aya harcama karşılaştırması yapabilmek için sistemde en az iki farklı ekstre dönemi bulunmalıdır. Bir ekstre daha yükleyerek aydan aya karşılaştırmayı hemen başlatabilirsiniz.",
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
