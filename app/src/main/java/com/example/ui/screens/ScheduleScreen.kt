package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyMission
import com.example.model.MilestoneItem
import com.example.model.PhaseBookRequirement
import com.example.model.PhaseMilestone
import com.example.model.StudySyncContract
import com.example.ui.components.BookAnalysisCard
import com.example.ui.components.MilestoneStepTrack
import com.example.ui.components.NotionCard
import com.example.ui.theme.AcademicAmber
import com.example.ui.theme.AcademicAmberSubtle
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicGreenSubtle
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle

enum class PlanViewMode(val title: String, val icon: String) {
    MONTHLY("按月书目结项", "📅"),
    WEEKLY("按周攻坚清单", "🗓️"),
    DAILY("每日全长大纲", "📜")
}

@Composable
fun ScheduleScreen(
    contract: StudySyncContract,
    onSelectDay: (Int) -> Unit,
    onMarkBookCompleted: ((String) -> Unit)? = null,
    onAddPhaseBooks: (() -> Unit)? = null,
    onGenerateNextDaysBatch: ((Int) -> Unit)? = null,
    isGeneratingDay: Boolean = false,
    progressiveGenProgress: String? = null,
    modifier: Modifier = Modifier
) {
    var activeViewMode by remember { mutableStateOf(PlanViewMode.MONTHLY) }

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
                    androidx.compose.material3.LinearProgressIndicator(
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

        // Book Lifecycle, Sprint & Archived Completed Books Card
        BookLifecycleSummaryCard(
            contract = contract,
            onAddPhaseBooks = onAddPhaseBooks
        )

        // AI Deep Book Analysis, Question Counts & Workload Report
        contract.bookAnalysis?.let { report ->
            BookAnalysisCard(
                report = report,
                onMarkBookCompleted = onMarkBookCompleted
            )
        }

        // View Scope Mode Segment Selector (Monthly vs Weekly vs Daily)
        NotionCard(
            title = "备考规划维度视窗",
            calloutIcon = "🧭",
            badgeText = "多粒度动态编排"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "告别盲目平铺！考研需科学分解为阶段性书目结项目标，拒绝从现在到12月的一次性任务堆砌。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanViewMode.values().forEach { mode ->
                        val isSelected = activeViewMode == mode
                        val badgeCount = when (mode) {
                            PlanViewMode.MONTHLY -> "${contract.monthlyPhases.size}个月"
                            PlanViewMode.WEEKLY -> "${contract.weeklyPhases.size}周"
                            PlanViewMode.DAILY -> "${contract.dailySchedule.size}天"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AcademicBlue else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AcademicBlue else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeViewMode = mode }
                                .testTag("view_mode_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${mode.icon} ${mode.title}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = badgeCount,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        when (activeViewMode) {
            PlanViewMode.MONTHLY -> {
                // Monthly Phases List
                NotionCard(
                    title = "按月书目结项总规划 (Monthly Clearances)",
                    calloutIcon = "📅",
                    badgeText = "共 ${contract.monthlyPhases.size} 个备考月度"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (contract.monthlyPhases.isEmpty()) {
                            Text(
                                text = "正在生成月度书目规划...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            contract.monthlyPhases.forEach { phase ->
                                PhaseMilestoneCard(
                                    phase = phase,
                                    onJumpToDay = onSelectDay
                                )
                            }
                        }
                    }
                }
            }
            PlanViewMode.WEEKLY -> {
                // Weekly Phases List
                NotionCard(
                    title = "按周攻坚与书目章节验收 (Weekly Cycles)",
                    calloutIcon = "🗓️",
                    badgeText = "共 ${contract.weeklyPhases.size} 个周期周"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (contract.weeklyPhases.isEmpty()) {
                            Text(
                                text = "正在生成周度攻坚清单...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            contract.weeklyPhases.forEach { phase ->
                                PhaseMilestoneCard(
                                    phase = phase,
                                    onJumpToDay = onSelectDay
                                )
                            }
                        }
                    }
                }
            }
            PlanViewMode.DAILY -> {
                // Milestone Checkpoints Card (25%, 50%, 75%, 100%)
                NotionCard(
                    title = "阶段里程碑诊断评估",
                    calloutIcon = "🏆",
                    badgeText = "25% • 50% • 75% • 100%"
                ) {
                    MilestoneStepTrack(milestones = contract.milestones)
                }

                // Spaced Repetition Protocol Notion Card
                NotionCard(
                    title = "内置艾宾浩斯间隔复习方案",
                    calloutIcon = "🔄",
                    badgeText = "艾宾浩斯记忆矩阵"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "科学穿插交织于每日任务中，确保在考前硬性截止日 ${contract.planOverview.targetDate} 前牢固掌握考点。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SpacedBadge(label = "D+1", sub = "核心回忆", modifier = Modifier.weight(1f))
                            SpacedBadge(label = "D+3", sub = "错题补漏", modifier = Modifier.weight(1f))
                            SpacedBadge(label = "D+7", sub = "滚动演练", modifier = Modifier.weight(1f))
                            SpacedBadge(label = "D+14", sub = "综合全真", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Full Days Timeline
                val populatedCount = contract.dailySchedule.count { it.tasks.isNotEmpty() }
                val unpopulatedCount = contract.dailySchedule.count { it.tasks.isEmpty() }

                if (unpopulatedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AcademicBlueSubtle.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI 逐日推演进度：已就绪 $populatedCount 天 / 待细化 $unpopulatedCount 天",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcademicBlue
                                )
                                Text(
                                    text = "采用统筹规划+按需返回机制，可点击任意天进入精细推演",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { onGenerateNextDaysBatch?.invoke(3) },
                                enabled = !isGeneratingDay,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "⚡ 推进后续 3 天",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                NotionCard(
                    title = "备考每日任务全景流水",
                    calloutIcon = "🗓️",
                    badgeText = "共 ${contract.dailySchedule.size} 天"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        contract.dailySchedule.forEach { mission ->
                            ScheduleDayItem(
                                mission = mission,
                                onClick = { onSelectDay(mission.day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhaseMilestoneCard(
    phase: PhaseMilestone,
    onJumpToDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMonth = phase.phaseType == "MONTH"
    val accentColor = if (isMonth) AcademicBlue else AcademicPurple

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title & Range Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isMonth) "M${phase.phaseNumber}" else "W${phase.phaseNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = phase.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = phase.timeRange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Core Objective Quote Box
            if (phase.coreObjective.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🎯", fontSize = 12.sp)
                        Text(
                            text = phase.coreObjective,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Target Books Requirement Checklist
            if (phase.targetBooks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "本期必须结项书目要求 (${phase.targetBooks.size} 本):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    phase.targetBooks.forEach { bookReq ->
                        PhaseBookItemRow(bookReq = bookReq)
                    }
                }
            }

            // Deliverables Checklist
            if (phase.deliverables.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "验收与检验交付标准:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    phase.deliverables.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AcademicGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick Jump Button to Phase's starting Day
            val startDayInt = phase.timeRange.substringAfter("第 ").substringBefore("-").toIntOrNull() ?: 1
            OutlinedButton(
                onClick = { onJumpToDay(startDayInt) },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "进入第 $startDayInt 天每日日程",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseBookItemRow(
    bookReq: PhaseBookRequirement,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (bookReq.subject.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AcademicBlueSubtle
                        ) {
                            Text(
                                text = bookReq.subject,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = bookReq.bookTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (bookReq.status == "已结项") AcademicGreenSubtle else AcademicAmberSubtle
                ) {
                    Text(
                        text = bookReq.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bookReq.status == "已结项") AcademicGreen else AcademicAmber,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = "📖 范围: ${bookReq.targetScope}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (bookReq.associatedProblems.isNotBlank()) {
                Text(
                    text = "✍️ 习题: ${bookReq.associatedProblems}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpacedBadge(
    label: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AcademicBlue
            )
            Text(
                text = sub,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScheduleDayItem(
    mission: DailyMission,
    onClick: () -> Unit
) {
    val isMilestone = mission.coreFocus.contains("MILESTONE", ignoreCase = true)
    val isBuffer = mission.coreFocus.contains("Buffer", ignoreCase = true)
    val allCompleted = mission.tasks.isNotEmpty() && mission.tasks.all { it.completed }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when {
            allCompleted -> AcademicGreenSubtle.copy(alpha = 0.3f)
            isMilestone -> AcademicAmberSubtle.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = BorderStroke(
            1.dp,
            if (allCompleted) AcademicGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (allCompleted) AcademicGreen
                        else if (isMilestone) AcademicAmber
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (allCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已完成",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = "D${mission.day}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMilestone) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mission.coreFocus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isMilestone -> AcademicAmber
                            allCompleted -> AcademicGreen
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${mission.allocatedMinutes}分钟",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mission.phaseLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AcademicBlueSubtle
                        ) {
                            Text(
                                text = mission.phaseLabel,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AcademicBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    val isTasksEmpty = mission.tasks.isEmpty()
                    Text(
                        text = if (isTasksEmpty) "${mission.date} • 待 AI 逐日细化" else "${mission.date} • ${mission.tasks.size} 项任务",
                        fontSize = 11.sp,
                        color = if (isTasksEmpty) AcademicBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isTasksEmpty) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AcademicBlueSubtle
                        ) {
                            Text(
                                text = "✨ 待 AI 生成",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AcademicBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (isMilestone) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AcademicAmberSubtle
                        ) {
                            Text(
                                text = "阶段诊断",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AcademicAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (mission.phaseBookGoal.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mission.phaseBookGoal,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BookLifecycleSummaryCard(
    contract: StudySyncContract,
    onAddPhaseBooks: (() -> Unit)? = null
) {
    val archivedBooks = contract.archivedCompletedBooks
    val sprintBooks = contract.bookProgressProfiles.filter { it.targetFinishDays != null }
    val offsetBooks = contract.bookProgressProfiles.filter { it.startChapter > 1 || it.completedPercent > 0 }

    if (archivedBooks.isEmpty() && sprintBooks.isEmpty() && contract.stageTransitionNotes.isBlank() && onAddPhaseBooks == null) {
        return
    }

    NotionCard(
        title = "书目生命周期与多阶段结项中枢",
        calloutIcon = "⚡",
        badgeText = "动态结项归档",
        modifier = Modifier.testTag("card_book_lifecycle_hub")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Stage Transition Note if present
            if (contract.stageTransitionNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AcademicBlueSubtle.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AcademicBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = contract.stageTransitionNotes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Sprint Target Goals
            if (sprintBooks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🎯 当前冲刺目标书目 (计划${sprintBooks.firstOrNull()?.targetFinishDays ?: 15}天突击完成):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcademicAmber
                    )
                    sprintBooks.forEach { profile ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AcademicAmberSubtle.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, AcademicAmber.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = AcademicAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = profile.bookTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "前 ${profile.targetFinishDays} 天集中攻坚",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AcademicAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Partially Studied / Offset Books
            if (offsetBooks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "📖 既有进度衔接书目 (已跳过既学章节):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcademicBlue
                    )
                    offsetBooks.forEach { profile ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = profile.bookTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "已学${profile.completedPercent}% · 从第${profile.startChapter}章起排",
                                    fontSize = 11.sp,
                                    color = AcademicBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Archived Completed Books
            if (archivedBooks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🏆 已学完结项归档书目 (已彻底从每日日程中删除):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcademicGreen
                    )
                    archivedBooks.forEach { book ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AcademicGreenSubtle.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, AcademicGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AcademicGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = book,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AcademicGreenSubtle
                                ) {
                                    Text(
                                        text = "已结项 · 免排期",
                                        fontSize = 10.sp,
                                        color = AcademicGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Phased Append Button
            if (onAddPhaseBooks != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onAddPhaseBooks,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AcademicBlue)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = AcademicBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("阶段追加新书 (如15天后新任务)", fontSize = 11.sp, color = AcademicBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

