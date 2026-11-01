package com.example.extreapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleRedLight
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesAndBackupScreen(
    uiState: ExtreUiState,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onReapplyRulesClick: () -> Unit,
    onCreateRule: (keyword: String, merchantName: String, categoryId: Long, priority: Int) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onUpdateRulePriority: (Long, Int) -> Unit,
    onAddCategory: (name: String, colorHex: Long) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddRuleForm by remember { mutableStateOf(false) }
    var newRuleKeyword by remember { mutableStateOf("") }
    var newRuleMerchant by remember { mutableStateOf("") }
    var newRulePriority by remember { mutableStateOf("10") }
    var newRuleCategoryId by remember(uiState.categories) {
        mutableLongStateOf(uiState.categories.firstOrNull()?.id ?: 1L)
    }

    var showAddCategoryForm by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    val colorOptions = listOf(
        0xFF10B981L, 0xFF3B82F6L, 0xFFF59E0BL, 0xFF8B5CF6L,
        0xFFEC4899L, 0xFF06B6D4L, 0xFFEF4444L, 0xFF6366F1L
    )
    var selectedColorHex by remember { mutableLongStateOf(colorOptions.first()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val merchantsById = remember(uiState.merchants) { uiState.merchants.associateBy { it.id } }
    val categoriesById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Veritabanını Sıfırla") },
            text = {
                Text("Cihaz belleğindeki tüm işlemler, satıcılar ve tanımladığınız kurallar kalıcı olarak silinecek. Devam etmek istiyor musunuz?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRedLight)
                ) {
                    Text("Tümünü Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Offline Privacy & Local Database Backup Card
        item {
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppleGreenLight.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = AppleGreenLight
                            )
                        }
                        Column {
                            Text(
                                text = "%100 Çevrimdışı Yerel Veritabanı",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.totalTransactionCount} işlem • ${uiState.merchants.size} satıcı • ${uiState.rules.size} kural",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Tüm veriler yalnızca cihaz belleğinde saklanır ve harici hiçbir sunucuya gönderilmez. Tam veritabanı yedeğinizi (İşlemler + Kurallar + Satıcılar + Kategoriler) JSON olarak dışa veya içe aktarabilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportBackupClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yedekle (JSON)")
                        }
                        OutlinedButton(
                            onClick = onImportBackupClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Geri Yükle")
                        }
                    }

                    if (uiState.totalTransactionCount > 0 || uiState.rules.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = AppleRedLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yerel Veritabanını Sıfırla", color = AppleRedLight)
                        }
                    }
                }
            }
        }

        // 2. Custom Categories Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kategoriler (${uiState.categories.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { showAddCategoryForm = !showAddCategoryForm },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yeni Kategori", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.categories.forEach { cat ->
                            BadgePill(
                                text = cat.name,
                                color = Color.fromHex(cat.colorHex)
                            )
                        }
                    }

                    if (showAddCategoryForm) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Kategori Adı (Örn: Eğitim & Kitap)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorOptions.forEach { hex ->
                                val c = Color.fromHex(hex)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .then(
                                            if (selectedColorHex == hex) {
                                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .applePressScale { selectedColorHex = hex }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    onAddCategory(newCategoryName.trim(), selectedColorHex)
                                    newCategoryName = ""
                                    showAddCategoryForm = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Kategoriyi Ekle")
                        }
                    }
                }
            }
        }

        // 3. Rule Matching Engine Header + Manual Rule Creation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Eşleme Kuralları (${uiState.rules.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Önceliği yüksek olan kural önce değerlendirilir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onReapplyRulesClick) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Kuralları Yeniden Uygula"
                        )
                    }
                    Button(
                        onClick = { showAddRuleForm = !showAddRuleForm },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kural Ekle", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (showAddRuleForm) {
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
                        Text(
                            text = "Yeni Otomatik Eşleme Kuralı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newRuleKeyword,
                                onValueChange = { newRuleKeyword = it },
                                label = { Text("Anahtar Kelime (Contains)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newRuleMerchant,
                                onValueChange = { newRuleMerchant = it },
                                label = { Text("Satıcı Adı") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = newRulePriority,
                            onValueChange = { newRulePriority = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Öncelik (Varsayılan: 10, Spesifik Kural: 20)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.categories.forEach { cat ->
                                val isSel = cat.id == newRuleCategoryId
                                val c = Color.fromHex(cat.colorHex)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) c.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.applePressScale { newRuleCategoryId = cat.id }
                                ) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (newRuleKeyword.isNotBlank() && newRuleMerchant.isNotBlank()) {
                                    val prio = newRulePriority.toIntOrNull() ?: 10
                                    onCreateRule(newRuleKeyword.trim(), newRuleMerchant.trim(), newRuleCategoryId, prio)
                                    newRuleKeyword = ""
                                    newRuleMerchant = ""
                                    showAddRuleForm = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kuralı Kaydet ve Geçmiş İşlemlere Uygula")
                        }
                    }
                }
            }
        }

        items(
            items = uiState.rules,
            key = { "rule_${it.id}" }
        ) { rule ->
            val merchantName = merchantsById[rule.merchantId]?.displayName ?: "Bilinmeyen Satıcı"
            val category = categoriesById[rule.categoryId]
            RuleItemRowCard(
                rule = rule,
                merchantName = merchantName,
                categoryName = category?.name ?: "Diğer",
                categoryColor = category?.let { Color.fromHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
                onIncreasePriority = { onUpdateRulePriority(rule.id, rule.priority + 5) },
                onDecreasePriority = { onUpdateRulePriority(rule.id, (rule.priority - 5).coerceAtLeast(1)) },
                onDelete = { onDeleteRule(rule.id) }
            )
        }
    }
}

@Composable
private fun RuleItemRowCard(
    rule: RuleEntity,
    merchantName: String,
    categoryName: String,
    categoryColor: Color,
    onIncreasePriority: () -> Unit,
    onDecreasePriority: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "\"${rule.keyword}\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BadgePill(
                        text = "Öncelik ${rule.priority}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "→ $merchantName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    BadgePill(
                        text = categoryName,
                        color = categoryColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onIncreasePriority) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Önceliği Artır",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDecreasePriority) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Önceliği Azalt",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Kuralı Sil",
                        tint = AppleRedLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

