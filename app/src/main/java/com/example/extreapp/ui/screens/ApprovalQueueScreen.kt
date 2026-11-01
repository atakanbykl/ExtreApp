package com.example.extreapp.ui.screens

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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.extreapp.data.local.entity.ApprovalQueueGroup
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.TransactionType
import com.example.extreapp.domain.engine.TurkishTextNormalizer
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon

@Composable
fun ApprovalQueueScreen(
    uiState: ExtreUiState,
    onApproveGroup: (
        group: ApprovalQueueGroup,
        keyword: String,
        merchantName: String,
        categoryId: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = uiState.approvalQueueGroups

    if (groups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AppleGreenLight.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            tint = AppleGreenLight,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        text = "Onay Kuyruğu Tamamlandı!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tüm harcama ve iade kayıtları satıcı ve kategorileriyle eşleştirildi. Yeni bir ekstre yüklediğinizde tanımsız işlemler burada listelenir.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "${uiState.unapprovedTotalCount} İşlem (${groups.size} Farklı Satıcı Grubu)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Belirlediğiniz satıcı adı ve kategori sisteme kural olarak kaydedilir; aynı satıcıya ait diğer işlemler otomatik onaylanır.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(
            items = groups,
            key = { "queue_${it.suggestedKeyword}_${it.sampleRawDescription}" }
        ) { group ->
            ApprovalQueueGroupCard(
                group = group,
                isPrivacyMode = uiState.isPrivacyModeEnabled,
                categories = uiState.categories,
                existingMerchants = uiState.merchants,
                onApprove = { keyword, merchantName, categoryId ->
                    onApproveGroup(group, keyword, merchantName, categoryId)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApprovalQueueGroupCard(
    group: ApprovalQueueGroup,
    isPrivacyMode: Boolean = false,
    categories: List<CategoryEntity>,
    existingMerchants: List<MerchantEntity>,
    onApprove: (keyword: String, merchantName: String, categoryId: Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val initialCategoryId = remember(group.suggestedKeyword, categories) {
        inferSuggestedCategoryId(group.sampleRawDescription, categories)
    }

    var merchantName by remember(group.suggestedMerchantName) {
        mutableStateOf(group.suggestedMerchantName)
    }
    var keyword by remember(group.suggestedKeyword) {
        mutableStateOf(group.suggestedKeyword)
    }
    var selectedCategoryId by remember(initialCategoryId) {
        mutableLongStateOf(initialCategoryId)
    }
    var showAllTransactions by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgePill(
                            text = "${group.transactions.size} İşlem",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Formatters.formatEpochDay(group.transactions.first().dateEpochDay),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = group.sampleRawDescription,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isPrivacyMode) "***" else Formatters.formatCurrency(group.totalAmount, showSign = group.totalAmount < 0),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (group.totalAmount < 0) AppleGreenLight else MaterialTheme.colorScheme.onSurface
                )
            }

            if (group.transactions.size > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .applePressScale {
                            showAllTransactions = !showAllTransactions
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showAllTransactions) {
                                    "Bu gruptaki ${group.transactions.size} işlemi gizle"
                                } else {
                                    "Bu kuralın etkileyeceği ${group.transactions.size} işlemi gör"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showAllTransactions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (showAllTransactions) {
                            Spacer(modifier = Modifier.height(8.dp))
                            group.transactions.forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${Formatters.formatEpochDay(tx.dateEpochDay)} • ${tx.rawDescription}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val sign = if (tx.type == TransactionType.REFUND) "-" else ""
                                    Text(
                                        text = if (isPrivacyMode) "***" else "$sign${Formatters.formatCurrency(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (tx.type == TransactionType.REFUND) AppleGreenLight else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (existingMerchants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Mevcut Satıcıya Bağla (İsteğe Bağlı):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingMerchants.forEach { existing ->
                            val isSelectedMerchant = merchantName.equals(existing.displayName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelectedMerchant) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier.applePressScale {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    merchantName = existing.displayName
                                    existing.defaultCategoryId?.let { selectedCategoryId = it }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = existing.displayName,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Atanacak Satıcı Adı") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Eşleştirme Anahtar Kelimesi (Contains)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Kategori Seçin",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    val catColor = Color.fromHex(cat.colorHex)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, catColor, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .applePressScale {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategoryId = cat.id
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = resolveCategoryIcon(cat.iconName),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApprove(keyword, merchantName, selectedCategoryId)
                },
                enabled = merchantName.isNotBlank() && keyword.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (group.transactions.size > 1) {
                        "Kuralı Kaydet ve ${group.transactions.size} İşlemi Onayla"
                    } else {
                        "Kuralı Kaydet ve Onayla"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun inferSuggestedCategoryId(
    rawDescription: String,
    categories: List<CategoryEntity>
): Long {
    val norm = TurkishTextNormalizer.normalize(rawDescription)
    fun findCatId(namePart: String): Long? =
        categories.firstOrNull { TurkishTextNormalizer.normalize(it.name).contains(namePart) }?.id

    return when {
        norm.contains("market") || norm.contains("file ") || norm.contains("metro") || norm.contains("getir") || norm.contains("gida") ->
            findCatId("market")
        norm.contains("restoran") || norm.contains("kafe") || norm.contains("cafe") || norm.contains("starbucks") ||
            norm.contains("sbux") || norm.contains("burger") || norm.contains("yemeksepet") || norm.contains("lokanta") ||
            norm.contains("makarna") || norm.contains("kofteci") || norm.contains("aspava") || norm.contains("mezzaluna") ||
            norm.contains("street food") || norm.contains("bagette") || norm.contains("kitchen") ->
            findCatId("restoran")
        norm.contains("petrol") || norm.contains("opet") || norm.contains("poas") || norm.contains("po-") ||
            norm.contains("bridgestone") || norm.contains("lassa") || norm.contains("eryaman p") ->
            findCatId("akaryakit")
        norm.contains("amazon") || norm.contains("hepsipay") || norm.contains("hepsibu") || norm.contains("teknosa") || norm.contains("vans") ->
            findCatId("e-ticaret")
        norm.contains("ajet") || norm.contains("taksi") || norm.contains("turizm") || norm.contains("turz") ->
            findCatId("ulasim")
        norm.contains("youtube") || norm.contains("spotify") || norm.contains("google one") || norm.contains("steamgames") ->
            findCatId("dijital")
        norm.contains("ikea") ->
            findCatId("ev")
        norm.contains("gymfit") || norm.contains("sportif") ->
            findCatId("spor")
        norm.contains("bar") || norm.contains("house of b") ->
            findCatId("eglence")
        norm.contains("belediyesi") ->
            findCatId("fatura")
        else -> null
    } ?: categories.firstOrNull()?.id ?: 1L
}
