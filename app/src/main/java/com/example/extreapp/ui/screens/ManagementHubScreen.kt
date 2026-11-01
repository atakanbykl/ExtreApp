package com.example.extreapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extreapp.data.local.entity.ApprovalQueueGroup
import com.example.extreapp.data.local.entity.LockTimeout
import com.example.extreapp.domain.engine.RuleMatchCandidate
import com.example.extreapp.ui.ExtreUiState
import com.example.extreapp.ui.ManagementSection
import com.example.extreapp.ui.theme.AppleGreenLight
import com.example.extreapp.ui.theme.AppleOrangeLight
import com.example.extreapp.ui.theme.applePressScale

@Composable
fun ManagementHubScreen(
    uiState: ExtreUiState,
    onSelectSection: (ManagementSection?) -> Unit,
    // Statements actions
    onPickStatementFile: () -> Unit,
    onSearchStatementQueryChange: (String) -> Unit,
    onDeleteStatement: (String) -> Unit,
    onRenameStatement: (String, String) -> Unit,
    onReapplyRulesToStatement: (String) -> Unit,
    onJumpToPeriod: (String) -> Unit,
    onClearAllStatements: () -> Unit,
    // Approval Queue actions
    onApproveGroup: (group: ApprovalQueueGroup, keyword: String, merchantName: String, categoryId: Long) -> Unit,
    // Rules & Backup actions
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onReapplyRulesClick: () -> Unit,
    onCreateRule: (keyword: String, merchantName: String, categoryId: Long, priority: Int) -> Unit,
    onEditRule: (ruleId: Long, newKeyword: String, newMerchantName: String, newCategoryId: Long, newPriority: Int) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onUpdateRulePriority: (Long, Int) -> Unit,
    onApproveCandidateForRule: (anchorRuleId: Long, candidate: RuleMatchCandidate) -> Unit,
    onApproveAllCandidatesForRule: (anchorRuleId: Long, candidates: List<RuleMatchCandidate>) -> Unit,
    onAddCategory: (name: String, colorHex: Long) -> Unit,
    onClearAllData: () -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onSetBiometricLockEnabled: (Boolean) -> Unit,
    onSetScreenProtectionEnabled: (Boolean) -> Unit,
    onSetLockTimeout: (LockTimeout) -> Unit,
    onLockAppNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept back button when inside a management section
    BackHandler(enabled = uiState.managementSection != null) {
        onSelectSection(null)
    }

    AnimatedContent(
        targetState = uiState.managementSection,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "managementContentAnimation"
    ) { section ->
        if (section == null) {
            ManagementMainHub(
                uiState = uiState,
                onSelectSection = onSelectSection,
                modifier = modifier
            )
        } else {
            Column(modifier = modifier.fillMaxSize()) {
                // Section Top Bar with Back Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSelectSection(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Geri",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = when (section) {
                                ManagementSection.STATEMENTS -> "Ekstre Dosyaları Yönetimi"
                                ManagementSection.APPROVAL_QUEUE -> "Onay Bekleyen İşlemler"
                                ManagementSection.RULES -> "Kategori Kuralları"
                                ManagementSection.CATEGORIES -> "Kategori Tanımları"
                                ManagementSection.SECURITY -> "Güvenlik & Biyometri"
                                ManagementSection.BACKUP -> "Veri Yedekleme & Aktarım"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // Render specific management screen
                when (section) {
                    ManagementSection.STATEMENTS -> {
                        StatementsScreen(
                            uiState = uiState,
                            onPickStatementFile = onPickStatementFile,
                            onSearchQueryChange = onSearchStatementQueryChange,
                            onDeleteStatement = onDeleteStatement,
                            onRenameStatement = onRenameStatement,
                            onReapplyRules = onReapplyRulesToStatement,
                            onJumpToPeriod = onJumpToPeriod,
                            onClearAllStatements = onClearAllStatements,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ManagementSection.APPROVAL_QUEUE -> {
                        ApprovalQueueScreen(
                            uiState = uiState,
                            onApproveGroup = onApproveGroup,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ManagementSection.RULES -> {
                        RulesAndBackupScreen(
                            uiState = uiState,
                            initialSubTab = 0,
                            onExportBackupClick = onExportBackupClick,
                            onImportBackupClick = onImportBackupClick,
                            onReapplyRulesClick = onReapplyRulesClick,
                            onCreateRule = onCreateRule,
                            onEditRule = onEditRule,
                            onDeleteRule = onDeleteRule,
                            onUpdateRulePriority = onUpdateRulePriority,
                            onApproveCandidateForRule = onApproveCandidateForRule,
                            onApproveAllCandidatesForRule = onApproveAllCandidatesForRule,
                            onAddCategory = onAddCategory,
                            onClearAllData = onClearAllData,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onSetBiometricLockEnabled = onSetBiometricLockEnabled,
                            onSetScreenProtectionEnabled = onSetScreenProtectionEnabled,
                            onSetLockTimeout = onSetLockTimeout,
                            onLockAppNow = onLockAppNow,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ManagementSection.CATEGORIES -> {
                        RulesAndBackupScreen(
                            uiState = uiState,
                            initialSubTab = 1,
                            onExportBackupClick = onExportBackupClick,
                            onImportBackupClick = onImportBackupClick,
                            onReapplyRulesClick = onReapplyRulesClick,
                            onCreateRule = onCreateRule,
                            onEditRule = onEditRule,
                            onDeleteRule = onDeleteRule,
                            onUpdateRulePriority = onUpdateRulePriority,
                            onApproveCandidateForRule = onApproveCandidateForRule,
                            onApproveAllCandidatesForRule = onApproveAllCandidatesForRule,
                            onAddCategory = onAddCategory,
                            onClearAllData = onClearAllData,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onSetBiometricLockEnabled = onSetBiometricLockEnabled,
                            onSetScreenProtectionEnabled = onSetScreenProtectionEnabled,
                            onSetLockTimeout = onSetLockTimeout,
                            onLockAppNow = onLockAppNow,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ManagementSection.SECURITY -> {
                        RulesAndBackupScreen(
                            uiState = uiState,
                            initialSubTab = 3,
                            onExportBackupClick = onExportBackupClick,
                            onImportBackupClick = onImportBackupClick,
                            onReapplyRulesClick = onReapplyRulesClick,
                            onCreateRule = onCreateRule,
                            onEditRule = onEditRule,
                            onDeleteRule = onDeleteRule,
                            onUpdateRulePriority = onUpdateRulePriority,
                            onApproveCandidateForRule = onApproveCandidateForRule,
                            onApproveAllCandidatesForRule = onApproveAllCandidatesForRule,
                            onAddCategory = onAddCategory,
                            onClearAllData = onClearAllData,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onSetBiometricLockEnabled = onSetBiometricLockEnabled,
                            onSetScreenProtectionEnabled = onSetScreenProtectionEnabled,
                            onSetLockTimeout = onSetLockTimeout,
                            onLockAppNow = onLockAppNow,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ManagementSection.BACKUP -> {
                        RulesAndBackupScreen(
                            uiState = uiState,
                            initialSubTab = 2,
                            onExportBackupClick = onExportBackupClick,
                            onImportBackupClick = onImportBackupClick,
                            onReapplyRulesClick = onReapplyRulesClick,
                            onCreateRule = onCreateRule,
                            onEditRule = onEditRule,
                            onDeleteRule = onDeleteRule,
                            onUpdateRulePriority = onUpdateRulePriority,
                            onApproveCandidateForRule = onApproveCandidateForRule,
                            onApproveAllCandidatesForRule = onApproveAllCandidatesForRule,
                            onAddCategory = onAddCategory,
                            onClearAllData = onClearAllData,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onSetBiometricLockEnabled = onSetBiometricLockEnabled,
                            onSetScreenProtectionEnabled = onSetScreenProtectionEnabled,
                            onSetLockTimeout = onSetLockTimeout,
                            onLockAppNow = onLockAppNow,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementMainHub(
    uiState: ExtreUiState,
    onSelectSection: (ManagementSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group 1: Ekstreler & Tanımlanmamış İşlemler
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EKSTRE & İŞLEM YÖNETİMİ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Yüklü Ekstreler",
                            subtitle = "${uiState.loadedStatements.size} ekstre dosyası kayıtlı",
                            badgeText = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSection(ManagementSection.STATEMENTS)
                            }
                        )

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(start = 58.dp)
                        )

                        SettingsRowItem(
                            icon = Icons.Outlined.PendingActions,
                            iconTint = if (uiState.unapprovedTotalCount > 0) AppleOrangeLight else MaterialTheme.colorScheme.primary,
                            title = "Onay Kuyruğu",
                            subtitle = if (uiState.unapprovedTotalCount > 0) {
                                "${uiState.unapprovedTotalCount} işlem onay bekliyor"
                            } else {
                                "Tüm işlemler onaylanmış"
                            },
                            badgeText = if (uiState.unapprovedTotalCount > 0) {
                                if (uiState.unapprovedTotalCount > 99) "99+" else "${uiState.unapprovedTotalCount}"
                            } else null,
                            badgeColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSection(ManagementSection.APPROVAL_QUEUE)
                            }
                        )
                    }
                }
            }
        }

        // Group 2: Otomasyon & Kurallar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OTOMASYON & SINIFLANDIRMA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.AutoMirrored.Outlined.Rule,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Otomatik Kategori Kuralları",
                            subtitle = "${uiState.rules.size} aktif eşleşme kuralı",
                            badgeText = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSection(ManagementSection.RULES)
                            }
                        )

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(start = 58.dp)
                        )

                        SettingsRowItem(
                            icon = Icons.Outlined.Category,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Kategori Tanımları",
                            subtitle = "${uiState.categories.size} harcama kategorisi",
                            badgeText = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSection(ManagementSection.CATEGORIES)
                            }
                        )
                    }
                }
            }
        }

        // Group 3: Sistem, Güvenlik & Gizlilik
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GÜVENLİK & VERİ YÖNETİMİ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.Outlined.Lock,
                            iconTint = if (uiState.isBiometricLockEnabled) AppleGreenLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            title = "Güvenlik & Biyometrik Kilit",
                            subtitle = if (uiState.isBiometricLockEnabled) {
                                "Kilit Aktif (${uiState.lockTimeout.title})"
                            } else {
                                "Biyometrik kilit kapalı"
                            },
                            badgeText = if (uiState.isBiometricLockEnabled) "Açık" else null,
                            badgeColor = AppleGreenLight,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSection(ManagementSection.SECURITY)
                            }
                        )

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(start = 58.dp)
                        )

                        SettingsRowItem(
                            icon = Icons.Outlined.SaveAlt,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Veri Yedekleme & Aktarım",
                            subtitle = "JSON yedek al, geri yükle veya verileri temizle",
                            badgeText = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSection(ManagementSection.BACKUP)
                            }
                        )
                    }
                }
            }
        }

        // Informational Local Privacy Footer Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "ExtreApp tamamen %100 çevrimdışıdır. Ekstreleriniz ve harcama verileriniz asla sunuculara gönderilmez, sadece bu cihazda saklanır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.error,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .applePressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

