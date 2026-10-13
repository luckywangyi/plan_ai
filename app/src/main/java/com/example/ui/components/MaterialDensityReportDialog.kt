package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MaterialChapterInfo
import com.example.model.MaterialContentDensity
import com.example.model.ParsedAcademicMaterial
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialDensityReportDialog(
    material: ParsedAcademicMaterial,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("dialog_material_density_report"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AcademicBlueSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (material.sourceType == "PDF") Icons.Default.Description else Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = AcademicBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "学术材料密度与体量报告",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${material.fileName} · [${material.subject}]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Core Cognitive Load & Density Tier Card
                val density = material.contentDensity
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AcademicBlueSubtle.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = AcademicBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = density.densityTier,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AcademicBlue
                                )
                            }
                            Text(
                                text = "认知负荷系数: ${String.format(Locale.US, "%.2f", density.cognitiveLoadIndex)}x",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AcademicBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "建议单页精读: ${String.format(Locale.US, "%.1f", density.recommendedMinutesPerPage)} 分钟/页 · 单章预算: 约 ${density.recommendedMinutesPerChapter} 分钟",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = material.suggestedDailyPace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reading Volumes Metric Grid
                Text(
                    text = "📖 阅读体量提取指标 (Reading Volumes)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VolumeMetricBox(
                        title = "总页数",
                        value = "${material.readingVolume.totalPages} 页",
                        subtitle = "折算教材印刷页",
                        modifier = Modifier.weight(1f)
                    )
                    VolumeMetricBox(
                        title = "核心章节",
                        value = "${material.readingVolume.totalChapters} 大章",
                        subtitle = "已抽取知识单元",
                        modifier = Modifier.weight(1f)
                    )
                    VolumeMetricBox(
                        title = "配套例题/习题",
                        value = "${material.readingVolume.estimatedProblemsCount} 道",
                        subtitle = "待攻坚演练量",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Density Breakdown
                Text(
                    text = "🔬 学术内容密度细分 (Content Density Radar)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DensityProgressRow(
                            icon = Icons.Default.Functions,
                            label = "公式与定理推导密度 (Formula)",
                            value = density.formulaDensity,
                            color = Color(0xFFE65100)
                        )
                        DensityProgressRow(
                            icon = Icons.Default.Code,
                            label = "代码、算法与数据结构密度 (Code/Algo)",
                            value = density.codeDensity,
                            color = Color(0xFF0D47A1)
                        )
                        DensityProgressRow(
                            icon = Icons.Default.Speed,
                            label = "核心考纲概念与术语密度 (Concept)",
                            value = density.conceptDensity,
                            color = Color(0xFF1B5E20)
                        )
                        DensityProgressRow(
                            icon = Icons.Default.Description,
                            label = "课后习题与例题题量分布 (Practice)",
                            value = density.exerciseDensity,
                            color = Color(0xFF4A148C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chapter Breakdown Table
                if (material.readingVolume.chapters.isNotEmpty()) {
                    Text(
                        text = "📑 抽取章节结构与日配额 (Extracted Chapters)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        material.readingVolume.chapters.take(8).forEach { ch ->
                            ChapterRowItem(ch)
                        }
                        if (material.readingVolume.chapters.size > 8) {
                            Text(
                                text = "… 另有 ${material.readingVolume.chapters.size - 8} 章节已录入计划生成库",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Strategic Summary
                if (material.aiSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AcademicBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = material.aiSummary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue)
                ) {
                    Text("完成核验，返回规划", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VolumeMetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AcademicBlue)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DensityProgressRow(
    icon: ImageVector,
    label: String,
    value: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                "${(value * 100).toInt()}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0.01f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun ChapterRowItem(chapter: MaterialChapterInfo) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chapter.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chapter.pageCount > 0) {
                    Text(
                        text = "p.${chapter.startPage}-${chapter.endPage} (${chapter.pageCount}p)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (chapter.densityLevel == "高难") Color(0xFFFFEBEE) else AcademicBlueSubtle
                ) {
                    Text(
                        text = chapter.densityLevel,
                        fontSize = 10.sp,
                        color = if (chapter.densityLevel == "高难") Color(0xFFC62828) else AcademicBlue,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
