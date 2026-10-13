package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContractTask
import com.example.model.DailyMission
import com.example.model.StudySyncContract
import com.example.ui.components.ExecutionLogDialog
import com.example.ui.components.NotionCard
import com.example.ui.components.QuickStrategyAdviceBanner
import com.example.ui.components.TaskTypeBadge
import com.example.ui.theme.AcademicAmber
import com.example.ui.theme.AcademicAmberSubtle
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicGreenSubtle
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle
import com.example.ui.theme.AcademicRose
import com.example.ui.theme.AcademicRoseSubtle

@Composable
fun MissionScreen(
    contract: StudySyncContract,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    onToggleTask: (dayNumber: Int, taskId: String) -> Unit,
    onRecordExecution: (dayNumber: Int, taskId: String, actualMinutes: Int, errorRate: Int, difficulty: String, notes: String) -> Unit,
    onTriggerRecalibrate: () -> Unit,
    onGenerateDayMission: ((Int) -> Unit)? = null,
    onGenerateNextDaysBatch: ((Int) -> Unit)? = null,
    isGeneratingDay: Boolean = false,
    generatingDayNumber: Int? = null,
    progressiveGenProgress: String? = null,
    modifier: Modifier = Modifier
) {
    val totalDays = contract.dailySchedule.size
    val currentMission = contract.dailySchedule.firstOrNull { it.day == selectedDay }
        ?: contract.dailySchedule.firstOrNull()
        ?: return

    var activeTaskForLogging by remember { mutableStateOf<ContractTask?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (progressiveGenProgress != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AcademicPurpleSubtle.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, AcademicPurple.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(40.dp).height(4.dp),
                        color = AcademicPurple
                    )
                    Text(
                        text = progressiveGenProgress,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AcademicPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Day Navigation Carousel
        DaySelectorCarousel(
            dailySchedule = contract.dailySchedule,
            selectedDay = selectedDay,
            onSelectDay = onSelectDay
        )

        // Notion Card 1: Daily Focus
        DailyFocusCard(
            mission = currentMission,
            selectedDay = selectedDay,
            totalDays = totalDays,
            onPrevDay = { if (selectedDay > 1) onSelectDay(selectedDay - 1) },
            onNextDay = { if (selectedDay < totalDays) onSelectDay(selectedDay + 1) }
        )

        // Notion Card: AI Strategic Guidance & Daily Recommended Ratio
        contract.bookAnalysis?.let { report ->
            QuickStrategyAdviceBanner(report = report)
        }

        // Notion Card 2: Task Breakdown
        TaskBreakdownCard(
            mission = currentMission,
            onToggleTask = { taskId -> onToggleTask(currentMission.day, taskId) },
            onOpenLogDialog = { task -> activeTaskForLogging = task },
            onGenerateThisDay = { onGenerateDayMission?.invoke(currentMission.day) },
            onGenerateNextBatch = { onGenerateNextDaysBatch?.invoke(3) },
            isGeneratingThisDay = isGeneratingDay && generatingDayNumber == currentMission.day,
            isGeneratingAnyDay = isGeneratingDay
        )

        // Notion Card 3: Adaptive Adjustments & Dynamic Recalibration Feedback
        AdaptiveAdjustmentsCard(
            adjustments = contract.adaptiveAdjustments,
            onRecalibrate = onTriggerRecalibrate
        )
    }

    // Execution Log Dialog
    if (activeTaskForLogging != null) {
        ExecutionLogDialog(
            dayNumber = currentMission.day,
            task = activeTaskForLogging!!,
            onDismiss = { activeTaskForLogging = null },
            onSaveExecution = { actualMinutes, errorRate, difficulty, notes ->
                onRecordExecution(
                    currentMission.day,
                    activeTaskForLogging!!.id.ifEmpty { "${currentMission.day}-${activeTaskForLogging!!.material}" },
                    actualMinutes,
                    errorRate,
                    difficulty,
                    notes
                )
                activeTaskForLogging = null
            }
        )
    }
}

@Composable
private fun DaySelectorCarousel(
    dailySchedule: List<DailyMission>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "复习大纲时间线",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "第 $selectedDay 天 / 共 ${dailySchedule.size} 天",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AcademicBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(dailySchedule) { mission ->
                    val isSelected = mission.day == selectedDay
                    val allCompleted = mission.tasks.isNotEmpty() && mission.tasks.all { it.completed }
                    val someCompleted = mission.tasks.any { it.completed }
                    val isMilestone = mission.coreFocus.contains("MILESTONE", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isSelected -> AcademicBlue
                            allCompleted -> AcademicGreenSubtle
                            isMilestone -> AcademicAmberSubtle
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AcademicBlue else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .testTag("day_chip_${mission.day}")
                            .clickable { onSelectDay(mission.day) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "D${mission.day}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSelected -> Color.White
                                    allCompleted -> AcademicGreen
                                    isMilestone -> AcademicAmber
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Color.White
                                            allCompleted -> AcademicGreen
                                            someCompleted -> AcademicAmber
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyFocusCard(
    mission: DailyMission,
    selectedDay: Int,
    totalDays: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val totalEstimated = mission.allocatedMinutes
    val isMilestone = mission.coreFocus.contains("MILESTONE", ignoreCase = true)
    val isBuffer = mission.coreFocus.contains("Buffer", ignoreCase = true)

    NotionCard(
        title = "今日核心焦点",
        calloutIcon = if (isMilestone) "🎯" else if (isBuffer) "🛡️" else "⚡",
        badgeText = "${mission.date} • 第 $selectedDay 天"
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mission.coreFocus,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isMilestone -> AcademicAmber
                        isBuffer -> AcademicGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(
                        onClick = onPrevDay,
                        enabled = selectedDay > 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "前一天")
                    }
                    IconButton(
                        onClick = onNextDay,
                        enabled = selectedDay < totalDays,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "后一天")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stage-based Book Clearance Milestone Banner
            if (mission.phaseLabel.isNotBlank() || mission.phaseBookGoal.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AcademicBlueSubtle.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🗓️", fontSize = 12.sp)
                            Text(
                                text = mission.phaseLabel.ifBlank { "本阶段攻坚" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AcademicBlue
                            ) {
                                Text(
                                    text = "阶段书目结项要求",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (mission.phaseBookGoal.isNotBlank()) {
                            Text(
                                text = mission.phaseBookGoal,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Focus Metrics Callout
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日任务预算: $totalEstimated 分钟 (~${String.format("%.1f", totalEstimated / 60f)} 小时)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "已完成 ${mission.tasks.count { it.completed }}/${mission.tasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (mission.tasks.all { it.completed }) AcademicGreen else AcademicBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskBreakdownCard(
    mission: DailyMission,
    onToggleTask: (String) -> Unit,
    onOpenLogDialog: (ContractTask) -> Unit,
    onGenerateThisDay: (() -> Unit)? = null,
    onGenerateNextBatch: (() -> Unit)? = null,
    isGeneratingThisDay: Boolean = false,
    isGeneratingAnyDay: Boolean = false
) {
    val isTasksEmpty = mission.tasks.isEmpty()

    NotionCard(
        title = "任务细化分解",
        calloutIcon = if (isTasksEmpty) "✨" else "📋",
        badgeText = if (isTasksEmpty) "待 AI 深度推演" else "共 ${mission.tasks.size} 项任务"
    ) {
        if (isTasksEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AcademicBlueSubtle.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AcademicBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "宏观统筹蓝图已锁定 · 待 AI 逐日定制",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue
                            )
                        }
                        Text(
                            text = "当前核心攻坚重点：${mission.coreFocus}\n阶段目标：${mission.phaseLabel.ifBlank { "备考攻坚" }}（${mission.phaseBookGoal.ifBlank { "多学科推进" }}）。为杜绝一次性生成的误差与本地死板模板，StudySync 采用「全局统筹 + AI 逐日演化」架构。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                if (isGeneratingThisDay) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = AcademicBlue
                            )
                            Text(
                                text = "AI 正在结合前序进度推演第 ${mission.day} 天精准任务...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AcademicBlue
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onGenerateThisDay?.invoke() },
                        enabled = !isGeneratingAnyDay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcademicBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_generate_day_mission")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✨ 立即召唤 AI 推演生成第 ${mission.day} 天每日任务",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onGenerateNextBatch?.invoke() },
                        enabled = !isGeneratingAnyDay,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ 连续推演生成未来 3 天任务",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                mission.tasks.forEachIndexed { index, task ->
                    val taskId = task.id.ifEmpty { "${mission.day}-${task.material}" }
                    TaskItemRow(
                        task = task,
                        onToggle = { onToggleTask(taskId) },
                        onOpenLog = { onOpenLogDialog(task) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onGenerateThisDay?.invoke() },
                        enabled = !isGeneratingAnyDay,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isGeneratingThisDay) "AI 正在重新推演..." else "🔄 召唤 AI 重新微调本日任务",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemRow(
    task: ContractTask,
    onToggle: () -> Unit,
    onOpenLog: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (task.completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (task.completed) AcademicGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AcademicGreen,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier
                    .size(24.dp)
                    .testTag("checkbox_task_${task.id}")
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskTypeBadge(taskType = task.taskType)

                    Text(
                        text = "计划 ${task.estimatedMinutes}分钟",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.material,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = task.scope,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )

                if (task.completed && task.actualMinutes > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "实耗: ${task.actualMinutes}分钟",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (task.errorRatePercent > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (task.errorRatePercent > 25) AcademicRoseSubtle else AcademicGreenSubtle
                            ) {
                                Text(
                                    text = "错题率: ${task.errorRatePercent}%",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (task.errorRatePercent > 25) AcademicRose else AcademicGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = task.difficultyFeedback,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenLog,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_log_telemetry_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "记录执行遥测数据",
                    tint = AcademicBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AdaptiveAdjustmentsCard(
    adjustments: com.example.model.AdaptiveAdjustments,
    onRecalibrate: () -> Unit
) {
    NotionCard(
        title = "自适应动态重校与导师洞察",
        calloutIcon = "🧠",
        badgeText = "自适应重校引擎"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Feedback Summary Callout
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AcademicBlueSubtle.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AcademicBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = adjustments.feedbackSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }

            // Pace Adjustment Strategy
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AcademicGreenSubtle.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, AcademicGreen.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AcademicGreen,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "配速策略",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcademicGreen
                        )
                        Text(
                            text = adjustments.paceAdjustment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Upcoming Changes List
            if (adjustments.upcomingChanges.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "后续负荷与缓冲动态重调",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    adjustments.upcomingChanges.forEach { change ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", color = AcademicBlue, fontWeight = FontWeight.Bold)
                            Text(
                                text = change,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onRecalibrate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, AcademicBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AcademicBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("刷新 AI 自适应重校", color = AcademicBlue, fontSize = 12.sp)
            }
        }
    }
}
