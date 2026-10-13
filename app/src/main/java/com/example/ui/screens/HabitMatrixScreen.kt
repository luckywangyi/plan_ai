package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.A4MatrixExporter
import com.example.model.ContractTask
import com.example.model.DailyMission
import com.example.model.StudySyncContract
import com.example.ui.components.NotionCard
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicRose

@Composable
fun HabitMatrixScreen(
    contract: StudySyncContract,
    onToggleTask: ((dayNumber: Int, taskId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showPrintPreviewDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 每日任务全景打卡清单, 1: A4 纸质排版表格
    var filterStatus by remember { mutableIntStateOf(0) } // 0: 全部, 1: 待打卡, 2: 已全部完成

    val onExportPdf: () -> Unit = {
        try {
            val pdfFile = A4MatrixExporter.exportToPdf(context, contract)
            A4MatrixExporter.shareFile(context, pdfFile, "application/pdf", "导出 A4 备考打卡表 (PDF)")
            Toast.makeText(context, "A4 PDF 导出成功，已调起系统通道！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "导出 PDF 失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val onExportImage: () -> Unit = {
        try {
            val imageFile = A4MatrixExporter.exportToPng(context, contract)
            A4MatrixExporter.shareFile(context, imageFile, "image/png", "导出 A4 备考打卡表 (PNG 高清图片)")
            Toast.makeText(context, "A4 高清图片已生成，已调起系统通道！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "导出图片失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val onSystemPrint: () -> Unit = {
        try {
            val pdfFile = A4MatrixExporter.exportToPdf(context, contract)
            A4MatrixExporter.printPdf(context, pdfFile, "StudySync_${contract.profileId}_A4打卡表")
        } catch (e: Exception) {
            Toast.makeText(context, "调起系统打印失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateMarkdownMatrix(): String {
        val sb = StringBuilder()
        val profileLabel = if (contract.profileId == "profile_a") "画像A (主备考人)" else "画像B (研友协作)"
        sb.appendLine("# StudySync AI — 考研全周期备考打卡表")
        sb.appendLine("报考科目: ${contract.planOverview.title} | 考前硬性截止: ${contract.planOverview.targetDate} | 考生分区: $profileLabel")
        sb.appendLine("总计划课时: ${contract.planOverview.totalEstimatedHours}小时 | 完成率: ${contract.planOverview.completionRate}")
        sb.appendLine()
        sb.appendLine("### 阶段考核里程碑")
        contract.milestones.forEach { m ->
            sb.appendLine("- [${if (m.completed) "x" else " "}] ${m.stage} (${m.targetDate}): ${m.assessmentTopic}")
        }
        sb.appendLine()
        sb.appendLine("### 每日备考任务清单及打卡记录")
        sb.appendLine("| 天数 | 日期 | 当日所有备考任务 (打卡勾选) | 完成进度 |")
        sb.appendLine("|---|---|---|---|")
        contract.dailySchedule.forEach { mission ->
            val tasksText = if (mission.tasks.isEmpty()) {
                mission.coreFocus
            } else {
                mission.tasks.joinToString("<br>") { task ->
                    val mark = if (task.completed) "[x]" else "[ ]"
                    val cleanMat = A4MatrixExporter.cleanMaterialTitle(task.material)
                    val cleanScope = A4MatrixExporter.cleanScopeForChecklist(task.scope)
                    "$mark 《$cleanMat》$cleanScope (${task.estimatedMinutes}m)"
                }
            }
            val doneCount = mission.tasks.count { it.completed }
            val totalCount = mission.tasks.size
            val status = if (totalCount > 0 && doneCount == totalCount) "已全打卡 ✓" else "$doneCount/$totalCount"
            sb.appendLine("| D${mission.day} | ${mission.date} | $tasksText | $status |")
        }
        return sb.toString()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Print Sheet Actions Card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "考研备考每日任务打卡表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "完整展开当日所有攻坚任务，完成即可打勾确认，支持 A4 打印与导出",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showPrintPreviewDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_preview_matrix")
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("A4 排版预览", fontSize = 12.sp)
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_pdf")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出 PDF", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onExportImage,
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("btn_export_image")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出图片", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onSystemPrint,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("btn_direct_print")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("系统打印", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = generateMarkdownMatrix()
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "A4 备考打卡表已成功复制到剪贴板！", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_copy_matrix")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制MD", fontSize = 12.sp)
                    }
                }
            }
        }

        // View Mode Switcher: 清单全景打卡 vs A4 纸质表格排版
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("每日全任务打卡清单 (推荐交互)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("A4 纸质打卡表排版 (打印对齐)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
        }

        if (selectedTab == 0) {
            // Filter Bar
            val totalDays = contract.dailySchedule.size
            val pendingDays = contract.dailySchedule.count { mission ->
                mission.tasks.isNotEmpty() && !mission.tasks.all { it.completed }
            }
            val completedDays = contract.dailySchedule.count { mission ->
                mission.tasks.isNotEmpty() && mission.tasks.all { it.completed }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterStatus == 0,
                    onClick = { filterStatus = 0 },
                    label = { Text("全部 ($totalDays 天)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = filterStatus == 1,
                    onClick = { filterStatus = 1 },
                    label = { Text("待打卡 ($pendingDays 天)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = filterStatus == 2,
                    onClick = { filterStatus = 2 },
                    label = { Text("已达成 ($completedDays 天)", fontSize = 11.sp) }
                )
            }

            // Filtered Daily Cards List
            val displayedDays = contract.dailySchedule.filter { mission ->
                when (filterStatus) {
                    1 -> mission.tasks.isNotEmpty() && !mission.tasks.all { it.completed }
                    2 -> mission.tasks.isNotEmpty() && mission.tasks.all { it.completed }
                    else -> true
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayedDays.forEach { mission ->
                    val doneCount = mission.tasks.count { it.completed }
                    val totalCount = mission.tasks.size
                    val allDone = totalCount > 0 && doneCount == totalCount

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (allDone) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (allDone) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Daily Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (allDone) AcademicGreen else AcademicBlue
                                    ) {
                                        Text(
                                            text = "Day ${mission.day}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = mission.date,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "预计 ${mission.allocatedMinutes} 分钟",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Status Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (allDone) Color(0xFFDCFCE7) else if (doneCount > 0) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(0.8.dp, if (allDone) Color(0xFF16A34A) else if (doneCount > 0) Color(0xFF3B82F6) else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = if (allDone) "全部打卡 ✓" else "$doneCount / $totalCount 完成",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allDone) Color(0xFF15803D) else if (doneCount > 0) Color(0xFF1D4ED8) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Complete display of ALL tasks for the day
                            if (mission.tasks.isEmpty()) {
                                Text(
                                    text = mission.coreFocus,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    mission.tasks.forEachIndexed { taskIdx, task ->
                                        val taskId = task.id.ifBlank { "d${mission.day}_t$taskIdx" }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (task.completed) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                            border = BorderStroke(0.8.dp, if (task.completed) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onToggleTask?.invoke(mission.day, taskId)
                                                    val actionMsg = if (!task.completed) "已打卡: 《${task.material}》" else "已取消打卡: 《${task.material}》"
                                                    Toast.makeText(context, actionMsg, Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Checkbox(
                                                    checked = task.completed,
                                                    onCheckedChange = {
                                                        onToggleTask?.invoke(mission.day, taskId)
                                                        val actionMsg = if (!task.completed) "已打卡: 《${task.material}》" else "已取消打卡: 《${task.material}》"
                                                        Toast.makeText(context, actionMsg, Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = AcademicGreen,
                                                        uncheckedColor = Color(0xFF64748B)
                                                    ),
                                                    modifier = Modifier.testTag("checkbox_task_${mission.day}_$taskIdx")
                                                )

                                                Column(modifier = Modifier.weight(1f)) {
                                                    val cleanMat = A4MatrixExporter.cleanMaterialTitle(task.material)
                                                    val cleanScope = A4MatrixExporter.cleanScopeForChecklist(task.scope)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "《$cleanMat》",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (task.completed) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface,
                                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = when (task.taskType) {
                                                                "reading" -> Color(0xFFDBEAFE)
                                                                "practice" -> Color(0xFFDCFCE7)
                                                                "lecture" -> Color(0xFFF3E8FF)
                                                                else -> Color(0xFFFEF3C7)
                                                            }
                                                        ) {
                                                            Text(
                                                                text = when (task.taskType) {
                                                                    "reading" -> "精读"
                                                                    "practice" -> "刷题"
                                                                    "lecture" -> "网课"
                                                                    else -> "复习"
                                                                },
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = when (task.taskType) {
                                                                    "reading" -> Color(0xFF1D4ED8)
                                                                    "practice" -> Color(0xFF15803D)
                                                                    "lecture" -> Color(0xFF7E22CE)
                                                                    else -> Color(0xFFB45309)
                                                                },
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = cleanScope,
                                                        fontSize = 12.sp,
                                                        color = if (task.completed) Color(0xFF6B7280) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                                    )
                                                }

                                                // Time pill
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF1F5F9)
                                                ) {
                                                    Text(
                                                        text = "${task.estimatedMinutes}分钟",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF475569),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Mode 1: A4 Paper Sheet View (Print Alignment)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White,
                border = BorderStroke(2.dp, Color(0xFFD4D4D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Document Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STUDYSYNC AI — 考研备考每日攻坚打卡矩阵 (A4)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = contract.planOverview.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            val profileLabel = if (contract.profileId == "profile_a") "画像A (主备考人)" else "画像B (研友协作)"
                            Text(
                                text = "考生分区: $profileLabel • 总计划: ${contract.planOverview.totalEstimatedHours} 小时 • 完成率: ${contract.planOverview.completionRate}",
                                fontSize = 11.sp,
                                color = Color(0xFF4B5563)
                            )
                        }

                        Surface(
                            border = BorderStroke(1.dp, Color(0xFF111827)),
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFFF9FAFB)
                        ) {
                            Text(
                                text = "考前截止:\n${contract.planOverview.targetDate}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF111827),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Milestones Checklist Row
                    Surface(
                        border = BorderStroke(1.dp, Color(0xFF9CA3AF)),
                        shape = RoundedCornerShape(2.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            contract.milestones.take(4).forEach { m ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .border(1.dp, Color(0xFF111827))
                                                .background(if (m.completed) Color(0xFF059669) else Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (m.completed) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = m.stage.take(10),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                    Text(
                                        text = m.targetDate,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clean 4-Column Table: 天数 | 日期 | 当日备考任务清单 (带打卡勾选) | 状态
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF94A3B8))
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("天数", width40dp(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("日期", width50dp(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("当日所有备考攻坚任务 (每项打勾确认)", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("状态", width60dp(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        // Daily Rows
                        contract.dailySchedule.forEachIndexed { index, mission ->
                            val isEven = index % 2 == 0
                            val rowBg = if (isEven) Color.White else Color(0xFFF8FAFC)
                            val doneCount = mission.tasks.count { it.completed }
                            val totalCount = mission.tasks.size
                            val allDone = totalCount > 0 && doneCount == totalCount

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .border(BorderStroke(0.5.dp, Color(0xFFCBD5E1)))
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("D${mission.day}", width40dp(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text(mission.date.takeLast(5), width50dp(), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF475569))

                                // Task Checklist in Table Cell
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (mission.tasks.isEmpty()) {
                                        Text(mission.coreFocus, fontSize = 9.5.sp, color = Color(0xFF64748B))
                                    } else {
                                        mission.tasks.forEachIndexed { tIdx, task ->
                                            val taskId = task.id.ifBlank { "d${mission.day}_t$tIdx" }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onToggleTask?.invoke(mission.day, taskId) },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (task.completed) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                    contentDescription = null,
                                                    tint = if (task.completed) Color(0xFF059669) else Color(0xFF64748B),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                val cleanMat = A4MatrixExporter.cleanMaterialTitle(task.material)
                                                val cleanScope = A4MatrixExporter.cleanScopeForChecklist(task.scope)
                                                Text(
                                                    text = "《$cleanMat》$cleanScope (${task.estimatedMinutes}m)",
                                                    fontSize = 9.sp,
                                                    color = if (task.completed) Color(0xFF059669) else Color(0xFF0F172A),
                                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            }
                                        }
                                    }
                                }

                                // Status Pill
                                Box(modifier = width60dp(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (allDone) "全达成 ✓" else "$doneCount/$totalCount",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allDone) Color(0xFF15803D) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Signatures Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("考生手写核签: ____________________", fontSize = 9.sp, color = Color(0xFF475569))
                        Text("研友/导师复核: ____________________", fontSize = 9.sp, color = Color(0xFF475569))
                    }
                }
            }
        }
    }

    // A4 Print & Layout Full Preview Dialog
    if (showPrintPreviewDialog) {
        HabitMatrixPreviewDialog(
            contract = contract,
            markdownText = generateMarkdownMatrix(),
            onExportPdf = onExportPdf,
            onExportImage = onExportImage,
            onSystemPrint = onSystemPrint,
            onDismiss = { showPrintPreviewDialog = false }
        )
    }
}

private fun width40dp() = Modifier.width(36.dp)
private fun width50dp() = Modifier.width(48.dp)
private fun width60dp() = Modifier.width(56.dp)

@Composable
fun HabitMatrixPreviewDialog(
    contract: StudySyncContract,
    markdownText: String,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit,
    onSystemPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val a4Bitmap = remember(contract) {
        try {
            A4MatrixExporter.renderMatrixBitmap(contract)
        } catch (e: Exception) {
            null
        }
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
                    .padding(16.dp)
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
                            text = "A4 打卡表打印与实景导出",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "可直接导出标准 A4 PDF 文档或 1240×1754 高清图片",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Visual Layout vs Markdown Source
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("A4 纸质实景渲染", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Markdown 源码表格", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    if (a4Bitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "A4 矢量印刷标准 (1240 × 1754 px)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "含全科目/里程碑/签名栏",
                                        fontSize = 10.sp,
                                        color = AcademicBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(390.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0))
                                        .verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Image(
                                        bitmap = a4Bitmap.asImageBitmap(),
                                        contentDescription = "A4 备考打卡表打印实景",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback Simulated Paper Sheet
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = contract.planOverview.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("A4 打卡纸正在实时生成渲染中...", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    // Markdown Code Block
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = markdownText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Export Actions (Dual Row)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onExportPdf()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_dialog_export_pdf")
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出 PDF 文档", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onExportImage()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("btn_dialog_export_image")
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出高清图片 (PNG)", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSystemPrint()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_dialog_system_print")
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("系统无线打印", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(markdownText))
                                Toast.makeText(context, "A4 备考打卡表已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_dialog_copy_matrix")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制 Markdown", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
