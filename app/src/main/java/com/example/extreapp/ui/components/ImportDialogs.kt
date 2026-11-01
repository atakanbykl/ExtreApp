package com.example.extreapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.TableChart
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.extreapp.domain.parser.ColumnMapping
import com.example.extreapp.domain.parser.RawStatementTable
import com.example.extreapp.domain.parser.StatementImportReport
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight

@Composable
fun ImportReportDialog(
    report: StatementImportReport,
    isPrivacyMode: Boolean = false,
    onDismiss: () -> Unit,
    onOpenApprovalQueue: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Ekstre İçe Aktarıldı",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${report.fileName} • ${report.detectedCharset}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (report.metadata.cardInfo != null || report.metadata.statementDate != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            report.metadata.cardInfo?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                report.metadata.statementDate?.let {
                                    Text(
                                        text = "Kesim: $it",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                report.metadata.statementDebt?.let {
                                    Text(
                                        text = if (isPrivacyMode) "Ekstre Borcu: ***" else "Ekstre Borcu: $it",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                StatRowItem(
                    icon = Icons.Outlined.AutoAwesome,
                    tint = AppleGreenLight,
                    label = "Kurallarla Otomatik Eşleşen",
                    value = "${report.autoMatchedCount} işlem"
                )
                StatRowItem(
                    icon = Icons.Outlined.PendingActions,
                    tint = AppleOrangeLight,
                    label = "Onay Kuyruğuna Eklenen",
                    value = "${report.queuedForApprovalCount} işlem"
                )
                StatRowItem(
                    icon = Icons.Outlined.FilterNone,
                    tint = MaterialTheme.colorScheme.secondary,
                    label = "Atlanan Mükerrer Kayıt",
                    value = "${report.duplicateSkippedCount} kayıt"
                )
                if (report.paymentAndRewardCount > 0) {
                    StatRowItem(
                        icon = Icons.Outlined.TableChart,
                        tint = MaterialTheme.colorScheme.primary,
                        label = "Ödeme / Kampanya Bilgi Satırı",
                        value = "${report.paymentAndRewardCount} satır"
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Kapat")
                    }
                    if (report.queuedForApprovalCount > 0) {
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenApprovalQueue()
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Onay Kuyruğu (${report.queuedForApprovalCount})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRowItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ColumnMappingWizardDialog(
    table: RawStatementTable,
    onConfirm: (ColumnMapping) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = table.autoDetectedMapping ?: ColumnMapping(0, 1, 2)
    val columnCount = maxOf(
        table.headers.size,
        table.dataRows.maxOfOrNull { it.size } ?: 3
    ).coerceAtLeast(3)

    var dateCol by remember { mutableIntStateOf(initial.dateColumnIndex.coerceIn(0, columnCount - 1)) }
    var descCol by remember { mutableIntStateOf(initial.descriptionColumnIndex.coerceIn(0, columnCount - 1)) }
    var amountCol by remember { mutableIntStateOf(initial.amountColumnIndex.coerceIn(0, columnCount - 1)) }

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
                Text(
                    text = "Sütun Eşleştirme Sihirbazı",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${table.fileName} dosyasındaki sütunları eşleştirin:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val columnLabels = (0 until columnCount).map { idx ->
                    val headerName = table.headers.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: "Sütun ${idx + 1}"
                    val sampleVal = table.dataRows.firstOrNull()?.getOrNull(idx)?.take(18) ?: ""
                    if (sampleVal.isNotEmpty()) "$headerName ($sampleVal)" else headerName
                }

                ColumnSelectorSection(
                    title = "Tarih Sütunu",
                    columnLabels = columnLabels,
                    selectedIndex = dateCol,
                    onSelect = { dateCol = it }
                )

                ColumnSelectorSection(
                    title = "İşlem Açıklaması Sütunu",
                    columnLabels = columnLabels,
                    selectedIndex = descCol,
                    onSelect = { descCol = it }
                )

                ColumnSelectorSection(
                    title = "Tutar Sütunu",
                    columnLabels = columnLabels,
                    selectedIndex = amountCol,
                    onSelect = { amountCol = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                ColumnMapping(
                                    dateColumnIndex = dateCol,
                                    descriptionColumnIndex = descCol,
                                    amountColumnIndex = amountCol
                                )
                            )
                        }
                    ) {
                        Text("İçe Aktar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnSelectorSection(
    title: String,
    columnLabels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            columnLabels.forEachIndexed { idx, label ->
                FilterChip(
                    selected = (selectedIndex == idx),
                    onClick = { onSelect(idx) },
                    label = { Text(label, maxLines = 1) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

