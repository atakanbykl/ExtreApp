package com.example.extreapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
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
import com.example.extreapp.data.local.entity.LoadedStatementSummary
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight
import com.example.extreapp.ui.theme.Formatters
import com.example.extreapp.ui.theme.applePressScale

@Composable
fun StatementsScreen(
    uiState: ExtreUiState,
    onPickStatementFile: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteStatement: (fileName: String) -> Unit,
    onRenameStatement: (oldFileName: String, newFileName: String) -> Unit,
    onReapplyRules: (fileName: String) -> Unit,
    onJumpToPeriod: (periodKey: String) -> Unit,
    onClearAllStatements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var statementToDelete by remember { mutableStateOf<LoadedStatementSummary?>(null) }
    var statementToRename by remember { mutableStateOf<LoadedStatementSummary?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    statementToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { statementToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Ekstreyi Sil?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "'${target.fileName}' ekstresi ve içerdiği ${target.transactionCount} adet işlem veritabanından kalıcı olarak silinecektir. Devam etmek istiyor musunuz?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteStatement(target.fileName)
                        statementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ekstreyi Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { statementToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Rename dialog
    statementToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { statementToRename = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Ekstre Adını Düzenle",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ekstre için akılda kalıcı bir isim veya etiket belirleyebilirsiniz:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Ekstre Adı") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRenameStatement(target.fileName, renameInput.trim())
                            statementToRename = null
                        }
                    },
                    enabled = renameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { statementToRename = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Tüm Ekstreleri Temizle?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Yüklü tüm ekstreler ve bunlara ait işlemler tamamen silinecektir. Bu işlem geri alınamaz.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClearAllStatements()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tümünü Temizle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Summary Hero Cards & Add Action
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Metric cards row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniCard(
                        title = "Ekstreler",
                        value = "${uiState.loadedStatements.size}",
                        subtitle = "Yüklü dosya",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Toplam İşlem",
                        value = "${uiState.totalTransactionCount}",
                        subtitle = "Kayıtlı hareket",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Toplam Harcama",
                        value = if (uiState.isPrivacyModeEnabled) "***" else {
                            val totalSpend = uiState.loadedStatements.sumOf { it.netExpenseTotal }
                            Formatters.formatCurrency(totalSpend)
                        },
                        subtitle = "Net harcama",
                        modifier = Modifier.weight(1.3f)
                    )
                }

                // Add Statement Button & Clear All Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPickStatementFile,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .applePressScale {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPickStatementFile()
                            },
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yeni Ekstre Yükle",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.loadedStatements.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearAllDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Tümünü Temizle",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Search Bar (if there are multiple statements)
                if (uiState.loadedStatements.isNotEmpty() || uiState.statementSearchQuery.isNotEmpty()) {
                    OutlinedTextField(
                        value = uiState.statementSearchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Ekstre adı, dönem veya kart ara...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.statementSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Statements List / Empty State
        if (uiState.loadedStatements.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (uiState.statementSearchQuery.isNotEmpty()) {
                                "Aramanızla Eşleşen Ekstre Bulunamadı"
                            } else {
                                "Henüz Yüklü Ekstre Yok"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (uiState.statementSearchQuery.isNotEmpty()) {
                                "Farklı bir arama terimi deneyin."
                            } else {
                                "Banka kredi kartı ekstrenizi (CSV, Excel veya PDF formatında) ekleyerek harcamalarınızı ve hesap kesim dönemlerinizi yönetmeye başlayın."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.statementSearchQuery.isEmpty()) {
                            Button(
                                onClick = onPickStatementFile,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ekstre Dosyası Seç")
                            }
                        }
                    }
                }
            }
        } else {
            items(uiState.loadedStatements, key = { it.fileName }) { statement ->
                StatementCard(
                    statement = statement,
                    isPrivacyMode = uiState.isPrivacyModeEnabled,
                    onRename = {
                        statementToRename = statement
                        renameInput = statement.fileName
                    },
                    onDelete = {
                        statementToDelete = statement
                    },
                    onReapplyRules = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReapplyRules(statement.fileName)
                    },
                    onJumpToPeriod = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (statement.primaryPeriod.isNotBlank()) {
                            onJumpToPeriod(statement.primaryPeriod)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatementCard(
    statement: LoadedStatementSummary,
    isPrivacyMode: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onReapplyRules: () -> Unit,
    onJumpToPeriod: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + File Name + Edit Button + Primary Period Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = statement.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = onRename,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Yeniden Adlandır",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (statement.primaryPeriod.isNotBlank()) {
                            Text(
                                text = "Dönem: ${Formatters.formatYearMonthDisplay(statement.primaryPeriod)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Ekstreyi Kaldır",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Info Badges: Transaction Count, Date Span, Card Info, Approval Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoTag(text = "${statement.transactionCount} İşlem")

                if (statement.minDateEpochDay > 0 && statement.maxDateEpochDay > 0) {
                    InfoTag(
                        icon = Icons.Outlined.DateRange,
                        text = "${Formatters.formatEpochDay(statement.minDateEpochDay)} - ${Formatters.formatEpochDay(statement.maxDateEpochDay)}"
                    )
                }

                statement.cardInfos.firstOrNull()?.let { card ->
                    InfoTag(
                        icon = Icons.Outlined.CreditCard,
                        text = card
                    )
                }
            }

            // Financial Summary Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Net Harcama",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "***" else Formatters.formatCurrency(statement.netExpenseTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (statement.paymentTotal > 0.0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Ödenen Borç",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isPrivacyMode) "***" else "-${Formatters.formatCurrency(statement.paymentTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleGreenLight
                        )
                    }
                }

                // Approval Status indicator
                if (statement.unapprovedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppleOrangeLight.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${statement.unapprovedCount} Onay Bekliyor",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleOrangeLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppleGreenLight.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Tümü Onaylı",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleGreenLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reapply Rules button if pending items exist
                if (statement.unapprovedCount > 0) {
                    TextButton(
                        onClick = onReapplyRules,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kuralları Uygula",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Jump to Monthly Overview for this statement
                if (statement.primaryPeriod.isNotBlank()) {
                    OutlinedButton(
                        onClick = onJumpToPeriod,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Dönem Analizine Git",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InfoTag(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

