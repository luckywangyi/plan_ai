package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.StudySyncJsonParser
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicGreenSubtle
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle
import com.example.ui.theme.AcademicRose
import com.example.ui.theme.AcademicRoseSubtle
import java.util.Locale

private fun getFileInfoFromUri(context: Context, uri: Uri): Pair<String, Long> {
    var name = "plan.json"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return Pair(name, size)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "未知大小"
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
        else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportContractDialog(
    targetProfileId: String = "Profile_A",
    rawInputs: StudyRawInputs? = null,
    existingContract: StudySyncContract? = null,
    onImport: (StudySyncContract) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Input modes: 0 = File Import (Recommended), 1 = Paste / Text Input
    var selectedTab by remember { mutableIntStateOf(0) }
    var jsonText by remember { mutableStateOf("") }

    // File metadata
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableLongStateOf(0L) }
    var fileReadError by remember { mutableStateOf<String?>(null) }
    var showRawTextPreview by remember { mutableStateOf(false) }

    val hasCustomInputs = rawInputs != null || existingContract != null
    val customizedPrompt = remember(rawInputs, existingContract, targetProfileId) {
        StudySyncJsonParser.buildCustomizedAiPrompt(rawInputs, existingContract, targetProfileId)
    }

    // System File Picker launcher (Storage Access Framework)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val (name, size) = getFileInfoFromUri(context, uri)
                selectedFileName = name
                selectedFileSize = size

                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""

                if (content.isNotBlank()) {
                    jsonText = content
                    fileReadError = null
                    Toast.makeText(context, "成功读取文件：$name (${formatBytes(size)})", Toast.LENGTH_SHORT).show()
                } else {
                    fileReadError = "所选文件内容为空，请选择有效的 JSON 规划文件"
                    Toast.makeText(context, "文件内容为空", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                fileReadError = "读取文件失败：${e.localizedMessage ?: e.message}"
                Toast.makeText(context, "读取文件异常: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val parsedContract by remember(jsonText) {
        derivedStateOf {
            if (jsonText.isBlank()) null
            else StudySyncJsonParser.lenientParseContract(jsonText, targetProfileId)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.93f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AcademicPurpleSubtle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = AcademicPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "导入外部规划 JSON 到 ${if (targetProfileId == "Profile_A") "画像 A (本人)" else "画像 B (学友)"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "将规划导入当前激活画像 · 支持外部文件或粘贴（无剪贴板限制）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_import_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prompt Helper Bar
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(customizedPrompt))
                            val msg = if (hasCustomInputs) {
                                "✓ 已复制专属 Prompt (含已勾选书目)！直接发给 DeepSeek / ChatGPT 生成完整规划"
                            } else {
                                "AI 规划 Prompt 模板已复制到剪贴板！"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        },
                        label = {
                            Text(
                                text = if (hasCustomInputs) "📋 复制专属定制 Prompt (含已选书目)" else "📋 复制 AI 规划 Prompt 模板",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = AcademicPurpleSubtle,
                            labelColor = AcademicPurple
                        ),
                        modifier = Modifier.testTag("btn_copy_prompt_template")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs (File Import vs Paste/Text)
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📁 本地 JSON 文件导入 (推荐)", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✍️ 剪贴板 / 手动粘贴", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Parse Status Card (Common for both tabs)
                if (parsedContract != null) {
                    val c = parsedContract!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AcademicGreenSubtle),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AcademicGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "✓ 契约结构解析成功！",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcademicGreen
                                )
                                Text(
                                    text = "计划：《${c.planOverview.title}》 | 目标画像：${if (targetProfileId == "Profile_A") "画像 A" else "画像 B"} | 共 ${c.dailySchedule.size} 天日程 | 合计 ${c.dailySchedule.sumOf { it.tasks.size }} 个任务项",
                                    fontSize = 11.sp,
                                    color = AcademicGreen
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (fileReadError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AcademicRoseSubtle),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AcademicRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fileReadError ?: "文件读取失败",
                                fontSize = 11.sp,
                                color = AcademicRose
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (jsonText.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AcademicRoseSubtle),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AcademicRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ 尚未识别到有效的 dailySchedule 每日日程数据。请确保文件/内容包含每日任务规划数组。",
                                fontSize = 11.sp,
                                color = AcademicRose,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // TAB 0: File Import Section
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFileName != null && parsedContract != null) AcademicGreenSubtle.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (selectedFileName != null && parsedContract != null) AcademicGreen
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedFileName != null) AcademicGreenSubtle else AcademicBlueSubtle,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (selectedFileName != null) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = if (selectedFileName != null) AcademicGreen else AcademicBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (selectedFileName != null) {
                                    Text(
                                        text = selectedFileName!!,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "文件大小：${formatBytes(selectedFileSize)} · 已完全读入内存",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("重新选择文件", fontSize = 12.sp)
                                    }
                                } else {
                                    Text(
                                        text = "点击浏览并选取 JSON 规划文件",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "支持从“下载”、网盘或微信/QQ 保存的 .json 文件直接读取",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("btn_select_json_file")
                                    ) {
                                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("选择本地 JSON 文件", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Advantages of file import card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AcademicBlueSubtle),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "💡 为什么大容量规划必须使用文件导入？",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcademicBlue
                                )
                                Text(
                                    text = "• 解决剪贴板截断：当规划长达 30~90 天时，JSON 往往包含数万至数十万字符，手机剪贴板容易被系统截断导致 JSON 损坏。\n• 内存级直接流式读取：直接选择文件可无损秒级载入超大完整日程，杜绝界面卡顿。",
                                    fontSize = 11.sp,
                                    color = AcademicBlue,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Collapsible raw text preview (if file was loaded)
                        if (jsonText.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRawTextPreview = !showRawTextPreview },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showRawTextPreview) "▼ 收起文件内容预览" else "▶ 展开查看文件内容片段 (共 ${jsonText.length} 字符)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            AnimatedVisibility(visible = showRawTextPreview) {
                                OutlinedTextField(
                                    value = if (jsonText.length > 5000) jsonText.take(5000) + "\n\n...[已省略后续 ${jsonText.length - 5000} 字符预览，导入时会自动包含完整数据]..." else jsonText,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                } else {
                    // TAB 1: Paste / Manual Text Input
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) {
                                        jsonText = clip
                                        selectedFileName = null
                                        Toast.makeText(context, "已从剪贴板粘贴", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("📋 粘贴剪贴板", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )

                            AssistChip(
                                onClick = {
                                    jsonText = StudySyncJsonParser.getSampleContractJson()
                                    selectedFileName = "sample_test_plan.json"
                                    Toast.makeText(context, "已填入标准示例规划 JSON", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("💡 填入测试示例", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )

                            if (jsonText.isNotBlank()) {
                                AssistChip(
                                    onClick = {
                                        jsonText = ""
                                        selectedFileName = null
                                    },
                                    label = { Text("清空", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = {
                                jsonText = it
                                selectedFileName = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("input_contract_json_text"),
                            placeholder = {
                                Text(
                                    text = "在此粘贴外部 AI 生成的 JSON 内容（若文字过长建议切换到「本地文件导入」模式避免截断）...",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("取消", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            parsedContract?.let { rawParsed ->
                                val contract = rawParsed.copy(profileId = targetProfileId)
                                onImport(contract)
                                val profileLabel = if (targetProfileId == "Profile_A") "画像 A" else "画像 B"
                                Toast.makeText(
                                    context,
                                    "✓ 成功导入规划至 $profileLabel！共 ${contract.dailySchedule.size} 天备战日程 (${contract.dailySchedule.sumOf { it.tasks.size }} 个任务)",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismiss()
                            }
                        },
                        enabled = parsedContract != null,
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_import_json")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (parsedContract != null) "立即载入此规划 (${parsedContract!!.dailySchedule.size} 天)" else "等待解析 JSON",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

