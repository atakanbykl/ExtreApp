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
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleRedLight
import com.example.extreapp.ui.theme.applePressScale
import com.example.extreapp.ui.theme.fromHex
import com.example.extreapp.ui.theme.resolveCategoryIcon

private enum class RulesBackupSubTab(val title: String) {
    RULES("Kurallar"),
    CATEGORIES("Kategoriler"),
    BACKUP("Yedek & Veri")
}

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
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = RulesBackupSubTab.entries

    var showAddRuleForm by remember { mutableStateOf(false) }
    var newRuleKeyword by remember { mutableStateOf("") }
    var newRuleMerchant by remember { mutableStateOf("") }
    var newRulePriority by remember { mutableStateOf("10") }
    var newRuleCategoryId by remember(uiState.categories) {
        mutableLongStateOf(uiState.categories.firstOrNull()?.id ?: 1L)
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Sub-Tab Bar (Kurallar | Kategoriler | Yedek & Veri)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                subTabs.forEachIndexed { index, tab ->
                    val isSelected = selectedSubTab == index
                    val countSuffix = when (tab) {
                        RulesBackupSubTab.RULES -> " (${uiState.rules.size})"
                        RulesBackupSubTab.CATEGORIES -> " (${uiState.categories.size})"
                        RulesBackupSubTab.BACKUP -> ""
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .applePressScale { selectedSubTab = index }
                    ) {
                        Text(
                            text = "${tab.title}$countSuffix",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (subTabs[selectedSubTab]) {
            RulesBackupSubTab.RULES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action Toolbar Card for Rules
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Kural Tabanlı Eşleme Motoru",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "İçe aktarılan işlem açıklamaları buradaki anahtar kelimelerle (contains) karşılaştırılır. Öncelik puanı yüksek olan kural her zaman önce çalışır.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { showAddRuleForm = !showAddRuleForm },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (showAddRuleForm) "Formu Kapat" else "Yeni Kural Ekle")
                                    }

                                    OutlinedButton(
                                        onClick = onReapplyRulesClick,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Yeniden Uygula")
                                    }
                                }
                            }
                        }
                    }

                    // Expandable New Rule Form (Stacked vertically for clean readability)
                    if (showAddRuleForm) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Yeni Otomatik Eşleme Kuralı Oluştur",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedTextField(
                                        value = newRuleKeyword,
                                        onValueChange = { newRuleKeyword = it },
                                        label = { Text("Aranacak Anahtar Kelime (Örn: AMAZON.COM)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newRuleMerchant,
                                        onValueChange = { newRuleMerchant = it },
                                        label = { Text("Atanacak Satıcı Adı (Örn: Amazon)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newRulePriority,
                                        onValueChange = { newRulePriority = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Öncelik Puanı (Standart: 10, Özel Şube: 20)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = "Atanacak Kategori:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                modifier = Modifier
                                                    .then(
                                                        if (isSel) Modifier.border(1.5.dp, c, RoundedCornerShape(10.dp)) else Modifier
                                                    )
                                                    .applePressScale { newRuleCategoryId = cat.id }
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
                                        enabled = newRuleKeyword.isNotBlank() && newRuleMerchant.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Kuralı Kaydet ve İşlemlere Uygula")
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.rules.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Rule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Henüz Kayıtlı Kural Yok",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Onay Kuyruğu'nda onayladığınız satıcı ve kategori tercihleri veya yukarıdan eklediğiniz kurallar burada listelenir.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.rules,
                            key = { "rule_${it.id}" }
                        ) { rule ->
                            val merchantName = merchantsById[rule.merchantId]?.displayName ?: "Bilinmeyen Satıcı"
                            val category = categoriesById[rule.categoryId]
                            RuleItemCard(
                                rule = rule,
                                merchantName = merchantName,
                                category = category,
                                onIncreasePriority = { onUpdateRulePriority(rule.id, rule.priority + 5) },
                                onDecreasePriority = { onUpdateRulePriority(rule.id, (rule.priority - 5).coerceAtLeast(1)) },
                                onDelete = { onDeleteRule(rule.id) }
                            )
                        }
                    }
                }
            }

            RulesBackupSubTab.CATEGORIES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Category Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Yeni Kategori Ekle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    label = { Text("Kategori Adı (Örn: Eğitim & Kitap)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Kategori Rengi:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    colorOptions.forEach { hex ->
                                        val c = Color.fromHex(hex)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .then(
                                                    if (selectedColorHex == hex) {
                                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
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
                                        }
                                    },
                                    enabled = newCategoryName.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kategoriyi Kaydet")
                                }
                            }
                        }
                    }

                    // Existing Categories List
                    items(
                        items = uiState.categories,
                        key = { "cat_list_${it.id}" }
                    ) { cat ->
                        val catColor = Color.fromHex(cat.colorHex)
                        val ruleCountForCat = uiState.rules.count { it.categoryId == cat.id }

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = resolveCategoryIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (cat.isDefault) "Varsayılan Kategori" else "Özel Kategori",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                BadgePill(
                                    text = "$ruleCountForCat kural",
                                    color = catColor
                                )
                            }
                        }
                    }
                }
            }

            RulesBackupSubTab.BACKUP -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "%100 Çevrimdışı Yerel Veritabanı",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Verileriniz yalnızca cihaz belleğinde tutulur",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 3-Column Database Stats Boxes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatBox(
                                        label = "İşlemler",
                                        value = "${uiState.totalTransactionCount}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatBox(
                                        label = "Satıcılar",
                                        value = "${uiState.merchants.size}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatBox(
                                        label = "Kurallar",
                                        value = "${uiState.rules.size}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = "Tam veritabanı yedeğinizi (tüm işlemler, öğrendiğiniz kurallar, satıcılar ve kategoriler) tek bir JSON dosyası olarak dışa aktarabilir veya farklı bir cihaza geri yükleyebilirsiniz.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onExportBackupClick,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 13.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Backup,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tam Veritabanı Yedeğini Dışa Aktar (JSON)")
                                    }

                                    OutlinedButton(
                                        onClick = onImportBackupClick,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 13.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Yedek Dosyasından Geri Yükle (JSON)")
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.totalTransactionCount > 0 || uiState.rules.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Veritabanını Sıfırla",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleRedLight
                                    )
                                    Text(
                                        text = "Cihazdaki tüm ekstre işlemlerini, satıcıları ve kuralları temizleyerek uygulamayı ilk kurulum haline döndürür.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { showClearConfirmDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            tint = AppleRedLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tüm Yerel Verileri Temizle", color = AppleRedLight)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleItemCard(
    rule: RuleEntity,
    merchantName: String,
    category: CategoryEntity?,
    onIncreasePriority: () -> Unit,
    onDecreasePriority: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = category?.let { Color.fromHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val categoryName = category?.name ?: "Diğer"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top section: Keyword container
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "EŞLEŞEN ANAHTAR KELİME (CONTAINS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rule.keyword,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Middle section: Assigned Merchant & Category
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = merchantName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = categoryColor.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(category?.iconName ?: "Category"),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Bottom Action Bar: Priority Stepper on Left, Delete Button on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Öncelik:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.applePressScale(onClick = onDecreasePriority)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Önceliği Azalt",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                    Text(
                        text = "${rule.priority}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.applePressScale(onClick = onIncreasePriority)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Önceliği Artır",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppleRedLight.copy(alpha = 0.12f),
                    modifier = Modifier.applePressScale(onClick = onDelete)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Kuralı Sil",
                            tint = AppleRedLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Kuralı Sil",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppleRedLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
