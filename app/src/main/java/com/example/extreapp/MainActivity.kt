package com.example.extreapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.extreapp.ui.theme.ExtreAppTheme
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
            onDismiss = { viewModel.dismissImportReport() },
            onOpenApprovalQueue = { viewModel.selectTab(AppTab.APPROVAL_QUEUE) }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (uiState.activeTab) {
                                AppTab.DASHBOARD -> "ExtreApp • Aylık Analiz"
                                AppTab.APPROVAL_QUEUE -> "Onay Kuyruğu"
                                AppTab.MERCHANTS -> "Satıcı Analitiği"
                                AppTab.RULES_AND_BACKUP -> "Kurallar ve Yedekleme"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Quick load sample HESAP_OZETI.csv button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { viewModel.importBundledHesapOzeti(context) },
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "Örnek Ekstre",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Örnek Ekstre",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = {
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
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.UploadFile,
                            contentDescription = "Ekstre Dosyası Yükle"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                    icon = {
                        Icon(imageVector = Icons.Outlined.DonutLarge, contentDescription = null)
                    },
                    label = { Text("Aylık Analiz") }
                )

                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.APPROVAL_QUEUE,
                    onClick = { viewModel.selectTab(AppTab.APPROVAL_QUEUE) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.unapprovedTotalCount > 0) {
                                    Badge {
                                        Text("${uiState.unapprovedTotalCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.PendingActions, contentDescription = null)
                        }
                    },
                    label = { Text("Onay Kuyruğu") }
                )

                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.MERCHANTS,
                    onClick = { viewModel.selectTab(AppTab.MERCHANTS) },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Storefront, contentDescription = null)
                    },
                    label = { Text("Satıcılar") }
                )

                NavigationBarItem(
                    selected = uiState.activeTab == AppTab.RULES_AND_BACKUP,
                    onClick = { viewModel.selectTab(AppTab.RULES_AND_BACKUP) },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Rule, contentDescription = null)
                    },
                    label = { Text("Kurallar & Yedek") }
                )
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
                                    statementPickerLauncher.launch(arrayOf("*/*"))
                                },
                                onImportSampleHesapOzeti = {
                                    viewModel.importBundledHesapOzeti(context)
                                },
                                onSelectPeriod = { viewModel.selectPeriod(it) },
                                onSelectPeriodMode = { viewModel.setPeriodFilterMode(it) },
                                onToggleCategoryFilter = { viewModel.toggleCategoryFilter(it) },
                                onOpenApprovalQueue = { viewModel.selectTab(AppTab.APPROVAL_QUEUE) },
                                onOpenMerchantDetail = { viewModel.openMerchantDetail(it) }
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
