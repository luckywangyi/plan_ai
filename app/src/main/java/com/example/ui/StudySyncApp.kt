package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiConfigDialog
import com.example.ui.components.ImportContractDialog
import com.example.ui.components.IngestPlanDialog
import com.example.ui.components.StudySyncHeader
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.ContractJsonScreen
import com.example.ui.screens.HabitMatrixScreen
import com.example.ui.screens.MissionScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicPurple

enum class NavDestination(val label: String, val icon: ImageVector, val tag: String) {
    MISSION("每日任务", Icons.AutoMirrored.Filled.Assignment, "nav_mission"),
    CURRICULUM("大纲总览", Icons.AutoMirrored.Filled.EventNote, "nav_curriculum"),
    PEER_SYNC("学友同步", Icons.AutoMirrored.Filled.CompareArrows, "nav_peer_sync"),
    A4_MATRIX("A4打卡表", Icons.Default.TableChart, "nav_a4_matrix"),
    CONTRACT("契约数据", Icons.Default.Code, "nav_contract")
}

@Composable
fun StudySyncApp(
    viewModel: StudySyncViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val activeContract by viewModel.activeContract.collectAsState()
    val contractA by viewModel.contractA.collectAsState()
    val contractB by viewModel.contractB.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isGeneratingDay by viewModel.isGeneratingDay.collectAsState()
    val generatingDayNumber by viewModel.generatingDayNumber.collectAsState()
    val progressiveGenProgress by viewModel.progressiveGenProgress.collectAsState()
    val isAnalyzingBooks by viewModel.isAnalyzingBooks.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val savedRawInputsA by viewModel.savedRawInputsA.collectAsState()
    val savedRawInputsB by viewModel.savedRawInputsB.collectAsState()
    val currentRawInputs = if (activeProfileId == "Profile_A") savedRawInputsA else savedRawInputsB

    var currentNav by remember { mutableStateOf(NavDestination.MISSION) }
    var showIngestDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            val hasAiWarning = msg.contains("配额") || msg.contains("429") || msg.contains("异常") || msg.contains("配置")
            val actionLabel = if (hasAiWarning) "⚙️ 配置模型" else null
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = actionLabel,
                duration = if (hasAiWarning) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                showAiConfigDialog = true
            }
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavDestination.values().forEach { dest ->
                    val selected = currentNav == dest
                    val activeColor = if (activeProfileId == "Profile_A") AcademicBlue else AcademicPurple

                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentNav = dest },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            selectedTextColor = activeColor,
                            indicatorColor = activeColor.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(dest.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Persistent Header & Switcher across all screens
                StudySyncHeader(
                    activeProfileId = activeProfileId,
                    contract = activeContract,
                    aiConfig = aiConfig,
                    onOpenAiConfigDialog = { showAiConfigDialog = true },
                    onSwitchProfile = { viewModel.switchProfile(it) },
                    onOpenIngestDialog = { showIngestDialog = true },
                    onOpenImportDialog = { showImportDialog = true },
                    onRecalibrate = { viewModel.triggerAiRecalibration() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Content Screen
                when (currentNav) {
                    NavDestination.MISSION -> {
                        if (activeContract != null) {
                            MissionScreen(
                                contract = activeContract!!,
                                selectedDay = selectedDay,
                                onSelectDay = { viewModel.selectDay(it) },
                                onToggleTask = { day, taskId -> viewModel.toggleTaskCompletion(day, taskId) },
                                onRecordExecution = { day, taskId, actualMin, errRate, diff, notes ->
                                    viewModel.recordExecution(day, taskId, actualMin, errRate, diff, notes)
                                },
                                onTriggerRecalibrate = { viewModel.triggerAiRecalibration() },
                                onGenerateDayMission = { viewModel.generateDayMission(it) },
                                onGenerateNextDaysBatch = { viewModel.generateNextDaysBatch(it) },
                                isGeneratingDay = isGeneratingDay,
                                generatingDayNumber = generatingDayNumber,
                                progressiveGenProgress = progressiveGenProgress
                            )
                        } else {
                            EmptyContractPlaceholder(
                                onIngest = { showIngestDialog = true },
                                onImportJson = { showImportDialog = true },
                                onApplyPreset = { viewModel.applyPreset(it) }
                            )
                        }
                    }

                    NavDestination.CURRICULUM -> {
                        if (activeContract != null) {
                            ScheduleScreen(
                                contract = activeContract!!,
                                onSelectDay = { day ->
                                    viewModel.selectDay(day)
                                    currentNav = NavDestination.MISSION
                                },
                                onMarkBookCompleted = { bookTitle ->
                                    viewModel.markBookCompleted(bookTitle)
                                },
                                onAddPhaseBooks = {
                                    showIngestDialog = true
                                },
                                onGenerateNextDaysBatch = { viewModel.generateNextDaysBatch(it) },
                                isGeneratingDay = isGeneratingDay,
                                progressiveGenProgress = progressiveGenProgress
                            )
                        } else {
                            EmptyContractPlaceholder(
                                onIngest = { showIngestDialog = true },
                                onImportJson = { showImportDialog = true },
                                onApplyPreset = { viewModel.applyPreset(it) }
                            )
                        }
                    }

                    NavDestination.PEER_SYNC -> {
                        ComparisonScreen(
                            contractA = contractA,
                            contractB = contractB
                        )
                    }

                    NavDestination.A4_MATRIX -> {
                        if (activeContract != null) {
                            HabitMatrixScreen(
                                contract = activeContract!!,
                                onToggleTask = { day, taskId -> viewModel.toggleTaskCompletion(day, taskId) }
                            )
                        } else {
                            EmptyContractPlaceholder(
                                onIngest = { showIngestDialog = true },
                                onImportJson = { showImportDialog = true },
                                onApplyPreset = { viewModel.applyPreset(it) }
                            )
                        }
                    }

                    NavDestination.CONTRACT -> {
                        if (activeContract != null) {
                            ContractJsonScreen(
                                contract = activeContract!!,
                                rawInputs = currentRawInputs,
                                onImportContract = { viewModel.importContract(it) }
                            )
                        } else {
                            EmptyContractPlaceholder(
                                onIngest = { showIngestDialog = true },
                                onImportJson = { showImportDialog = true },
                                onApplyPreset = { viewModel.applyPreset(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Loading / Ingestion Progress Overlay
            AnimatedVisibility(
                visible = isGenerating || isAnalyzingBooks,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = AcademicBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isAnalyzingBooks) "AI 正在全面研读教材与测算刷题时间..." else "正在深度解构复习资料与生成每日日程...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${aiConfig.activeDisplayName}：科学解构章节难度、题量并给出每日任务与战略建议",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Ingest Plan Dialog
    if (showIngestDialog) {
        val savedRawInputs = viewModel.getSavedRawInputs(activeProfileId)
        IngestPlanDialog(
            activeProfileId = activeProfileId,
            initialRawInputs = savedRawInputs,
            aiConfig = aiConfig,
            onOpenAiConfig = {
                showIngestDialog = false
                showAiConfigDialog = true
            },
            onDismiss = { showIngestDialog = false },
            onSaveDraft = { rawInputs ->
                viewModel.saveRawInputs(activeProfileId, rawInputs)
            },
            onResetToPresets = {
                viewModel.clearSavedRawInputs(activeProfileId)
            },
            onIngestPlan = { rawInputs ->
                viewModel.ingestPlan(rawInputs)
            },
            onAnalyzeBooks = { rawInputs ->
                viewModel.analyzeBooks(rawInputs)
            }
        )
    }

    // AI Configuration Dialog
    if (showAiConfigDialog) {
        AiConfigDialog(
            currentConfig = aiConfig,
            onSaveConfig = { viewModel.updateAiConfig(it) },
            onTestConnection = { viewModel.testAiConnection(it) },
            onDismiss = { showAiConfigDialog = false }
        )
    }

    // Import External Contract JSON Dialog
    if (showImportDialog) {
        ImportContractDialog(
            targetProfileId = activeProfileId,
            rawInputs = currentRawInputs,
            existingContract = activeContract,
            onImport = { contract ->
                viewModel.importContract(contract)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
private fun EmptyContractPlaceholder(
    onIngest: () -> Unit,
    onImportJson: () -> Unit,
    onApplyPreset: (com.example.model.ExamPreset) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "暂无激活的备考规划",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "支持从外部 AI（ChatGPT / Claude / DeepSeek 网页版等）直接导入规划 JSON，或录入书目大纲进行排期。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Button(
                    onClick = onImportJson,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AcademicGreen),
                    modifier = Modifier.testTag("btn_empty_import_json")
                ) {
                    Text("📥 导入外部规划 JSON")
                }

                androidx.compose.material3.OutlinedButton(
                    onClick = onIngest,
                    modifier = Modifier.testTag("btn_empty_ingest")
                ) {
                    Text("录入大纲资料")
                }
            }
        }
    }
}
