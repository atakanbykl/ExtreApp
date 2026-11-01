package com.example.extreapp

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Tune
import com.example.extreapp.domain.security.BiometricAuthManager
import com.example.extreapp.ui.AppTab
import com.example.extreapp.ui.PlanningTabMode
import com.example.extreapp.ui.InsightsTabMode
import com.example.extreapp.ui.ManagementSection
import com.example.extreapp.ui.ExtreViewModel
import com.example.extreapp.ui.components.AppleAppLockScreen
import com.example.extreapp.ui.components.ColumnMappingWizardDialog
import com.example.extreapp.ui.components.ImportReportDialog
import com.example.extreapp.ui.screens.OverviewScreen
import com.example.extreapp.ui.screens.PlanningScreen
import com.example.extreapp.ui.screens.InsightsScreen
import com.example.extreapp.ui.screens.ManagementHubScreen
import com.example.extreapp.ui.theme.ExtreAppTheme
import com.example.extreapp.ui.theme.applePressScale
import java.time.LocalDate

class MainActivity : FragmentActivity() {
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
    val activity = context as? FragmentActivity

    // Check device biometric capability on launch
    LaunchedEffect(Unit) {
        val availability = BiometricAuthManager.checkAvailability(context)
        viewModel.updateBiometricAvailability(availability)
    }

    // Screen Protection (C.2 - FLAG_SECURE)
    LaunchedEffect(uiState.isScreenProtectionEnabled) {
        activity?.let { act ->
            if (uiState.isScreenProtectionEnabled) {
                act.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Background / Foreground lifecycle observer for auto-lock timeout
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            } else if (event == Lifecycle.Event.ON_START) {
                viewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            onOpenApprovalQueue = { viewModel.openApprovalQueue() }
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
                                        imageVector = when (uiState.activeTab) {
                                            AppTab.OVERVIEW -> Icons.Outlined.DonutLarge
                                            AppTab.PLANNING -> Icons.Outlined.CalendarMonth
                                            AppTab.INSIGHTS -> if (uiState.selectedMerchantId != null) Icons.Outlined.Storefront else Icons.AutoMirrored.Outlined.TrendingUp
                                            AppTab.MANAGEMENT -> Icons.Outlined.Tune
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
                                    text = when (uiState.activeTab) {
                                        AppTab.OVERVIEW -> "Genel Bakış"
                                        AppTab.PLANNING -> if (uiState.planningTabMode == PlanningTabMode.INSTALLMENTS) "Taksit Takvimi" else "Tekrarlanan Ödemeler"
                                        AppTab.INSIGHTS -> if (uiState.selectedMerchantId != null) "Satıcı Detayı" else if (uiState.insightsTabMode == InsightsTabMode.COMPARISON) "Aydan Aya Kıyasla" else "Satıcı Analitiği"
                                        AppTab.MANAGEMENT -> if (uiState.managementSection != null) "Yönetim" else "Yönetim Merkezi"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Lock Button (visible when Biometric Lock is ON)
                        if (uiState.isBiometricLockEnabled) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(34.dp)
                                    .applePressScale {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.lockAppNow()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = "Hemen Kilitle",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

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
                    // Tab 1: Genel Bakış
                    val isOverview = uiState.activeTab == AppTab.OVERVIEW
                    NavigationBarItem(
                        selected = isOverview,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.OVERVIEW)
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
                                text = "Genel Bakış",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isOverview) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 2: Gelecek & Plan
                    val isPlanning = uiState.activeTab == AppTab.PLANNING
                    NavigationBarItem(
                        selected = isPlanning,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.PLANNING)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Gelecek & Plan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isPlanning) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 3: Trend & Satıcı
                    val isInsights = uiState.activeTab == AppTab.INSIGHTS
                    NavigationBarItem(
                        selected = isInsights,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.INSIGHTS)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Trend & Satıcı",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isInsights) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        colors = navItemColors
                    )

                    // Tab 4: Yönetim
                    val isManagement = uiState.activeTab == AppTab.MANAGEMENT
                    NavigationBarItem(
                        selected = isManagement,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(AppTab.MANAGEMENT)
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
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Yönetim",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isManagement) FontWeight.Bold else FontWeight.Medium,
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
                        AppTab.OVERVIEW -> {
                            OverviewScreen(
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
                                onOpenApprovalQueue = { viewModel.openApprovalQueue() },
                                onOpenMerchantDetail = { viewModel.openMerchantDetail(it) }
                            )
                        }
                        AppTab.PLANNING -> {
                            PlanningScreen(
                                uiState = uiState,
                                onSelectPlanningTabMode = { viewModel.setPlanningTabMode(it) },
                                onSelectForecastMonth = { viewModel.selectForecastMonth(it) },
                                onSelectRecurringTabFilter = { viewModel.selectRecurringTabFilter(it) },
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
                                }
                            )
                        }
                        AppTab.INSIGHTS -> {
                            InsightsScreen(
                                uiState = uiState,
                                onSelectInsightsTabMode = { viewModel.setInsightsTabMode(it) },
                                onSelectComparisonTarget = { viewModel.setComparisonTargetPeriod(it) },
                                onSwapComparisonPeriods = { viewModel.swapComparisonPeriods() },
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
                                onSearchMerchantQueryChange = { viewModel.updateMerchantSearch(it) },
                                onSelectMerchant = { viewModel.openMerchantDetail(it) },
                                onBackFromMerchantDetail = { viewModel.closeMerchantDetail() },
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
                        AppTab.MANAGEMENT -> {
                            ManagementHubScreen(
                                uiState = uiState,
                                onSelectSection = { viewModel.setManagementSection(it) },
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
                                onSearchStatementQueryChange = { viewModel.updateStatementSearch(it) },
                                onDeleteStatement = { viewModel.deleteLoadedStatement(it) },
                                onRenameStatement = { oldName, newName -> viewModel.renameLoadedStatement(oldName, newName) },
                                onReapplyRulesToStatement = { viewModel.reapplyRulesToStatement(it) },
                                onJumpToPeriod = { viewModel.jumpToPeriod(it) },
                                onClearAllStatements = { viewModel.clearAllStatements() },
                                onApproveGroup = { group, keyword, merchantName, categoryId ->
                                    viewModel.approveQueueItemAndSaveRule(
                                        group = group,
                                        keyword = keyword,
                                        merchantDisplayName = merchantName,
                                        categoryId = categoryId
                                    )
                                },
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
                                onClearAllData = { viewModel.clearAllDatabase() },
                                onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
                                onSetBiometricLockEnabled = { enabled ->
                                    if (enabled) {
                                        activity?.let { act ->
                                            BiometricAuthManager.authenticate(
                                                activity = act,
                                                title = "Biyometrik Kilidi Etkinleştir",
                                                subtitle = "Uygulama kilidini aktif etmek için kimliğinizi doğrulayın",
                                                onSuccess = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.setBiometricLockEnabled(true)
                                                }
                                            )
                                        } ?: viewModel.setBiometricLockEnabled(true)
                                    } else {
                                        viewModel.setBiometricLockEnabled(false)
                                    }
                                },
                                onSetScreenProtectionEnabled = { viewModel.setScreenProtectionEnabled(it) },
                                onSetLockTimeout = { viewModel.setLockTimeout(it) },
                                onLockAppNow = { viewModel.lockAppNow() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Apple-style Lock Overlay
    if (uiState.isAppLocked) {
        AppleAppLockScreen(
            onUnlockClick = {
                activity?.let { act ->
                    BiometricAuthManager.authenticate(
                        activity = act,
                        title = "ExtreApp Kimlik Doğrulama",
                        subtitle = "Uygulamaya erişmek için kimliğinizi doğrulayın",
                        onSuccess = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setAppLocked(false)
                        }
                    )
                }
            }
        )
    }
}
