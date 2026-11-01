package com.example.extreapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.MerchantMonthlyTotal
import com.example.extreapp.data.local.entity.MerchantSummaryItem
import com.example.extreapp.data.repository.MerchantDetailData
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleFluidMotion
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon

@Composable
fun MerchantsScreen(
    uiState: ExtreUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectMerchant: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onEditMerchant: (merchantId: Long, newDisplayName: String, newCategoryId: Long) -> Unit,
    onAddKeywordRuleForMerchant: (keyword: String, merchantName: String, categoryId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = uiState.selectedMerchantDetail
    if (detail != null) {
        MerchantDetailAnalyticsScreen(
            detail = detail,
            categories = uiState.categories,
            existingMerchants = uiState.merchants,
            isPrivacyMode = uiState.isPrivacyModeEnabled,
            onBack = onBackFromDetail,
            onEditMerchant = { newDisplayName, newCatId ->
                onEditMerchant(detail.summary.merchant.id, newDisplayName, newCatId)
            },
            onAddKeyword = { keyword ->
                val catId = detail.summary.category?.id
                    ?: uiState.categories.firstOrNull()?.id
                    ?: 1L
                onAddKeywordRuleForMerchant(keyword, detail.summary.merchant.displayName, catId)
            },
            modifier = modifier
        )
        return
    }

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.merchantSearchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Satıcı veya kural anahtar kelimesi ara...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = {
                if (uiState.merchantSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Temizle")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.merchantSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Henüz Tanımlı Satıcı Yok",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Onay Kuyruğu'ndaki harcamaları onayladığınızda satıcılar ve aydan aya harcama analizleri burada listelenir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val totalSpendAllMerchants = uiState.merchantSummaries.sumOf { it.netSpend }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.merchantSummaries.size} Satıcı",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Toplam: ${if (uiState.isPrivacyModeEnabled) "***" else Formatters.formatCurrency(totalSpendAllMerchants)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                items(
                    items = uiState.merchantSummaries,
                    key = { "merchant_${it.merchant.id}" }
                ) { item ->
                    MerchantSummaryRowCard(
                        item = item,
                        isPrivacyMode = uiState.isPrivacyModeEnabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectMerchant(item.merchant.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantSummaryRowCard(
    item: MerchantSummaryItem,
    isPrivacyMode: Boolean = false,
    onClick: () -> Unit
) {
    val catColor = item.category?.let { Color.fromHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .applePressScale(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Icon + Merchant Name & Category Badges on Left, Spend Amount on Right
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(item.category?.iconName ?: "Category"),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = item.merchant.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            item.category?.let { cat ->
                                BadgePill(text = cat.name, color = catColor)
                            }
                            BadgePill(
                                text = "${item.transactionCount} işlem",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPrivacyMode) "***" else Formatters.formatCurrency(item.netSpend),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (item.refundTotal > 0.0) {
                            Text(
                                text = if (isPrivacyMode) "***" else "-${Formatters.formatCurrency(item.refundTotal)} iade",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleGreenLight,
                                maxLines = 1
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bottom Full-Width Row for Rules (prevents squeezing the middle column)
            if (item.ruleKeywords.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kurallar: ${item.ruleKeywords.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantDetailAnalyticsScreen(
    detail: MerchantDetailData,
    categories: List<CategoryEntity>,
    existingMerchants: List<MerchantEntity>,
    isPrivacyMode: Boolean = false,
    onBack: () -> Unit,
    onEditMerchant: (newDisplayName: String, newCategoryId: Long) -> Unit,
    onAddKeyword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = detail.summary
    val catColor = summary.category?.let { Color.fromHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    var newKeywordText by remember { mutableStateOf("") }
    var showAddKeywordField by remember { mutableStateOf(false) }
    var showEditMerchantDialog by remember { mutableStateOf(false) }

    if (showEditMerchantDialog) {
        EditMerchantDialog(
            merchant = summary.merchant,
            initialCategoryId = summary.category?.id ?: summary.merchant.defaultCategoryId ?: categories.firstOrNull()?.id ?: 1L,
            categories = categories,
            existingMerchants = existingMerchants,
            onDismiss = { showEditMerchantDialog = false },
            onSave = { updatedName, updatedCategoryId ->
                onEditMerchant(updatedName, updatedCategoryId)
                showEditMerchantDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Back button + Merchant Header + Edit Merchant Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.merchant.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        summary.category?.let { cat ->
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = catColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.applePressScale {
                        showEditMerchantDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Satıcıyı Düzenle",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Düzenle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Total & Monthly Average Spend Overview Card
        item {
            val monthlyAvg = if (detail.monthlyTotals.isNotEmpty()) {
                summary.netSpend / detail.monthlyTotals.size
            } else {
                summary.netSpend
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Toplam Net Harcama",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPrivacyMode) "***" else Formatters.formatCurrency(summary.netSpend),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveCategoryIcon(summary.category?.iconName ?: "Category"),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatPill(
                            label = "Aylık Ortalama",
                            value = if (isPrivacyMode) "***" else Formatters.formatCurrency(monthlyAvg),
                            modifier = Modifier.weight(1.2f)
                        )
                        DetailStatPill(
                            label = "Toplam İşlem",
                            value = "${summary.transactionCount} adet",
                            modifier = Modifier.weight(0.9f)
                        )
                        DetailStatPill(
                            label = "Aktif Ay",
                            value = "${detail.monthlyTotals.size} ay",
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }

        // 3. Month-over-Month (Aydan Aya) Spend Chart & Table Card
        item {
            MerchantMonthlySpendChartCard(
                monthlyTotals = detail.monthlyTotals,
                barColor = catColor,
                isPrivacyMode = isPrivacyMode
            )
        }

        // 4. Associated Rule Keywords for this Merchant
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eşleştirme Kuralları (${detail.rules.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.applePressScale {
                                showAddKeywordField = !showAddKeywordField
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Yeni Kelime Ekle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detail.rules.forEach { rule ->
                            BadgePill(
                                text = "\"${rule.keyword}\" (Öncelik: ${rule.priority})",
                                color = catColor
                            )
                        }
                    }

                    if (showAddKeywordField) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newKeywordText,
                                onValueChange = { newKeywordText = it },
                                label = { Text("Ek Anahtar Kelime (Örn: SBUX)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newKeywordText.isNotBlank()) {
                                        onAddKeyword(newKeywordText.trim())
                                        newKeywordText = ""
                                        showAddKeywordField = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Ekle")
                            }
                        }
                    }
                }
            }
        }

        // 5. Full Transaction History of the Selected Merchant
        item {
            Text(
                text = "İşlem Geçmişi (${detail.transactions.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            items = detail.transactions,
            key = { "m_tx_${it.transaction.id}" }
        ) { txDetail ->
            TransactionListItemCard(
                detail = txDetail,
                isPrivacyMode = isPrivacyMode,
                onClickMerchant = {},
                onClickUnapproved = {}
            )
        }
    }
}

@Composable
private fun DetailStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditMerchantDialog(
    merchant: MerchantEntity,
    initialCategoryId: Long,
    categories: List<CategoryEntity>,
    existingMerchants: List<MerchantEntity>,
    onDismiss: () -> Unit,
    onSave: (newDisplayName: String, newCategoryId: Long) -> Unit
) {
    var merchantName by remember(merchant.id) { mutableStateOf(merchant.displayName) }
    var selectedCategoryId by remember(merchant.id) { mutableLongStateOf(initialCategoryId) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Satıcıyı Düzenle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Satıcı adını veya kategorisini değiştirdiğinizde bağlı kurallar ve işlemler de güncellenir.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Satıcı Görünen Adı") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val otherMerchants = remember(existingMerchants, merchant.id) {
                    existingMerchants.filter { it.id != merchant.id }
                }
                if (otherMerchants.isNotEmpty()) {
                    Text(
                        text = "Veya Mevcut Bir Satıcıyla Birleştir:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        otherMerchants.take(15).forEach { m ->
                            val isSelected = merchantName.equals(m.displayName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.applePressScale {
                                    merchantName = m.displayName
                                    m.defaultCategoryId?.let { selectedCategoryId = it }
                                }
                            ) {
                                Text(
                                    text = m.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Kategori:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat.id == selectedCategoryId
                        val c = Color.fromHex(cat.colorHex)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) c.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .then(
                                    if (isSel) Modifier.border(1.5.dp, c, RoundedCornerShape(10.dp)) else Modifier
                                )
                                .applePressScale { selectedCategoryId = cat.id }
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }
                    Button(
                        onClick = {
                            if (merchantName.isNotBlank()) {
                                onSave(merchantName.trim(), selectedCategoryId)
                            }
                        },
                        enabled = merchantName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantMonthlySpendChartCard(
    monthlyTotals: List<MerchantMonthlyTotal>,
    barColor: Color,
    isPrivacyMode: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = barColor
                )
                Text(
                    text = "Aydan Aya Toplam Harcama Trendi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (monthlyTotals.isEmpty()) {
                Text(
                    text = "Bu satıcı için henüz aylık harcama kaydı bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxSpend = (monthlyTotals.maxOfOrNull { it.netSpend } ?: 1.0).coerceAtLeast(1.0)

                // Horizontally scrollable Bar Chart Row so labels never overlap
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyTotals.forEach { monthItem ->
                        val ratio = (monthItem.netSpend / maxSpend).toFloat().coerceIn(0.08f, 1f)
                        val animatedRatio by animateFloatAsState(
                            targetValue = ratio,
                            animationSpec = AppleFluidMotion.momentumSpring(),
                            label = "barHeight"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .widthIn(min = 68.dp)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = if (isPrivacyMode) "***" else Formatters.formatCurrency(monthItem.netSpend).replace(" TL", ""),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .fillMaxHeight(animatedRatio * 0.72f)
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                    .background(barColor)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = Formatters.formatYearMonthShort(monthItem.yearMonth),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Month-by-month detailed breakdown rows
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    monthlyTotals.sortedByDescending { it.yearMonth }.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = Formatters.formatYearMonthDisplay(m.yearMonth),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append("${m.transactionCount} işlem")
                                        if (m.refundTotal > 0.0) {
                                            if (isPrivacyMode) {
                                                append(" • *** iade düşüldü")
                                            } else {
                                                append(" • ${Formatters.formatCurrency(m.refundTotal)} iade düşüldü")
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isPrivacyMode) "***" else Formatters.formatCurrency(m.netSpend),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
