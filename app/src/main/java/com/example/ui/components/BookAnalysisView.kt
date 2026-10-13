package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
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
import com.example.model.AnalyzedBookItem
import com.example.model.BookAnalysisReport
import com.example.model.GroundingSourceItem
import com.example.model.StrategicAdviceItem
import com.example.model.WorkloadMetrics
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

/**
 * Full AI Book & Workload Analysis Card.
 * Displays book breakdown, question counts, estimated study times, strategic advice,
 * and Search Grounding citations from real exam syllabi.
 */
@Composable
fun BookAnalysisCard(
    report: BookAnalysisReport,
    onMarkBookCompleted: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedBooks by remember { mutableStateOf(true) }
    var expandedAdvice by remember { mutableStateOf(true) }

    NotionCard(
        title = "AI 书目深度研读与刷题时间测算报告",
        calloutIcon = "🧠",
        badgeText = "Gemini 3.6 Flash 考情检索",
        modifier = modifier.testTag("card_book_analysis")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Feasibility Assessment Banner
            WorkloadFeasibilityBanner(metrics = report.workloadMetrics, assessment = report.feasibilityAssessment)

            // Workload Metrics Grid
            WorkloadMetricsGrid(metrics = report.workloadMetrics)

            // Books Breakdown Section
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedBooks = !expandedBooks },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = AcademicBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "导入教材题集特征分析 (${report.booksAnalyzed.size} 册)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (expandedBooks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expandedBooks) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = expandedBooks) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            report.booksAnalyzed.forEach { item ->
                                AnalyzedBookItemRow(item = item, onMarkBookCompleted = onMarkBookCompleted)
                            }
                        }
                    }
                }
            }

            // Strategic Pedagogical Advice Section
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AcademicPurpleSubtle.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, AcademicPurple.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedAdvice = !expandedAdvice },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AcademicPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "备考规划额外战略建议 (${report.strategicAdvice.size} 项)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AcademicPurple
                            )
                        }
                        Icon(
                            imageVector = if (expandedAdvice) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expandedAdvice) "收起" else "展开",
                            tint = AcademicPurple
                        )
                    }

                    AnimatedVisibility(visible = expandedAdvice) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            report.strategicAdvice.forEach { advice ->
                                StrategicAdviceCard(item = advice)
                            }
                        }
                    }
                }
            }

            // Search Grounding Citations (if present)
            if (report.searchGroundingSources.isNotEmpty()) {
                SearchGroundingFooter(sources = report.searchGroundingSources)
            }
        }
    }
}

@Composable
private fun WorkloadFeasibilityBanner(
    metrics: WorkloadMetrics,
    assessment: String
) {
    val isDeficit = metrics.paceDeficitOrSurplusHours < -0.3f
    val isTight = metrics.paceDeficitOrSurplusHours in -0.3f..0.5f

    val (bgColor, borderColor, textColor, badgeText) = when {
        isDeficit -> Quad(AcademicRoseSubtle, AcademicRose, AcademicRose, "时间紧张")
        isTight -> Quad(AcademicAmberSubtle, AcademicAmber, AcademicAmber, "紧凑打平")
        else -> Quad(AcademicGreenSubtle, AcademicGreen, AcademicGreen, "从容充裕")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "负荷评估: ${metrics.paceFeasibility}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = borderColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeText,
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = assessment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "推荐日均配比:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = metrics.recommendedDailyRatio,
                        style = MaterialTheme.typography.labelSmall,
                        color = AcademicBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkloadMetricsGrid(metrics: WorkloadMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricTile(
            title = "精读耗时",
            value = "${metrics.totalReadingHours}h",
            subtitle = "理论教材研读",
            color = AcademicBlue,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "刷题耗时",
            value = "${metrics.totalPracticeHours}h",
            subtitle = "真题限时演练",
            color = AcademicGreen,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "复盘耗时",
            value = "${metrics.totalReviewHours}h",
            subtitle = "艾宾浩斯重测",
            color = AcademicPurple,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "日均需耗",
            value = "${metrics.dailyHoursRequired}h",
            subtitle = "可用 ${metrics.dailyHoursAvailable}h/天",
            color = if (metrics.paceDeficitOrSurplusHours < 0) AcademicRose else AcademicAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnalyzedBookItemRow(
    item: AnalyzedBookItem,
    onMarkBookCompleted: ((String) -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (item.isCompleted) AcademicGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AcademicGreenSubtle
                        ) {
                            Text(
                                text = "已结项归档 (免排期)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = AcademicGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        if (item.targetFinishDays != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AcademicAmberSubtle
                            ) {
                                Text(
                                    text = "⚡ ${item.targetFinishDays}天冲刺结项",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = AcademicAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.completedPercent > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AcademicBlueSubtle
                            ) {
                                Text(
                                    text = "已学${item.completedPercent}% · 从第${item.startChapter}章起",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = AcademicBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AcademicBlueSubtle
                    ) {
                        Text(
                            text = item.bookType,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AcademicBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.difficultyLevel.contains("高") || item.difficultyLevel.contains("极")) AcademicRoseSubtle else AcademicGreenSubtle
                    ) {
                        Text(
                            text = item.difficultyLevel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (item.difficultyLevel.contains("高") || item.difficultyLevel.contains("极")) AcademicRose else AcademicGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookStatBadge(
                    icon = Icons.Default.Bookmark,
                    text = "预估: ${item.estimatedTotalChapters} 章 / 单元"
                )
                if (item.estimatedQuestionCount > 0) {
                    BookStatBadge(
                        icon = Icons.Default.CheckCircle,
                        text = "题量: 约 ${item.estimatedQuestionCount} 题"
                    )
                }
                BookStatBadge(
                    icon = Icons.Default.Timer,
                    text = "每章: ${item.recommendedReadingMinutesPerChapter} min"
                )
                if (item.recommendedMinutesPerProblem > 0) {
                    BookStatBadge(
                        icon = Icons.Default.Schedule,
                        text = "每题: ${item.recommendedMinutesPerProblem} min"
                    )
                }
            }

            if (item.keyExamWeight.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "考纲定位: ${item.keyExamWeight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AcademicPurple,
                    fontWeight = FontWeight.Medium
                )
            }

            if (item.studyTip.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "💡 建议: ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AcademicAmber
                        )
                        Text(
                            text = item.studyTip,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Archive Action Button
            if (!item.isCompleted && onMarkBookCompleted != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.targetFinishDays != null) "冲刺完成后可一键结项并释放每日排期：" else "已学完此书？一键结项：",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { onMarkBookCompleted(item.title) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = AcademicGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("结项并从计划中移除", fontSize = 10.sp, color = AcademicGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookStatBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StrategicAdviceCard(item: StrategicAdviceItem) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AcademicPurple.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AcademicPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (item.priority == "HIGH") AcademicRoseSubtle else AcademicAmberSubtle
                ) {
                    Text(
                        text = if (item.priority == "HIGH") "高优先级" else "建议执行",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (item.priority == "HIGH") AcademicRose else AcademicAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchGroundingFooter(sources: List<GroundingSourceItem>) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = AcademicBlueSubtle.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = AcademicBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Google Search Grounding 权威考情考纲比对支撑:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AcademicBlue
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            sources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "• ", style = MaterialTheme.typography.labelSmall, color = AcademicBlue)
                    Column {
                        Text(
                            text = source.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (source.snippet.isNotBlank()) {
                            Text(
                                text = source.snippet,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact overview banner used on MissionScreen
 */
@Composable
fun QuickStrategyAdviceBanner(
    report: BookAnalysisReport,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AcademicBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI 备考策略建议与每日刷题配比",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AcademicBlueSubtle
                ) {
                    Text(
                        text = report.workloadMetrics.paceFeasibility.take(4),
                        color = AcademicBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "建议配比: ${report.workloadMetrics.recommendedDailyRatio}",
                style = MaterialTheme.typography.labelSmall,
                color = AcademicPurple,
                fontWeight = FontWeight.SemiBold
            )

            if (report.strategicAdvice.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val firstAdvice = report.strategicAdvice.first()
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[${firstAdvice.category}] ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AcademicBlue
                        )
                        Text(
                            text = firstAdvice.content,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
