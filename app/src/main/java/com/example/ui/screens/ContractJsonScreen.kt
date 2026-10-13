package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudySyncJsonParser
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import com.example.ui.components.ImportContractDialog
import com.example.ui.components.NotionCard
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicGreenSubtle
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContractJsonScreen(
    contract: StudySyncContract,
    rawInputs: StudyRawInputs? = null,
    onImportContract: (StudySyncContract) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showImportDialog by remember { mutableStateOf(false) }
    var isPromptExpanded by remember { mutableStateOf(true) }

    val jsonString = remember(contract) {
        StudySyncJsonParser.toJson(contract)
    }

    // Dynamically build the prompt based on user's selected materials, progress notes, and hours
    val customizedPrompt = remember(rawInputs, contract) {
        StudySyncJsonParser.buildCustomizedAiPrompt(rawInputs, contract, contract.profileId)
    }

    // Material tags for preview
    val hasCards = rawInputs != null && rawInputs.materialCards.isNotEmpty()
    val excludedCount = if (hasCards) rawInputs!!.materialCards.count { !it.isIncluded } else 0

    val materialSummary = remember(rawInputs, contract) {
        if (rawInputs != null) {
            if (rawInputs.materialCards.isNotEmpty()) {
                rawInputs.materialCards.filter { it.isIncluded }.map { it.title to it.note }
            } else {
                (rawInputs.books + rawInputs.problemSets + rawInputs.videoLectures).map { it to "" }
            }
        } else {
            contract.bookAnalysis?.booksAnalyzed?.map { it.title to it.studyTip }
                ?: contract.dailySchedule.flatMap { it.tasks.map { t -> t.material } }.distinct().map { it to "" }
        }
    }

    val customNotesText = rawInputs?.customNotes?.ifBlank { null }
        ?: contract.adaptiveAdjustments.feedbackSummary.ifBlank { null }

    // Export contract as JSON file launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(jsonString)
                    }
                }
                Toast.makeText(context, "✓ 成功保存规划 JSON 文件！随时可再次载入", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "保存文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showImportDialog) {
        ImportContractDialog(
            targetProfileId = contract.profileId,
            rawInputs = rawInputs,
            existingContract = contract,
            onImport = { imported ->
                onImportContract(imported)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Schema Verification Header
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "结构校验通过",
                            tint = AcademicGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "备考契约 JSON 枢纽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "支持根据已选书目生成提示词，或通过 JSON 文件/剪贴板无损导入",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_open_import_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📁 导入规划文件/JSON", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(customizedPrompt))
                            val bookCount = materialSummary.size
                            Toast.makeText(
                                context,
                                "✓ 已生成并复制专属提示词（包含 ${bookCount} 项教材与批注细节）！直接发给 DeepSeek / ChatGPT 即可",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicPurple),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("btn_copy_prompt_template_screen")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📋 复制专属定制 Prompt", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val defaultName = "StudySync_Plan_${contract.profileId}_${System.currentTimeMillis() / 1000}.json"
                            exportJsonLauncher.launch(defaultName)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .testTag("btn_export_json_file")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("💾 导出为文件", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonString))
                            Toast.makeText(context, "契约 JSON 已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("btn_copy_contract_json")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制文本", fontSize = 11.sp)
                    }
                }
            }
        }

        // Dedicated External AI Prompt Generator Card
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AcademicPurple.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AcademicPurpleSubtle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = AcademicPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "外部 AI 提示词已装配完毕",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AcademicPurple
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AcademicGreenSubtle
                                ) {
                                    Text(
                                        text = if (excludedCount > 0) "已选 ${materialSummary.size} 本 · 排除 ${excludedCount} 本未选" else "已装载 ${materialSummary.size} 项资料",
                                        fontSize = 10.sp,
                                        color = AcademicGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (excludedCount > 0) "✓ 严格仅针对已勾选的 ${materialSummary.size} 项资料生成，未勾选的 ${excludedCount} 项未选书籍绝不纳入" else "已自动注入你选择的全部书目、起始章节、打卡要求与每日时长",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = { isPromptExpanded = !isPromptExpanded }
                    ) {
                        Text(if (isPromptExpanded) "收起" else "展开查看", fontSize = 11.sp, color = AcademicPurple)
                        Icon(
                            imageVector = if (isPromptExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AcademicPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Injected Books Chips
                if (materialSummary.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        materialSummary.forEach { (title, note) ->
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = if (note.isNotBlank()) "《$title》· $note" else "《$title》",
                                        fontSize = 10.5.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = AcademicPurple
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                // Strategy Note if any
                if (!customNotesText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = AcademicBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "个性化标注：$customNotesText",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isPromptExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = customizedPrompt,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 推荐使用 DeepSeek-R1 (深度思考) 或 Claude/ChatGPT 网页版",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(customizedPrompt))
                                    Toast.makeText(context, "✓ 已复制提示词！快去发给外部大模型吧", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AcademicPurple),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_copy_prompt_inner")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("一键复制此提示词", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Code Inspector Block
        NotionCard(
            title = "契约 JSON 完整数据流",
            calloutIcon = "💾",
            badgeText = "${jsonString.lines().size} 行"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = jsonString,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

