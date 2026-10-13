package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.StudySyncContract
import com.example.ui.components.NotionCard
import com.example.ui.components.PaceStatusBadge
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicGreenSubtle
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle
import com.example.ui.theme.AcademicRose
import com.example.ui.theme.AcademicRoseSubtle

@Composable
fun ComparisonScreen(
    contractA: StudySyncContract?,
    contractB: StudySyncContract?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showReportPreview by remember { mutableStateOf(false) }

    val comp = contractA?.dashboardComparison ?: contractB?.dashboardComparison

    val rateA = contractA?.planOverview?.completionRate?.replace("%", "")?.toIntOrNull() ?: 0
    val rateB = contractB?.planOverview?.completionRate?.replace("%", "")?.toIntOrNull() ?: 0
    val progressA = rateA / 100f
    val progressB = rateB / 100f

    val diff = rateA - rateB

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Memory Isolation Guarantee Banner (Clickable with Preview & Export CTA)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("banner_memory_isolation")
                .clickable { showReportPreview = true }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "隔离分区",
                        tint = AcademicBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "严格内存隔离分区运行中",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AcademicBlueSubtle
                            ) {
                                Text(
                                    text = "已加密隔离",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    color = AcademicBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "画像 A 与画像 B 各自独立维护专属备考规划、错题执行日志及自适应重校模型，点击可预览并导出全局对比简报。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showReportPreview = true },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                    modifier = Modifier.testTag("btn_preview_isolation_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("预览/导出", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Head-to-Head Comparative Velocity Gauge
        NotionCard(
            title = "双画像复习推进速率与进度差值",
            calloutIcon = "📊",
            badgeText = "统一大盘"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Differential Highlight
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (diff >= 0) AcademicGreenSubtle else AcademicPurpleSubtle,
                    border = BorderStroke(1.dp, if (diff >= 0) AcademicGreen else AcademicPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "速率差异评估",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                color = if (diff >= 0) AcademicGreen else AcademicPurple
                            )
                            Text(
                                text = comp?.velocityDiff ?: "同步基线 (进度差异 0%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { showReportPreview = true },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_export_velocity_report")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导出报告", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                tint = if (diff >= 0) AcademicGreen else AcademicPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Comparative Progress Dual Bars
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Profile A Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AcademicBlue)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "画像 A (本人)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$rateA%",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressA },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AcademicBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Profile B Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AcademicPurple)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "画像 B (学友)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$rateB%",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AcademicPurple
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressB },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AcademicPurple,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Side-by-Side Profile Dossiers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileSummaryDossier(
                profileName = "画像 A (本人)",
                accentColor = AcademicBlue,
                contract = contractA,
                modifier = Modifier.weight(1f)
            )

            ProfileSummaryDossier(
                profileName = "画像 B (学友)",
                accentColor = AcademicPurple,
                contract = contractB,
                modifier = Modifier.weight(1f)
            )
        }

        // Milestone Benchmark Race
        NotionCard(
            title = "里程碑阶段评估达标竞速",
            calloutIcon = "🏁",
            badgeText = "里程碑"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("25% 基础筑基诊断", "50% 强化中枢评估", "75% 综合真题冲刺", "100% 考前全真模拟").forEachIndexed { idx, stage ->
                    val threshold = (idx + 1) * 25
                    val aReached = rateA >= threshold
                    val bReached = rateB >= threshold

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CheckpointPill(
                                    label = "画像A",
                                    achieved = aReached,
                                    color = AcademicBlue
                                )
                                CheckpointPill(
                                    label = "画像B",
                                    achieved = bReached,
                                    color = AcademicPurple
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Comparison Report Preview & Export Dialog
    if (showReportPreview) {
        ComparativeReportPreviewDialog(
            contractA = contractA,
            contractB = contractB,
            onDismiss = { showReportPreview = false }
        )
    }
}

@Composable
fun ComparativeReportPreviewDialog(
    contractA: StudySyncContract?,
    contractB: StudySyncContract?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val rateA = contractA?.planOverview?.completionRate?.replace("%", "")?.toIntOrNull() ?: 0
    val rateB = contractB?.planOverview?.completionRate?.replace("%", "")?.toIntOrNull() ?: 0
    val diff = rateA - rateB
    val comp = contractA?.dashboardComparison ?: contractB?.dashboardComparison

    fun generateComparisonMarkdown(): String {
        val sb = StringBuilder()
        sb.appendLine("# StudySync AI — 双画像学友备考推进与内存隔离对比简报")
        sb.appendLine("生成时间: 实时动态同步 | 内存隔离保障状态: 严格生效中 [已加密隔离]")
        sb.appendLine()
        sb.appendLine("## 一、全局推进速率与差距评估")
        sb.appendLine("- 画像 A (本人): 完成度 $rateA% | 状态: ${contractA?.planOverview?.currentPaceStatus ?: "未配置"} | 总预算: ${contractA?.planOverview?.totalEstimatedHours ?: 0}小时 | 考前截止: ${contractA?.planOverview?.targetDate ?: "-"}")
        sb.appendLine("- 画像 B (学友): 完成度 $rateB% | 状态: ${contractB?.planOverview?.currentPaceStatus ?: "未配置"} | 总预算: ${contractB?.planOverview?.totalEstimatedHours ?: 0}小时 | 考前截止: ${contractB?.planOverview?.targetDate ?: "-"}")
        sb.appendLine("- 相对速率差值: ${comp?.velocityDiff ?: "进度差异 $diff%"}")
        sb.appendLine()
        sb.appendLine("## 二、四大阶段里程碑诊断达标竞速")
        listOf("25% 基础筑基诊断", "50% 强化中枢评估", "75% 综合真题冲刺", "100% 考前全真模拟").forEachIndexed { idx, stage ->
            val threshold = (idx + 1) * 25
            val aStr = if (rateA >= threshold) "【画像A: 已达标✓】" else "【画像A: 进行中...】"
            val bStr = if (rateB >= threshold) "【画像B: 已达标✓】" else "【画像B: 进行中...】"
            sb.appendLine("- $stage: $aStr $bStr")
        }
        sb.appendLine()
        sb.appendLine("## 三、严谨学术规划与内存隔离合规声明")
        sb.appendLine("1. 独立性：画像 A 与画像 B 的参考书目、精讲章节、做题错题率及个人反思笔记完全处于隔离存储沙箱中，互不干扰、互不泄露。")
        sb.appendLine("2. 协同性：本报告仅汇总高阶进度百分比与里程碑达成状态，用于良性学友竞争激励与考前配速动态参照。")
        return sb.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "双画像学友对比报告预览",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "严格内存隔离保障 • 速率差值与里程碑竞速",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Isolation Verification Badge
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
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AcademicBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "内存安全隔离：两份档案独立运算，仅同步脱敏高阶分析指标",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AcademicBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Comparison Cards Container
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "备考进度与速率差值",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("画像 A (本人)", fontSize = 11.sp, color = AcademicBlue, fontWeight = FontWeight.Bold)
                                Text("完成率: $rateA%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("状态: ${contractA?.planOverview?.currentPaceStatus ?: "按期推进"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("画像 B (学友)", fontSize = 11.sp, color = AcademicPurple, fontWeight = FontWeight.Bold)
                                Text("完成率: $rateB%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("状态: ${contractB?.planOverview?.currentPaceStatus ?: "按期推进"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (diff >= 0) AcademicGreenSubtle else AcademicPurpleSubtle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "相对推进速率: ${comp?.velocityDiff ?: "进度差异 $diff%"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (diff >= 0) AcademicGreen else AcademicPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Markdown Source Code Box
                Text(
                    text = "格式化报告全文 (可复制/导出)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val reportContent = remember(contractA, contractB) { generateComparisonMarkdown() }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reportContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(reportContent))
                            Toast.makeText(context, "双画像对比报告已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_copy_comparison_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制报告", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(reportContent))
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, reportContent)
                                    type = "text/plain"
                                }
                                val chooser = Intent.createChooser(sendIntent, "导出双画像对比简报")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                                Toast.makeText(context, "已调起系统导出，简报同时已备份至剪贴板！", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "报告已复制到剪贴板，可直接粘贴使用！", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_comparison_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出 / 分享", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryDossier(
    profileName: String,
    accentColor: Color,
    contract: StudySyncContract?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = profileName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (contract != null) {
                Text(
                    text = contract.planOverview.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = "考前截止: ${contract.planOverview.targetDate}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("状态:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PaceStatusBadge(status = contract.planOverview.currentPaceStatus)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("总预算:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${contract.planOverview.totalEstimatedHours}小时",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("完成率:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = contract.planOverview.completionRate,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            } else {
                Text(
                    text = "尚未配置备考规划。",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckpointPill(
    label: String,
    achieved: Boolean,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (achieved) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (achieved) color else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (achieved) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (achieved) "已达标" else "未达标",
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                color = if (achieved) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
