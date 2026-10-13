package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AiModelConfig
import com.example.model.AiProvider
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiConfigDialog(
    currentConfig: AiModelConfig,
    onSaveConfig: (AiModelConfig) -> Unit,
    onTestConnection: suspend (AiModelConfig) -> Pair<Boolean, String>,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var geminiModel by remember { mutableStateOf(currentConfig.geminiModelName) }
    var customGeminiKey by remember { mutableStateOf(currentConfig.customGeminiApiKey) }
    var customBaseUrl by remember { mutableStateOf(currentConfig.customBaseUrl) }
    var customApiKey by remember { mutableStateOf(currentConfig.customApiKey) }
    var customModelName by remember { mutableStateOf(currentConfig.customModelName) }

    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("dialog_ai_config"),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AcademicBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "AI 引擎",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI 模型引擎与 API 配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "支持 Gemini 3.5/3.8 旗舰模型与外部 DeepSeek V4 Pro 导入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Provider Tabs
                    Text(
                        text = "选择规划模型供应源",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProviderPillButton(
                                title = "Google Gemini",
                                subtitle = "内置云端",
                                isSelected = selectedProvider == AiProvider.GEMINI,
                                onClick = {
                                    selectedProvider = AiProvider.GEMINI
                                    testResult = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ProviderPillButton(
                                title = "DeepSeek",
                                subtitle = "外部 API 直连",
                                isSelected = selectedProvider == AiProvider.DEEPSEEK,
                                onClick = {
                                    selectedProvider = AiProvider.DEEPSEEK
                                    if (customBaseUrl.isBlank() || customBaseUrl.contains("generativelanguage")) {
                                        customBaseUrl = "https://api.deepseek.com/v1"
                                    }
                                    if (customModelName.isBlank() || customModelName.contains("gemini") || customModelName == "deepseek-v4-pro") {
                                        customModelName = "deepseek-chat"
                                    }
                                    testResult = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProviderPillButton(
                                title = "OpenAI 兼容",
                                subtitle = "自定义中继/私有",
                                isSelected = selectedProvider == AiProvider.OPENAI_COMPATIBLE,
                                onClick = {
                                    selectedProvider = AiProvider.OPENAI_COMPATIBLE
                                    testResult = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ProviderPillButton(
                                title = "离线学术引擎",
                                subtitle = "免Key·永不限额",
                                isSelected = selectedProvider == AiProvider.OFFLINE_ACADEMIC,
                                onClick = {
                                    selectedProvider = AiProvider.OFFLINE_ACADEMIC
                                    testResult = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Configuration Body based on Provider
                    when (selectedProvider) {
                        AiProvider.OFFLINE_ACADEMIC -> {
                            OfflineAcademicSettingsSection()
                        }
                        AiProvider.GEMINI -> {
                            GeminiSettingsSection(
                                selectedModel = geminiModel,
                                onModelChange = { geminiModel = it },
                                customKey = customGeminiKey,
                                onCustomKeyChange = { customGeminiKey = it }
                            )
                        }
                        AiProvider.DEEPSEEK -> {
                            DeepSeekSettingsSection(
                                baseUrl = customBaseUrl,
                                onBaseUrlChange = { customBaseUrl = it },
                                modelName = customModelName,
                                onModelNameChange = { customModelName = it },
                                apiKey = customApiKey,
                                onApiKeyChange = { customApiKey = it },
                                showApiKey = showApiKey,
                                onToggleShowApiKey = { showApiKey = !showApiKey }
                            )
                        }
                        AiProvider.OPENAI_COMPATIBLE -> {
                            OpenAiSettingsSection(
                                baseUrl = customBaseUrl,
                                onBaseUrlChange = { customBaseUrl = it },
                                modelName = customModelName,
                                onModelNameChange = { customModelName = it },
                                apiKey = customApiKey,
                                onApiKeyChange = { customApiKey = it },
                                showApiKey = showApiKey,
                                onToggleShowApiKey = { showApiKey = !showApiKey }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connection Test Result Pill
                    testResult?.let { (success, msg) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (success) AcademicGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (success) AcademicGreen else MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) AcademicGreen else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bottom Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Connection Button
                    OutlinedButton(
                        onClick = {
                            val candidateConfig = AiModelConfig(
                                provider = selectedProvider,
                                geminiModelName = geminiModel,
                                customGeminiApiKey = customGeminiKey,
                                customBaseUrl = customBaseUrl,
                                customApiKey = customApiKey,
                                customModelName = customModelName
                            )
                            coroutineScope.launch {
                                isTesting = true
                                testResult = null
                                val result = onTestConnection(candidateConfig)
                                isTesting = false
                                testResult = result
                            }
                        },
                        enabled = !isTesting,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("button_test_ai_connection")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AcademicBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("连通性握手...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "测试连接",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("测试连接", fontSize = 12.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("取消", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val newConfig = AiModelConfig(
                                    provider = selectedProvider,
                                    geminiModelName = geminiModel.trim(),
                                    customGeminiApiKey = customGeminiKey.trim(),
                                    customBaseUrl = customBaseUrl.trim(),
                                    customApiKey = customApiKey.trim(),
                                    customModelName = customModelName.trim()
                                )
                                onSaveConfig(newConfig)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("button_save_ai_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存并切换引擎", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderPillButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AcademicBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiSettingsSection(
    selectedModel: String,
    onModelChange: (String) -> Unit,
    customKey: String,
    onCustomKeyChange: (String) -> Unit
) {
    val presetModels = listOf(
        "gemini-3.6-flash" to "思考推理与备考排期首选 (强烈推荐)",
        "gemini-3.8-flash" to "最新旗舰 / 超强长上下文理解",
        "gemini-3.5-flash" to "新一代极速轻量模型",
        "gemini-3.1-pro-preview" to "深度逻辑与复杂多学科统筹",
        "gemini-2.5-flash" to "经典极速备选"
    )

    Column {
        // Zero Config Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AcademicBlueSubtle,
            border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = AcademicBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "云端免密开箱即用",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AcademicBlue
                    )
                    Text(
                        text = "本应用已通过 Google AI Studio 云端托管安全注入 API Key，无需手动创建与付费，直接选择模型即可运行！",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "选择 Gemini 模型版本",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetModels.forEach { (modelId, desc) ->
                val isSel = selectedModel == modelId
                FilterChip(
                    selected = isSel,
                    onClick = { onModelChange(modelId) },
                    label = {
                        Column {
                            Text(modelId, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            Text(desc, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AcademicBlueSubtle,
                        selectedLabelColor = AcademicBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom model input or custom API key override (optional)
        OutlinedTextField(
            value = selectedModel,
            onValueChange = onModelChange,
            label = { Text("当前 Gemini 模型标识 (可自定义如 gemini-3.8-flash)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcademicBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = customKey,
            onValueChange = onCustomKeyChange,
            label = { Text("自定义 Gemini Key (可选，留空则默认使用云端托管 Key)", fontSize = 11.sp) },
            placeholder = { Text("AI Studio 已内置免配置", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcademicBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quota / Rate limit tip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 提示：系统已彻底移除后台联网搜索注入，免费 AI Studio Key 现已可顺畅调用！\n" +
                            "• 强烈推荐选择「gemini-3.6-flash」或「gemini-3.8-flash」，响应极速、推理精准且具备充足免费并发；\n" +
                            "• 若选用 Pro 模型，若未绑定结算账户可能会受每日配额限制；\n" +
                            "• 亦可随时切换至顶部的「离线学术引擎」(零耗额·永不超限) 或「DeepSeek」直接排期！",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun OfflineAcademicSettingsSection() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "本地高精度学术规划引擎 (零 API 配额消耗)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF15803D)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✓ 永不超限：完全离线运行，不消耗任何 Google Gemini 或 DeepSeek 的 Token 额度。\n" +
                       "✓ 科学规划：内置考研多学科协同、艾宾浩斯复习曲线 (D+1, D+3, D+7, D+14)、50%刷题黄金律与考前缓冲窗口。\n" +
                       "✓ 毫秒级响应：日程排期与自适应重校瞬间完成，无网络延迟，绝无 429 报错。\n" +
                       "✓ 完美兜底：即便在线大模型网络波动或限额，学术引擎也能确保备考任务 100% 稳定推进。",
                fontSize = 11.sp,
                color = Color(0xFF166534),
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeepSeekSettingsSection(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit
) {
    val presetDeepSeekModels = listOf(
        "deepseek-chat" to "DeepSeek-V3 核心通用模型 (首选·极速精准)",
        "deepseek-reasoner" to "DeepSeek-R1 深度思考推理模型"
    )

    Column {
        // DeepSeek Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AcademicPurpleSubtle,
            border = BorderStroke(1.dp, AcademicPurple.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = AcademicPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "外部 DeepSeek API 导入",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AcademicPurple
                    )
                    Text(
                        text = "填入您的 DeepSeek API Key 即可直接调用 deepseek-chat 或 deepseek-reasoner 进行计划生成与做题测算。",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // API Key input
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("DeepSeek API Key (必须)", fontSize = 11.sp) },
            placeholder = { Text("sk-xxxxxxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleShowApiKey) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "显隐切换",
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_deepseek_api_key"),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcademicPurple,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Model selector
        Text(
            text = "选择或输入模型版本",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetDeepSeekModels.forEach { (mName, mDesc) ->
                val isSel = modelName == mName
                FilterChip(
                    selected = isSel,
                    onClick = { onModelNameChange(mName) },
                    label = {
                        Column {
                            Text(mName, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            Text(mDesc, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AcademicPurpleSubtle,
                        selectedLabelColor = AcademicPurple
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = modelName,
            onValueChange = onModelNameChange,
            label = { Text("模型名称 (默认 deepseek-chat)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcademicPurple,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Endpoint
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("API 端点 Base URL (默认官方)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcademicPurple,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun OpenAiSettingsSection(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit
) {
    Column {
        Text(
            text = "配置任何遵循 OpenAI /v1/chat/completions 协议的服务端点（如 OneAPI、本地 Ollama/vLLM 中继等）：",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL (例如 https://api.openai.com/v1 或中继地址)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = modelName,
            onValueChange = onModelNameChange,
            label = { Text("模型标识 (例如 gpt-4o, claude-3-7-sonnet, qwen-max 等)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key (Bearer Token)", fontSize = 11.sp) },
            placeholder = { Text("sk-...", fontSize = 11.sp) },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleShowApiKey) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "显隐切换",
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        )
    }
}
