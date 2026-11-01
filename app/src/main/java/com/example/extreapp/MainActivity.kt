package com.example.extreapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.extreapp.ui.AppTab
import com.example.extreapp.ui.ExtreViewModel
import com.example.extreapp.ui.components.ColumnMappingWizardDialog
import com.example.extreapp.ui.components.ImportReportDialog
import com.example.extreapp.ui.screens.ApprovalQueueScreen
import com.example.extreapp.ui.screens.DashboardScreen
import com.example.extreapp.ui.screens.MerchantsScreen
import com.example.extreapp.ui.screens.RulesAndBackupScreen
import com.example.extreapp.ui.screens.StatementsScreen
import com.example.extreapp.ui.theme.ExtreAppTheme
import com.example.extreapp.ui.theme.applePressScale
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExtreAppTheme {
                ExtreAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtreAppRoot() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: ExtreViewModel = viewModel(factory = ExtreViewModel.provideFactory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Back handler when viewing a specific Merchant's detail analytics
    BackHandler(enabled = uiState.selectedMerchantId != null) {
        viewModel.closeMerchantDetail()
    }

    // Statement File Picker (CSV, Excel, PDF)
    val statementPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importStatementFromUri(context, uri)
        }
    }

    // Backup Export Launcher (JSON)
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabaseBackup(context, uri)
        }
    }

    // Backup Restore Launcher (JSON)
    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreDatabaseBackup(context, uri)
        }
    }

    // Dialogs
    uiState.pendingWizardTable?.let { rawTable ->
        ColumnMappingWizardDialog(
            table = rawTable,
            onConfirm = { mapping -> viewModel.confirmWizardColumnMapping(mapping) },
            onDismiss = { viewModel.dismissWizard() }
        )
    }

    uiState.lastImportReport?.let { report ->
        ImportReportDialog(
            report = report,
            isPrivacyMode = uiState.isPrivacyModeEnabled,
            onDismiss = { viewModel.dismissImportReport() },
            onOpenApprovalQueue = { viewModel.selectTab(AppTab.APPROVAL_QUEUE) }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when {
                                            uiState.activeTab == AppTab.MERCHANTS && uiState.selectedMerchantId != null -> Icons.Outlined.Storefront
                                            uiState.activeTab == AppTab.DASHBOARD -> Icons.Outlined.DonutLarge
                                            uiState.activeTab == AppTab.STATEMENTS -> Icons.AutoMirrored.Outlined.ReceiptLong
                                            uiState.activeTab == AppTab.APPROVAL_QUEUE -> Icons.Outlined.PendingActions
                                            uiState.activeTab == AppTab.MERCHANTS -> Icons.Outlined.Storefront
                                            else -> Icons.AutoMirrored.Outlined.Rule
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "EXTREAPP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = when {
                                        uiState.activeTab == AppTab.MERCHANTS && uiState.selectedMerchantId != null -> "Satıcı Detayı"
                                        uiState.activeTab == AppTab.DASHBOARD -> "Aylık Analiz"
                                        uiState.activeTab == AppTab.STATEMENTS -> "Ekstre Yönetimi"
                                        uiState.activeTab == AppTab.APPROVAL_QUEUE -> "Onay Kuyruğu"
                                        uiState.activeTab == AppTab.MERCHANTS -> "Satıcı Analitiği"
                                        else -> "Kurallar & Yedek"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        // Privacy mode toggle (Hide/Show all currency amounts as ***)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.isPrivacyModeEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (uiState.isPrivacyModeEnabled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .applePressScale {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.togglePrivacyMode()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPrivacyModeEnabled) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (uiState.isPrivacyModeEnabled) {
                                        "Gizlilik Modu Açık (Miktarlar Gizli)"
                                    } else {
                                        "Gizlilik Modu Kapalı (Miktarları Göster)"
                                    },
                                    modifier = Modifier.size(16.dp),
                                    tint = if (uiState.isPrivacyModeEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = if (uiState.isPrivacyModeEnabled) "Gizli" else "Açık",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.isPrivacyModeEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        },
        bottomBar = {
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    // Tab 1: Analiz
                    val isDashboard = uiState.activeTab == AppTab.DASHBOARD
                    NavigationBarItem(
                        selected = isDashboard,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.DASHBOARD)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.DonutLarge,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Analiz",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isDashboard) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 2: Ekstreler
                    val isStatements = uiState.activeTab == AppTab.STATEMENTS
                    NavigationBarItem(
                        selected = isStatements,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.STATEMENTS)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Ekstreler",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isStatements) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 3: Onaylar
                    val isApproval = uiState.activeTab == AppTab.APPROVAL_QUEUE
                    NavigationBarItem(
                        selected = isApproval,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.APPROVAL_QUEUE)
                        },
                        icon = {
                            if (uiState.unapprovedTotalCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(
                                                text = if (uiState.unapprovedTotalCount > 99) "99+" else "${uiState.unapprovedTotalCount}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PendingActions,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.PendingActions,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Onaylar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isApproval) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 4: Satıcılar
                    val isMerchants = uiState.activeTab == AppTab.MERCHANTS
                    NavigationBarItem(
                        selected = isMerchants,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.MERCHANTS)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Satıcılar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isMerchants) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 5: Kurallar
                    val isRules = uiState.activeTab == AppTab.RULES_AND_BACKUP
                    NavigationBarItem(
                        selected = isRules,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.RULES_AND_BACKUP)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Rule,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Kurallar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isRules) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            uiState.bannerMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissBannerMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = uiState.activeTab, label = "tabCrossfade") { tab ->
                    when (tab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                uiState = uiState,
                                onPickStatementFile = {
                                    statementPickerLauncher.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "text/plain",
                                            "application/csv",
                                            "application/vnd.ms-excel",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/pdf",
                                            "*/*"
                                        )
                                    )
                                },
                                onSelectPeriod = { viewModel.selectPeriod(it) },
                                onSelectPeriodMode = { viewModel.setPeriodFilterMode(it) },
                                onToggleCategoryFilter = { viewModel.toggleCategoryFilter(it) },
                                onToggleSortByDate = { viewModel.toggleSortByDate() },
                                onToggleSortByAmount = { viewModel.toggleSortByAmount() },
                                onOpenApprovalQueue = { viewModel.selectTab(AppTab.APPROVAL_QUEUE) },
                                onOpenMerchantDetail = { viewModel.openMerchantDetail(it) }
                            )
                        }
                        AppTab.STATEMENTS -> {
                            StatementsScreen(
                                uiState = uiState,
                                onPickStatementFile = {
                                    statementPickerLauncher.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "text/plain",
                                            "application/csv",
                                            "application/vnd.ms-excel",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/pdf",
                                            "*/*"
                                        )
                                    )
                                },
                                onSearchQueryChange = { viewModel.updateStatementSearch(it) },
                                onDeleteStatement = { viewModel.deleteLoadedStatement(it) },
                                onRenameStatement = { oldName, newName -> viewModel.renameLoadedStatement(oldName, newName) },
                                onReapplyRules = { viewModel.reapplyRulesToStatement(it) },
                                onJumpToPeriod = { viewModel.jumpToPeriod(it) },
                                onClearAllStatements = { viewModel.clearAllStatements() }
                            )
                        }
                        AppTab.APPROVAL_QUEUE -> {
                            ApprovalQueueScreen(
                                uiState = uiState,
                                onApproveGroup = { group, keyword, merchantName, categoryId ->
                                    viewModel.approveQueueItemAndSaveRule(
                                        group = group,
                                        keyword = keyword,
                                        merchantDisplayName = merchantName,
                                        categoryId = categoryId
                                    )
                                }
                            )
                        }
                        AppTab.MERCHANTS -> {
                            MerchantsScreen(
                                uiState = uiState,
                                onSearchQueryChange = { viewModel.updateMerchantSearch(it) },
                                onSelectMerchant = { viewModel.openMerchantDetail(it) },
                                onBackFromDetail = { viewModel.closeMerchantDetail() },
                                onEditMerchant = { merchantId, newDisplayName, newCategoryId ->
                                    viewModel.editExistingMerchant(
                                        merchantId = merchantId,
                                        newDisplayName = newDisplayName,
                                        newCategoryId = newCategoryId
                                    )
                                },
                                onAddKeywordRuleForMerchant = { keyword, merchantName, categoryId ->
                                    viewModel.createOrUpdateRule(
                                        keyword = keyword,
                                        merchantDisplayName = merchantName,
                                        categoryId = categoryId,
                                        priority = 10,
                                        applyRetroactively = true
                                    )
                                }
                            )
                        }
                        AppTab.RULES_AND_BACKUP -> {
                            RulesAndBackupScreen(
                                uiState = uiState,
                                onExportBackupClick = {
                                    val dateStr = LocalDate.now().toString()
                                    backupExportLauncher.launch("extreapp_yedek_$dateStr.json")
                                },
                                onImportBackupClick = {
                                    backupRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onReapplyRulesClick = { viewModel.reapplyAllRules() },
                                onCreateRule = { keyword, merchantName, categoryId, priority ->
                                    viewModel.createOrUpdateRule(
                                        keyword = keyword,
                                        merchantDisplayName = merchantName,
                                        categoryId = categoryId,
                                        priority = priority,
                                        applyRetroactively = true
                                    )
                                },
                                onEditRule = { ruleId, newKeyword, newMerchantName, newCatId, newPrio ->
                                    viewModel.editExistingRule(
                                        ruleId = ruleId,
                                        newKeyword = newKeyword,
                                        newMerchantDisplayName = newMerchantName,
                                        newCategoryId = newCatId,
                                        newPriority = newPrio
                                    )
                                },
                                onDeleteRule = { viewModel.deleteRule(it) },
                                onUpdateRulePriority = { id, newPrio -> viewModel.updateRulePriority(id, newPrio) },
                                onApproveCandidateForRule = { anchorRuleId, candidate ->
                                    viewModel.approveCandidateForRule(anchorRuleId, candidate)
                                },
                                onApproveAllCandidatesForRule = { anchorRuleId, candidates ->
                                    viewModel.approveAllCandidatesForRule(anchorRuleId, candidates)
                                },
                                onAddCategory = { name, hex ->
                                    viewModel.addCustomCategory(name, "Category", hex)
                                },
                                onClearAllData = { viewModel.clearAllDatabase() }
                            )
                        }
                    }
                }
            }
        }
    }
}
