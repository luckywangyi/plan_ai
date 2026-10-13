package com.example.engine

import android.util.Log
import com.example.BuildConfig
import com.example.data.StudySyncJsonParser
import com.example.data.entity.ExecutionLogEntity
import com.example.model.AiModelConfig
import com.example.model.AiProvider
import com.example.model.BookAnalysisReport
import com.example.model.ContractTask
import com.example.model.DailyMission
import com.example.model.GroundingSourceItem
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AiServiceResult(
    val contract: StudySyncContract,
    val isAiSuccess: Boolean,
    val providerUsed: String,
    val errorMessage: String? = null
)

object GeminiStudyService {

    private const val TAG = "AiPlanningService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun cleanBaseUrl(url: String): String {
        var u = url.trim()
        while (u.endsWith("/")) {
            u = u.dropLast(1)
        }
        if (u.endsWith("/chat/completions")) {
            u = u.removeSuffix("/chat/completions")
        }
        while (u.endsWith("/")) {
            u = u.dropLast(1)
        }
        return if (u.isBlank()) "https://api.deepseek.com/v1" else u
    }

    fun cleanGeminiModel(model: String): String {
        val m = model.trim().removePrefix("models/")
        return if (m.isBlank() || m == "gemini-2.5-flash") {
            "gemini-3.6-flash"
        } else {
            m
        }
    }

    fun getGeminiCandidateModels(configuredModel: String): List<String> {
        val clean = cleanGeminiModel(configuredModel)
        return listOf(
            clean,
            "gemini-3.6-flash",
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash"
        ).distinct()
    }

    fun cleanApiKey(key: String): String {
        return key.trim().replace("\r", "").replace("\n", "").replace(" ", "")
    }

    fun diagnoseNetworkException(e: Exception, config: AiModelConfig): String {
        val msg = e.message ?: e.localizedMessage ?: ""
        val isTimeout = e is java.net.SocketTimeoutException || msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true)
        return when {
            isTimeout -> "云端大模型生成超时 (>120s): 长文本生成耗时过长，已由本地高精度学术引擎完成全周期精准排期"
            e is java.net.UnknownHostException || msg.contains("Unable to resolve host", ignoreCase = true) -> 
                "网络域名解析失败: 请检查网络连接或 API 地址 (${config.customBaseUrl})"
            e is java.net.ConnectException || msg.contains("Connection refused", ignoreCase = true) -> 
                "无法建立网络连接: 远端服务器拒绝连接，请检查服务地址与网络状态"
            e is javax.net.ssl.SSLException || msg.contains("SSL", ignoreCase = true) || msg.contains("CertPathValidator", ignoreCase = true) -> 
                "SSL 安全握手异常: 请检查设备网络代理或证书设置"
            msg.contains("unexpected end of stream", ignoreCase = true) || msg.contains("Connection reset", ignoreCase = true) || msg.contains("StreamResetException", ignoreCase = true) -> 
                "网络传输中断: 远端服务器断开连接，已由本地学术引擎完成安全排期"
            e is IllegalArgumentException || msg.contains("unexpected url", ignoreCase = true) -> 
                "配置参数格式有误: 请在 AI 配置中检查 Base URL 或 API Key 是否含有无效字符"
            else -> "云端通信受阻 (${e.localizedMessage ?: e.message})，已由本地高精度学术引擎完成科学排期"
        }
    }

    fun sanitizeModelName(config: AiModelConfig): String {
        return when (config.provider) {
            AiProvider.GEMINI -> cleanGeminiModel(config.geminiModelName)
            AiProvider.DEEPSEEK -> {
                val m = config.customModelName.trim()
                if (m.isBlank() || m == "deepseek-v4-pro" || m.contains("gemini", ignoreCase = true)) {
                    "deepseek-chat"
                } else {
                    m
                }
            }
            AiProvider.OPENAI_COMPATIBLE -> config.customModelName.trim().ifBlank { "gpt-4o-mini" }
            AiProvider.OFFLINE_ACADEMIC -> "offline-engine"
        }
    }

    fun isApiKeyAvailable(config: AiModelConfig = AiModelConfig()): Boolean {
        return when (config.provider) {
            AiProvider.GEMINI -> {
                val key = config.customGeminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
                key.isNotBlank() && key != "MY_GEMINI_API_KEY"
            }
            AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> {
                config.customApiKey.isNotBlank()
            }
            AiProvider.OFFLINE_ACADEMIC -> true
        }
    }

    /**
     * Converts raw API errors (like 429 quota exhaustion or invalid keys) into friendly Chinese guidance.
     */
    fun humanizeApiError(code: Int, rawDetail: String?, provider: AiProvider): String {
        val detail = rawDetail ?: ""
        return when {
            code == 429 || detail.contains("quota", ignoreCase = true) || detail.contains("rate-limit", ignoreCase = true) || detail.contains("rate limit", ignoreCase = true) || detail.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || detail.contains("insufficient_quota", ignoreCase = true) -> {
                if (provider == AiProvider.DEEPSEEK) {
                    "DeepSeek 账户余额不足或请求过频 (HTTP 429)。请前往 DeepSeek 开放平台充值，或切换至内置「离线学术引擎」排期。"
                } else {
                    "Gemini API 达到配额限制 (HTTP 429)。建议在 AI 设置中将模型设为推荐的「gemini-3.6-flash」(轻量高频且不占高级额度)，或切换至内置「离线学术引擎」零配额限制直接排期。"
                }
            }
            detail.contains("model", ignoreCase = true) && (detail.contains("not exist", ignoreCase = true) || detail.contains("not found", ignoreCase = true) || detail.contains("invalid", ignoreCase = true)) -> {
                when (provider) {
                    AiProvider.DEEPSEEK -> "模型名称不存在 (HTTP $code): DeepSeek 官方模型为 deepseek-chat 或 deepseek-reasoner，请在右上角 AI 配置中修正模型名称。"
                    AiProvider.GEMINI -> "模型名称不存在或暂不可用 (HTTP $code): 推荐在 AI 配置中选择「gemini-3.6-flash」、「gemini-3.8-flash」或「gemini-3.5-flash」。"
                    else -> "模型名称不存在 (HTTP $code): 请检查您的中继/服务商所支持的模型标识。"
                }
            }
            detail.contains("temperature", ignoreCase = true) -> {
                "参数错误 (HTTP $code): deepseek-reasoner 推理模型不支持设置 temperature 参数，系统已自动优化适配。"
            }
            code == 401 || detail.contains("API_KEY_INVALID", ignoreCase = true) || detail.contains("invalid api key", ignoreCase = true) || detail.contains("Authentication", ignoreCase = true) -> {
                "API Key 鉴权失败 (HTTP $code): 密钥无效或未开通权限，请检查并在 AI 配置中重新填入。"
            }
            code == 403 || detail.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                "权限受限 (HTTP 403): 请检查该 API Key 所在项目是否启用了相关权限，或解除了地区限制。"
            }
            code == 404 || detail.contains("models/", ignoreCase = true) -> {
                if (provider == AiProvider.GEMINI) {
                    "模型或端点未找到 (HTTP 404): 当前配置的 Gemini 模型标识不可用，推荐在 AI 配置中选择「gemini-3.6-flash」或「gemini-3.8-flash」。"
                } else {
                    "模型或端点未找到 (HTTP 404): 请检查 Base URL 与模型名称是否准确。"
                }
            }
            detail.contains("timed out", ignoreCase = true) || detail.contains("timeout", ignoreCase = true) -> {
                "网络通信超时 (>120s): 云端模型处理较慢，已启用本地高精度学术引擎为您完成保底重校。"
            }
            else -> {
                val clean = detail.replace(Regex("https?://\\S+"), "").replace("\n", " ").trim()
                val snippet = if (clean.length > 50) clean.take(50) + "..." else clean
                if (snippet.isNotBlank()) "接口响应异常 (HTTP $code: $snippet)" else "接口响应异常 (HTTP $code)"
            }
        }
    }

    /**
     * Diagnostic ping to verify endpoint reachability and API key authentication.
     */
    suspend fun testConnection(config: AiModelConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            when (config.provider) {
                AiProvider.OFFLINE_ACADEMIC -> {
                    return@withContext true to "内置高精度学术引擎就绪 (无需网络·全规则科学排期·永不超限)"
                }

                AiProvider.GEMINI -> {
                    val rawKey = cleanApiKey(config.customGeminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY })
                    if (rawKey.isBlank() || rawKey == "MY_GEMINI_API_KEY") {
                        return@withContext false to "未检测到有效的 Gemini API Key，请填入自备 Key"
                    }

                    // Candidate fallback models
                    val candidateModels = getGeminiCandidateModels(config.geminiModelName)

                    val pingJson = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().apply {
                                put("text", "Hi")
                            }))
                        }))
                        put("generationConfig", JSONObject().apply {
                            put("maxOutputTokens", 5)
                        })
                    }
                    val mediaType = "application/json; charset=utf-8".toMediaType()

                    var lastCode = 0
                    var lastBody = ""
                    var success = false
                    var connectedModel = candidateModels.first()

                    for (m in candidateModels) {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$rawKey"
                        val req = Request.Builder()
                            .url(url)
                            .post(pingJson.toString().toRequestBody(mediaType))
                            .build()

                        try {
                            val res = client.newCall(req).execute()
                            lastCode = res.code
                            lastBody = res.body?.string() ?: ""
                            if (res.isSuccessful) {
                                success = true
                                connectedModel = m
                                break
                            }
                            // If invalid key or quota exceeded, stop trying other models
                            if (res.code == 400 || res.code == 401 || res.code == 403 || res.code == 429) {
                                break
                            }
                        } catch (e: Exception) {
                            lastBody = e.message ?: "Connection error"
                        }
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    if (success) {
                        return@withContext true to "连接成功! ($connectedModel, 耗时 ${elapsed}ms)"
                    } else {
                        val errObj = try { JSONObject(lastBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val detail = errObj ?: lastBody.ifBlank { "HTTP $lastCode" }
                        val humanMsg = humanizeApiError(lastCode, detail, config.provider)
                        return@withContext false to humanMsg
                    }
                }

                AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> {
                    val apiKey = cleanApiKey(config.customApiKey)
                    if (apiKey.isBlank()) {
                        return@withContext false to "请输入有效的 API Key"
                    }
                    val effectiveModel = sanitizeModelName(config)
                    val isReasoner = effectiveModel.contains("reasoner", ignoreCase = true)
                    val endpoint = "${cleanBaseUrl(config.customBaseUrl)}/chat/completions"
                    val testJson = JSONObject().apply {
                        put("model", effectiveModel)
                        val messages = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "Ping test. Reply with 'OK'.")
                            })
                        }
                        put("messages", messages)
                        put("max_tokens", if (isReasoner) 500 else 60)
                        if (!isReasoner) {
                            put("temperature", 0.3)
                        }
                    }
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(testJson.toString().toRequestBody(mediaType))
                        .build()

                    val res = client.newCall(req).execute()
                    val elapsed = System.currentTimeMillis() - startTime
                    val resBody = res.body?.string() ?: ""
                    if (res.isSuccessful) {
                        val providerName = if (config.provider == AiProvider.DEEPSEEK) "DeepSeek" else "OpenAI"
                        return@withContext true to "连接成功! ($providerName $effectiveModel, 耗时 ${elapsed}ms)"
                    } else {
                        val errObj = try { JSONObject(resBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val detail = errObj ?: resBody.ifBlank { "HTTP ${res.code}" }
                        val humanMsg = humanizeApiError(res.code, detail, config.provider)
                        return@withContext false to humanMsg
                    }
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext false to "网络异常 (${elapsed}ms): ${e.message ?: "无法连接到服务器"}"
        }
    }

    /**
     * Performs standalone AI deep analysis of imported books, workload time estimation, and strategic advice.
     */
    suspend fun analyzeBooksWithAi(
        profileId: String,
        rawInputs: StudyRawInputs,
        config: AiModelConfig = AiModelConfig()
    ): BookAnalysisReport = withContext(Dispatchers.IO) {
        if (config.provider == AiProvider.OFFLINE_ACADEMIC || !isApiKeyAvailable(config)) {
            Log.d(TAG, "Using built-in academic engine for workload analysis.")
            return@withContext AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)
        }

        val resolved = BookLifecycleEngine.resolveInputsWithProgress(rawInputs)
        val knowledgeSection = AcademicBookCatalog.buildAiKnowledgePromptSection(rawInputs.books, resolved.progressProfiles)

        val prompt = """
            You are StudySync AI, an expert exam preparation advisor.
            Analyze the student's imported study books and problem sets. Estimate official exam syllabus, book chapter counts, and problem volume benchmarks where needed.
            
            Exam: ${rawInputs.examTitle}
            Target Deadline: ${rawInputs.targetDate}
            Daily Available Hours: ${rawInputs.dailyHoursAvailable}
            Books / Chapters: ${rawInputs.books.joinToString("; ")}
            Problem Sets: ${rawInputs.problemSets.joinToString("; ")}
            Video Lectures: ${rawInputs.videoLectures.joinToString("; ")}
            Student Strategy Notes: ${rawInputs.customNotes}

            $knowledgeSection
            
            Please generate an accurate BookAnalysisReport JSON object matching this structure:
            ```json
            {
              "booksAnalyzed": [
                {
                  "title": "string",
                  "bookType": "核心教材 / 真题精解 / 习题集 / 讲义",
                  "estimatedTotalChapters": 12,
                  "estimatedQuestionCount": 450,
                  "difficultyLevel": "高难重点 / 核心必考 / 入门筑基",
                  "recommendedReadingMinutesPerChapter": 45,
                  "recommendedMinutesPerProblem": 4.5,
                  "keyExamWeight": "分值占比或重要性说明",
                  "studyTip": "针对该书的高效研读建议"
                }
              ],
              "workloadMetrics": {
                "totalReadingHours": 0.0,
                "totalPracticeHours": 0.0,
                "totalReviewHours": 0.0,
                "totalEstimatedHours": 0.0,
                "dailyHoursRequired": 0.0,
                "dailyHoursAvailable": ${rawInputs.dailyHoursAvailable},
                "paceFeasibility": "负荷适中 / 节奏紧凑 / 存在赤字",
                "paceDeficitOrSurplusHours": 0.0,
                "recommendedDailyRatio": "30% 核心精读 : 50% 刷题内化 : 20% 艾宾浩斯复盘"
              },
              "strategicAdvice": [
                {
                  "category": "刷题战术 / 遗忘防范 / 精力管理 / 考纲动向",
                  "title": "建议标题",
                  "content": "详细指导内容",
                  "priority": "HIGH"
                }
              ],
              "feasibilityAssessment": "总体学习负荷诊断结论"
            }
            ```
        """.trimIndent()

        try {
            when (config.provider) {
                AiProvider.OFFLINE_ACADEMIC -> {
                    return@withContext AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)
                }
                AiProvider.GEMINI -> {
                    val rawKey = cleanApiKey(config.customGeminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY })
                    val candidateModels = getGeminiCandidateModels(config.geminiModelName)

                    val requestJson = JSONObject().apply {
                        val contentsArray = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArray = JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                }
                                put("parts", partsArray)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArray)

                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.3)
                            put("maxOutputTokens", 3500)
                        })
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = requestJson.toString().toRequestBody(mediaType)

                    var responseBody = ""
                    var success = false
                    var parsedReport: BookAnalysisReport? = null
                    var sources = emptyList<GroundingSourceItem>()

                    for (m in candidateModels) {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$rawKey"
                        val request = Request.Builder().url(url).post(body).build()
                        try {
                            val response = client.newCall(request).execute()
                            responseBody = response.body?.string() ?: ""
                            if (response.isSuccessful) {
                                val rootJson = JSONObject(responseBody)
                                val firstCandidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
                                val rawText = extractCandidateText(firstCandidate)
                                sources = extractGroundingSources(firstCandidate)
                                val jsonContent = extractJsonFromMarkdown(rawText)
                                if (jsonContent.isNotBlank()) {
                                    val parsed = StudySyncJsonParser.moshi.adapter(BookAnalysisReport::class.java).fromJson(jsonContent)
                                    if (parsed != null) {
                                        parsedReport = parsed
                                        success = true
                                        break
                                    }
                                }
                            }
                            if (response.code == 400 || response.code == 401 || response.code == 403 || response.code == 429) {
                                break
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Gemini candidate $m failed: ${e.message}")
                        }
                    }

                    val nativeFallback = AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)
                    if (success && parsedReport != null) {
                        val combinedSources = if (sources.isNotEmpty()) sources else nativeFallback.searchGroundingSources
                        return@withContext parsedReport.copy(searchGroundingSources = combinedSources)
                    }
                    return@withContext nativeFallback.copy(searchGroundingSources = if (sources.isNotEmpty()) sources else nativeFallback.searchGroundingSources)
                }

                AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> {
                    val effectiveModel = sanitizeModelName(config)
                    val isReasoner = effectiveModel.contains("reasoner", ignoreCase = true)
                    val endpoint = "${cleanBaseUrl(config.customBaseUrl)}/chat/completions"
                    val apiKey = cleanApiKey(config.customApiKey)

                    val messages = JSONArray().apply {
                        if (isReasoner) {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "[StudySync Book Analysis Instructions]\nReturn ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.\n\n$prompt")
                            })
                        } else {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", "You are StudySync AI, a rigorous academic planning engine. Return ONLY valid JSON inside ```json ... ``` codeblock.")
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                    }

                    val requestJson = JSONObject().apply {
                        put("model", effectiveModel)
                        put("messages", messages)
                        put("max_tokens", if (isReasoner) 4096 else 3500)
                        if (!isReasoner) {
                            put("temperature", 0.3)
                        }
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(requestJson.toString().toRequestBody(mediaType))
                        .build()

                    val response = client.newCall(req).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        Log.w(TAG, "Custom AI endpoint returned ${response.code}: $responseBody")
                        return@withContext AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)
                    }

                    val rootJson = JSONObject(responseBody)
                    val content = rootJson.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                    val jsonContent = extractJsonFromMarkdown(content)
                    val nativeFallback = AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)

                    if (jsonContent.isNotBlank()) {
                        val parsed = StudySyncJsonParser.moshi.adapter(BookAnalysisReport::class.java).fromJson(jsonContent)
                        if (parsed != null) {
                            val modelSource = GroundingSourceItem(
                                title = "${config.provider.title} ($effectiveModel) 深度测算",
                                snippet = "已由 $effectiveModel 模型完成教材章节难度推导与刷题耗时量化"
                            )
                            return@withContext parsed.copy(searchGroundingSources = listOf(modelSource) + nativeFallback.searchGroundingSources)
                        }
                    }
                    return@withContext nativeFallback
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyzeBooksWithAi: ${e.message}", e)
            return@withContext AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)
        }
    }

    /**
     * Calls AI (Gemini or DeepSeek / OpenAI) to generate a structured study plan adhering strictly to the StudySync JSON contract.
     * Falls back to AcademicPlanningEngine if offline or error occurs.
     */
    suspend fun generatePlanWithAi(
        profileId: String,
        rawInputs: StudyRawInputs,
        config: AiModelConfig = AiModelConfig()
    ): AiServiceResult = withContext(Dispatchers.IO) {
        if (config.provider == AiProvider.OFFLINE_ACADEMIC) {
            Log.d(TAG, "Using built-in rigorous academic engine (user selected offline mode).")
            return@withContext AiServiceResult(
                contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                isAiSuccess = true,
                providerUsed = "本地学术规划引擎"
            )
        }

        if (!isApiKeyAvailable(config)) {
            Log.d(TAG, "API key is not configured for ${config.provider}; using built-in rigorous academic engine.")
            return@withContext AiServiceResult(
                contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                isAiSuccess = false,
                providerUsed = "本地学术规划引擎",
                errorMessage = "未检测到已配置的 API Key"
            )
        }

        val resolved = BookLifecycleEngine.resolveInputsWithProgress(rawInputs)
        val knowledgeSection = AcademicBookCatalog.buildAiKnowledgePromptSection(rawInputs.books, resolved.progressProfiles)

        val groundTruthDetails = buildString {
            val cards = rawInputs.materialCards.filter { it.isIncluded }
            if (cards.isNotEmpty()) {
                append("\n=== MATERIAL CARDS GROUND TRUTH (VERIFIED CANONICAL SYLLABUS) ===\n")
                cards.forEach { card ->
                    append("• [${card.type}] 《${card.title}》 | 阶段: ${card.status}")
                    if (card.note.isNotBlank()) append(" | 备注: ${card.note}")
                    if (card.groundTruthCatalog.isNotBlank()) {
                        append("\n  [Ground Truth 真实目录基准]:\n  ")
                        append(card.groundTruthCatalog.lines().joinToString("\n  "))
                    }
                    append("\n")
                }
                append("=================================================================\n")
            }
        }

        val targetDays = calculateDaysBetween(rawInputs.targetDate)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val prompt = """
            You are StudySync AI, a rigorous academic planning engine for graduate entrance exam preparation.
            Ingest these raw study inputs and generate a comprehensive MACRO STRATEGIC BLUEPRINT strictly adhering to the JSON schema:
            
            Profile: $profileId
            Exam Title: ${rawInputs.examTitle}
            Current Start Date (Today): $todayStr
            Target Deadline: ${rawInputs.targetDate}
            TOTAL PLANNING TIMELINE: EXACTLY $targetDays DAYS
            Daily Available Hours: ${rawInputs.dailyHoursAvailable}
            Books / Chapters: ${rawInputs.books.joinToString("; ")}
            Problem Sets: ${rawInputs.problemSets.joinToString("; ")}
            Video Lectures: ${rawInputs.videoLectures.joinToString("; ")}
            Student Strategy Notes: ${rawInputs.customNotes}

            $knowledgeSection
            $groundTruthDetails
            
            CRITICAL TWO-STAGE ARCHITECTURE (MACRO BLUEPRINT FIRST):
            1. MACRO PHASING & MILESTONES:
               - Plan the entire $targetDays days timeline across $todayStr to ${rawInputs.targetDate}.
               - In `milestones`, provide 4 major assessment checkpoints placed at 25%, 50%, 75%, and 100% of the $targetDays-day span.
               - In `monthlyPhases` and `weeklyPhases`, divide the journey into structured academic stages with specific book/subject goals.
            2. PROGRESSIVE DAILY SCHEDULE GENERATION:
               - In `dailySchedule`, provide entries for ALL $targetDays days ($todayStr onwards).
               - DAY 1 (First Day): Provide a COMPLETE, HIGH-FIDELITY task breakdown (`tasks` array with 3-5 multi-subject tasks, including theory reading, problem solving, and spaced review) so the student can start immediately!
               - DAYS 2 to $targetDays: Establish the macro structure for each day (`day`, `date`, `coreFocus`, `allocatedMinutes`, `phaseLabel`, `phaseBookGoal`), but set `tasks`: [] (empty array). The system will invoke AI on-demand Day-by-Day as the student progresses, guaranteeing zero token truncation, maximum accuracy, and adaptive pacing!
            3. MULTI-DISCIPLINARY CO-PLANNING:
               - The student prepares multiple subjects simultaneously (e.g. 408, Math, English, Politics). Allocate balanced daily hours across morning, daytime, and evening slots.
            4. STRICT LEARNING PROGRESS AWARENESS:
               - Respect the student's current learning progress for each book (from the Knowledge Base). Never schedule finished chapters or archived books!
            5. Output ONLY valid JSON inside ```json ... ``` matching this exact schema:
            {
              "profileId": "$profileId",
              "planOverview": {
                "title": "${rawInputs.examTitle} 全期统筹大纲与备考蓝图",
                "targetDate": "${rawInputs.targetDate}",
                "totalEstimatedHours": ${(targetDays * rawInputs.dailyHoursAvailable).toInt()},
                "currentPaceStatus": "按期推进",
                "completionRate": "0%"
              },
              "dashboardComparison": {
                "profileA_Progress": "0%",
                "profileB_Progress": "0%",
                "velocityDiff": "同步基线 (进度差异 0%)"
              },
              "milestones": [
                {
                  "stage": "25% 基础筑基阶段诊断",
                  "targetDate": "YYYY-MM-DD",
                  "assessmentTopic": "基础理论概念与基础例题自测",
                  "completed": false
                },
                {
                  "stage": "50% 核心强化阶段诊断",
                  "targetDate": "YYYY-MM-DD",
                  "assessmentTopic": "各科专项做题突破与算法大题限时训练",
                  "completed": false
                },
                {
                  "stage": "75% 真题套卷提速阶段",
                  "targetDate": "YYYY-MM-DD",
                  "assessmentTopic": "近十年考研真题套卷闭卷全真模拟",
                  "completed": false
                },
                {
                  "stage": "100% 考前绝杀冲刺模考",
                  "targetDate": "${rawInputs.targetDate}",
                  "assessmentTopic": "考前全真模拟测试与核心错题过筛",
                  "completed": false
                }
              ],
              "monthlyPhases": [
                {
                  "phaseId": "phase_1",
                  "phaseType": "MONTH",
                  "phaseNumber": 1,
                  "title": "第 1 阶段 · 基础地毯式扫盲",
                  "timeRange": "第 1-15 天",
                  "coreObjective": "完成各科核心基础精读与基础章节配套习题",
                  "deliverables": ["核心概念树", "基础题错题本"],
                  "isCompleted": false
                }
              ],
              "weeklyPhases": [
                {
                  "phaseId": "week_1",
                  "phaseType": "WEEK",
                  "phaseNumber": 1,
                  "title": "第 1 周 · 基础启动期",
                  "timeRange": "第 1-7 天",
                  "coreObjective": "多学科建立框架，攻克首章核心考点",
                  "deliverables": ["首周知识小结"],
                  "isCompleted": false
                }
              ],
              "bookAnalysis": {
                "booksAnalyzed": [
                  {
                    "title": "书名",
                    "bookType": "核心教材",
                    "estimatedTotalChapters": 10,
                    "estimatedQuestionCount": 350,
                    "difficultyLevel": "核心重点",
                    "recommendedReadingMinutesPerChapter": 60,
                    "recommendedMinutesPerProblem": 4.0,
                    "keyExamWeight": "考研高分核心",
                    "studyTip": "针对性备考建议"
                  }
                ],
                "workloadMetrics": {
                  "totalReadingHours": 30.0,
                  "totalPracticeHours": 50.0,
                  "totalReviewHours": 20.0,
                  "totalEstimatedHours": 100.0,
                  "dailyHoursRequired": ${rawInputs.dailyHoursAvailable},
                  "dailyHoursAvailable": ${rawInputs.dailyHoursAvailable},
                  "paceFeasibility": "负荷适中",
                  "paceDeficitOrSurplusHours": 0.0,
                  "recommendedDailyRatio": "30% 核心精读 : 50% 刷题内化 : 20% 艾宾浩斯复盘"
                },
                "strategicAdvice": [
                  {
                    "category": "刷题战术",
                    "title": "闭卷实战模拟",
                    "content": "建议按限时要求独立完成课后题与真题",
                    "priority": "HIGH"
                  }
                ],
                "feasibilityAssessment": "总体时间负荷适中，各科目可协调推进"
              },
              "dailySchedule": [
                {
                  "day": 1,
                  "date": "$todayStr",
                  "coreFocus": "多学科首日启动与重点章节精读",
                  "allocatedMinutes": ${(rawInputs.dailyHoursAvailable * 60).toInt()},
                  "phaseLabel": "第 1 周 · 基础启动期",
                  "phaseBookGoal": "第一轮核心概念通读",
                  "tasks": [
                    {
                      "id": "d1_t1",
                      "material": "书名",
                      "scope": "第1章 核心概念精读与例题",
                      "taskType": "reading",
                      "estimatedMinutes": 60,
                      "completed": false
                    }
                  ]
                },
                {
                  "day": 2,
                  "date": "YYYY-MM-DD",
                  "coreFocus": "多学科重点概念推进与基础习题演练",
                  "allocatedMinutes": ${(rawInputs.dailyHoursAvailable * 60).toInt()},
                  "phaseLabel": "第 1 周 · 基础启动期",
                  "phaseBookGoal": "基础刷题强化",
                  "tasks": []
                }
              ]
            }
        """.trimIndent()

        try {
            when (config.provider) {
                AiProvider.OFFLINE_ACADEMIC -> {
                    return@withContext AiServiceResult(
                        contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                        isAiSuccess = true,
                        providerUsed = "本地学术规划引擎"
                    )
                }
                AiProvider.GEMINI -> {
                    val rawKey = cleanApiKey(config.customGeminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY })
                    val candidateModels = getGeminiCandidateModels(config.geminiModelName)

                    val requestJson = JSONObject().apply {
                        val contentsArray = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArray = JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                }
                                put("parts", partsArray)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArray)

                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.4)
                            put("maxOutputTokens", 4096)
                        })
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = requestJson.toString().toRequestBody(mediaType)

                    var lastCode = 0
                    var lastBody = ""
                    var success = false
                    var parsedContract: StudySyncContract? = null
                    var usedModel = candidateModels.first()
                    var sources = emptyList<GroundingSourceItem>()

                    for (m in candidateModels) {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$rawKey"
                        val request = Request.Builder().url(url).post(body).build()
                        try {
                            val response = client.newCall(request).execute()
                            lastCode = response.code
                            lastBody = response.body?.string() ?: ""
                            if (response.isSuccessful) {
                                val rootJson = JSONObject(lastBody)
                                val firstCandidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
                                val rawText = extractCandidateText(firstCandidate)
                                sources = extractGroundingSources(firstCandidate)
                                val cleanedJson = extractJsonFromMarkdown(rawText)
                                val parsed = StudySyncJsonParser.fromJson(cleanedJson)
                                if (parsed != null && parsed.dailySchedule.isNotEmpty()) {
                                    parsedContract = parsed
                                    usedModel = m
                                    success = true
                                    break
                                }
                            }
                            if (response.code == 400 || response.code == 401 || response.code == 403 || response.code == 429) {
                                break
                            }
                        } catch (e: Exception) {
                            lastBody = e.message ?: "Connection error"
                        }
                    }

                    if (success && parsedContract != null) {
                        Log.i(TAG, "Successfully generated plan from Gemini ($usedModel), raw schedule size=${parsedContract.dailySchedule.size}, targetDays=$targetDays")
                        val reconciled = reconcileScheduleToTargetDays(parsedContract, profileId, rawInputs, targetDays)
                        val enrichedBookAnalysis = (reconciled.bookAnalysis ?: AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)).let { ba ->
                            if (sources.isNotEmpty()) ba.copy(searchGroundingSources = sources) else ba
                        }
                        return@withContext AiServiceResult(
                            contract = reconciled.copy(bookAnalysis = enrichedBookAnalysis),
                            isAiSuccess = true,
                            providerUsed = "Gemini: $usedModel"
                        )
                    } else {
                        val errObj = try { JSONObject(lastBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val detail = errObj ?: if (lastBody.isNotBlank()) lastBody else "HTTP $lastCode"
                        val humanMsg = humanizeApiError(lastCode, detail, config.provider)
                        Log.w(TAG, "Gemini API failed with $lastCode: $lastBody. Falling back to native engine.")
                        return@withContext AiServiceResult(
                            contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                            isAiSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = humanMsg
                        )
                    }
                }

                AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> {
                    val effectiveModel = sanitizeModelName(config)
                    val isReasoner = effectiveModel.contains("reasoner", ignoreCase = true)
                    val endpoint = "${cleanBaseUrl(config.customBaseUrl)}/chat/completions"
                    val apiKey = cleanApiKey(config.customApiKey)

                    val messages = JSONArray().apply {
                        if (isReasoner) {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "[StudySync Academic Planning Instructions]\nReturn ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.\n\n$prompt")
                            })
                        } else {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", "You are StudySync AI, a rigorous academic planning engine. Return ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.")
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                    }

                    val requestJson = JSONObject().apply {
                        put("model", effectiveModel)
                        put("messages", messages)
                        put("max_tokens", if (isReasoner) 4096 else 3200)
                        if (!isReasoner) {
                            put("temperature", 0.3)
                        }
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(requestJson.toString().toRequestBody(mediaType))
                        .build()

                    val response = client.newCall(req).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        val errObj = try { JSONObject(responseBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val detail = errObj ?: "HTTP ${response.code}"
                        val humanMsg = humanizeApiError(response.code, detail, config.provider)
                        Log.w(TAG, "Custom AI endpoint error ${response.code}: $responseBody")
                        return@withContext AiServiceResult(
                            contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                            isAiSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = humanMsg
                        )
                    }

                    val rootJson = JSONObject(responseBody)
                    val content = rootJson.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                    val cleanedJson = extractJsonFromMarkdown(content)

                    val parsedContract = StudySyncJsonParser.fromJson(cleanedJson)
                    if (parsedContract != null && parsedContract.dailySchedule.isNotEmpty()) {
                        Log.i(TAG, "Successfully generated plan from Custom AI (${config.customModelName}), raw schedule size=${parsedContract.dailySchedule.size}, targetDays=$targetDays")
                        val reconciled = reconcileScheduleToTargetDays(parsedContract, profileId, rawInputs, targetDays)
                        val modelSource = GroundingSourceItem(
                            title = "${config.provider.title} (${config.customModelName})",
                            snippet = "已由外部大语言模型完成自适应日程推导与艾宾浩斯复习编排"
                        )
                        val ba = (reconciled.bookAnalysis ?: AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)).let { b ->
                            b.copy(searchGroundingSources = listOf(modelSource) + b.searchGroundingSources)
                        }
                        return@withContext AiServiceResult(
                            contract = reconciled.copy(bookAnalysis = ba),
                            isAiSuccess = true,
                            providerUsed = config.activeDisplayName
                        )
                    } else {
                        return@withContext AiServiceResult(
                            contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                            isAiSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = "自定义模型返回内容未能成功解析为有效日程合同"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling AI API: ${e.message}. Using native academic engine.", e)
            val errorMsg = diagnoseNetworkException(e, config)
            return@withContext AiServiceResult(
                contract = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs),
                isAiSuccess = false,
                providerUsed = config.activeDisplayName,
                errorMessage = errorMsg
            )
        }
    }

    /**
     * Dynamically recalibrates the plan using AI based on real execution logs and latest raw inputs.
     * Evaluates speed, error rates, and difficulty to adjust pacing, buffer days, and remediation.
     */
    suspend fun recalibratePlanWithAi(
        profileId: String,
        current: StudySyncContract,
        rawInputs: StudyRawInputs,
        logs: List<ExecutionLogEntity>,
        config: AiModelConfig = AiModelConfig()
    ): AiServiceResult = withContext(Dispatchers.IO) {
        val fallbackContract = AcademicPlanningEngine.recalibratePlan(current, logs, rawInputs)

        if (config.provider == AiProvider.OFFLINE_ACADEMIC) {
            Log.d(TAG, "Using built-in academic recalibration engine (offline mode).")
            return@withContext AiServiceResult(
                contract = fallbackContract,
                isAiSuccess = true,
                providerUsed = "本地学术规划引擎"
            )
        }

        if (!isApiKeyAvailable(config)) {
            Log.d(TAG, "API key not configured; using local academic recalibration.")
            return@withContext AiServiceResult(
                contract = fallbackContract,
                isAiSuccess = false,
                providerUsed = "本地学术规划引擎",
                errorMessage = "未配置 API Key，已由本地学术引擎完成自适应重校"
            )
        }

        val recentLogs = logs.take(15)
        val avgErrorRate = if (recentLogs.isNotEmpty()) recentLogs.map { it.errorRatePercent }.average().toInt() else 0
        val timeRatio = if (recentLogs.isNotEmpty() && recentLogs.sumOf { it.expectedMinutes } > 0) {
            String.format(Locale.US, "%.2f", recentLogs.sumOf { it.actualMinutes }.toFloat() / recentLogs.sumOf { it.expectedMinutes })
        } else "1.00"
        val highDiffCount = recentLogs.count { it.difficultyFeedback in listOf("Challenging", "Overwhelming", "偏难", "吃力", "严重超负荷") }

        val includedCards = rawInputs.materialCards.filter { it.isIncluded }
        val materialsSummary = if (includedCards.isNotEmpty()) {
            includedCards.joinToString("\n") { "- [${it.type}] ${it.title} (状态: ${it.status}, 备注: ${it.note})" }
        } else {
            (rawInputs.books + rawInputs.problemSets + rawInputs.videoLectures).joinToString("\n") { "- $it" }
        }

        val targetDays = calculateDaysBetween(rawInputs.targetDate)

        val prompt = """
            You are StudySync AI, an adaptive academic planning engine.
            Perform an ADAPTIVE RECALIBRATION of the student's study plan incorporating their latest materials and real execution logs.
            
            Profile: $profileId
            Exam Title: ${rawInputs.examTitle}
            Hard Target Deadline: ${rawInputs.targetDate}
            TOTAL PLANNING TIMELINE: EXACTLY $targetDays DAYS
            Daily Available Hours: ${rawInputs.dailyHoursAvailable}
            
            Enrolled Study Materials (Latest Snapshot):
            $materialsSummary
            
            Historical Execution Telemetry:
            - Recent Completed Tasks Analyzed: ${recentLogs.size}
            - Recent Average Error Rate: $avgErrorRate%
            - Actual vs Planned Study Time Ratio: $timeRatio
            - High Cognitive Overload / Difficult Tasks Count: $highDiffCount
            - Student Strategy & Instructions: ${rawInputs.customNotes}
            
            RECALIBRATION REQUIREMENTS:
            1. Reflect ALL enrolled study materials into the plan. If new materials were added, schedule them into remaining days.
            2. FULL SCHEDULE LENGTH MANDATE: The plan must span all $targetDays days (day 1 to $targetDays). Do NOT produce an abbreviated 3-day sample.
            3. If error rate is high (>25%), inject targeted diagnostic review tasks and increase solving time allowance.
            4. If actual time ratio > 1.15, redistribute daily loads to strictly fit ${rawInputs.dailyHoursAvailable}h/day and insert buffer windows.
            5. Strictly preserve the target deadline: ${rawInputs.targetDate}.
            6. Return ONLY valid JSON inside ```json ... ``` matching this exact StudySyncContract schema:
            {
              "profileId": "$profileId",
              "planOverview": {
                "title": "${rawInputs.examTitle}",
                "targetDate": "${rawInputs.targetDate}",
                "totalEstimatedHours": 120,
                "currentPaceStatus": "自适应校准就绪",
                "completionRate": "${current.planOverview.completionRate}"
              },
              "dashboardComparison": {
                "profileA_Progress": "${current.dashboardComparison.profileA_Progress}",
                "profileB_Progress": "${current.dashboardComparison.profileB_Progress}",
                "velocityDiff": "${current.dashboardComparison.velocityDiff}"
              },
              "milestones": [
                {
                  "stage": "25% 基础夯实",
                  "targetDate": "${rawInputs.targetDate}",
                  "assessmentTopic": "核心考点综合诊断",
                  "completed": false
                }
              ],
              "dailySchedule": [
                {
                  "day": 1,
                  "date": "${current.dailySchedule.firstOrNull()?.date ?: "2026-09-15"}",
                  "coreFocus": "自适应校准攻坚",
                  "allocatedMinutes": ${(rawInputs.dailyHoursAvailable * 60).toInt()},
                  "phaseLabel": "自适应调校期",
                  "phaseBookGoal": "精准突破弱项",
                  "tasks": [
                    {
                      "id": "T1",
                      "material": "资料名称",
                      "scope": "章节与范围",
                      "taskType": "reading",
                      "estimatedMinutes": 60,
                      "completed": false,
                      "actualMinutes": 0,
                      "errorRatePercent": 0,
                      "difficultyFeedback": "Optimal"
                    }
                  ]
                }
              ],
              "adaptiveAdjustments": {
                "feedbackSummary": "根据近${recentLogs.size}次做题日志(平均错题率$avgErrorRate%)完成自适应校准",
                "paceAdjustment": "每日排期严格契合${rawInputs.dailyHoursAvailable}小时",
                "upcomingChanges": [
                  "针对高错题率章节强化诊断",
                  "动态保障核心复习窗口"
                ]
              }
            }
        """.trimIndent()

        try {
            when (config.provider) {
                AiProvider.OFFLINE_ACADEMIC -> {
                    return@withContext AiServiceResult(
                        contract = fallbackContract,
                        isAiSuccess = true,
                        providerUsed = "本地学术规划引擎"
                    )
                }
                AiProvider.GEMINI -> {
                    val rawKey = cleanApiKey(config.customGeminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY })
                    val candidateModels = getGeminiCandidateModels(config.geminiModelName)

                    val requestJson = JSONObject().apply {
                        val contentsArray = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArray = JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                }
                                put("parts", partsArray)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArray)

                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.3)
                            put("maxOutputTokens", 4096)
                        })
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = requestJson.toString().toRequestBody(mediaType)

                    var lastCode = 0
                    var lastBody = ""
                    var success = false
                    var parsedContract: StudySyncContract? = null
                    var usedModel = candidateModels.first()

                    for (m in candidateModels) {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$rawKey"
                        val request = Request.Builder().url(url).post(body).build()
                        try {
                            val response = client.newCall(request).execute()
                            lastCode = response.code
                            lastBody = response.body?.string() ?: ""
                            if (response.isSuccessful) {
                                val rootJson = JSONObject(lastBody)
                                val firstCandidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
                                val rawText = extractCandidateText(firstCandidate)
                                val cleanedJson = extractJsonFromMarkdown(rawText)
                                val parsed = StudySyncJsonParser.fromJson(cleanedJson)
                                if (parsed != null && parsed.dailySchedule.isNotEmpty()) {
                                    parsedContract = parsed
                                    usedModel = m
                                    success = true
                                    break
                                }
                            }
                            if (response.code == 400 || response.code == 401 || response.code == 403 || response.code == 429) {
                                break
                            }
                        } catch (e: Exception) {
                            lastBody = e.message ?: "Connection error"
                        }
                    }

                    if (success && parsedContract != null) {
                        Log.i(TAG, "Successfully recalibrated plan with Gemini ($usedModel), raw schedule size=${parsedContract.dailySchedule.size}, targetDays=$targetDays")
                        val reconciled = reconcileScheduleToTargetDays(parsedContract, profileId, rawInputs, targetDays)
                        return@withContext AiServiceResult(
                            contract = reconciled,
                            isAiSuccess = true,
                            providerUsed = "Gemini: $usedModel"
                        )
                    } else {
                        val errObj = try { JSONObject(lastBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val detail = errObj ?: if (lastBody.isNotBlank()) lastBody else "HTTP $lastCode"
                        val humanMsg = humanizeApiError(lastCode, detail, config.provider)
                        Log.w(TAG, "Gemini API error during recalibration $lastCode: $lastBody")
                        return@withContext AiServiceResult(
                            contract = fallbackContract,
                            isAiSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = humanMsg
                        )
                    }
                }

                AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> {
                    val effectiveModel = sanitizeModelName(config)
                    val isReasoner = effectiveModel.contains("reasoner", ignoreCase = true)
                    val endpoint = "${cleanBaseUrl(config.customBaseUrl)}/chat/completions"
                    val apiKey = cleanApiKey(config.customApiKey)

                    val messages = JSONArray().apply {
                        if (isReasoner) {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "[StudySync Recalibration Instructions]\nReturn ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.\n\n$prompt")
                            })
                        } else {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", "You are StudySync AI, a rigorous academic planning engine. Return ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.")
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                    }

                    val requestJson = JSONObject().apply {
                        put("model", effectiveModel)
                        put("messages", messages)
                        put("max_tokens", if (isReasoner) 4096 else 3200)
                        if (!isReasoner) {
                            put("temperature", 0.3)
                        }
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(requestJson.toString().toRequestBody(mediaType))
                        .build()

                    val response = client.newCall(req).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        val errObj = try { JSONObject(responseBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val detail = errObj ?: "HTTP ${response.code}"
                        val humanMsg = humanizeApiError(response.code, detail, config.provider)
                        return@withContext AiServiceResult(
                            contract = fallbackContract,
                            isAiSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = humanMsg
                        )
                    }

                    val rootJson = JSONObject(responseBody)
                    val content = rootJson.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                    val cleanedJson = extractJsonFromMarkdown(content)

                    val parsedContract = StudySyncJsonParser.fromJson(cleanedJson)
                    if (parsedContract != null && parsedContract.dailySchedule.isNotEmpty()) {
                        Log.i(TAG, "Successfully recalibrated plan with Custom AI (${config.customModelName}), raw schedule size=${parsedContract.dailySchedule.size}, targetDays=$targetDays")
                        val reconciled = reconcileScheduleToTargetDays(parsedContract, profileId, rawInputs, targetDays)
                        return@withContext AiServiceResult(
                            contract = reconciled,
                            isAiSuccess = true,
                            providerUsed = config.activeDisplayName
                        )
                    } else {
                        return@withContext AiServiceResult(
                            contract = fallbackContract,
                            isAiSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = "自定义 AI 重校返回格式解析异常，已由本地学术引擎完成安全校准"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling AI recalibration: ${e.message}", e)
            val errorMsg = diagnoseNetworkException(e, config)
            return@withContext AiServiceResult(
                contract = fallbackContract,
                isAiSuccess = false,
                providerUsed = config.activeDisplayName,
                errorMessage = errorMsg
            )
        }
    }

    private fun extractJsonFromMarkdown(rawText: String): String {
        val trimmed = rawText.trim()
        val jsonStart = trimmed.indexOf("```json")
        if (jsonStart != -1) {
            val contentStart = jsonStart + 7
            val jsonEnd = trimmed.indexOf("```", contentStart)
            if (jsonEnd != -1) {
                return trimmed.substring(contentStart, jsonEnd).trim()
            }
            return trimmed.substring(contentStart).trim()
        }
        val genericCodeStart = trimmed.indexOf("```")
        if (genericCodeStart != -1) {
            val contentStart = genericCodeStart + 3
            val jsonEnd = trimmed.indexOf("```", contentStart)
            if (jsonEnd != -1) {
                return trimmed.substring(contentStart, jsonEnd).trim()
            }
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    private fun extractGroundingSources(candidate: JSONObject?): List<GroundingSourceItem> {
        val sources = mutableListOf<GroundingSourceItem>()
        if (candidate == null) return sources

        val groundingMetadata = candidate.optJSONObject("groundingMetadata") ?: return sources
        val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
        if (groundingChunks != null) {
            for (i in 0 until groundingChunks.length()) {
                val chunk = groundingChunks.optJSONObject(i)
                val web = chunk?.optJSONObject("web")
                if (web != null) {
                    val uri = web.optString("uri")
                    val title = web.optString("title")
                    if (title.isNotBlank()) {
                        sources.add(GroundingSourceItem(title = title, url = uri))
                    }
                }
            }
        }

        if (sources.isEmpty()) {
            val searchQueries = groundingMetadata.optJSONArray("webSearchQueries")
            if (searchQueries != null) {
                for (i in 0 until searchQueries.length()) {
                    val q = searchQueries.optString(i)
                    if (q.isNotBlank()) {
                        sources.add(GroundingSourceItem(title = "Google 考情联网检索: $q", snippet = "已参考最新全国统考大纲与教辅出版数据"))
                    }
                }
            }
        }

        return sources
    }

    private fun extractCandidateText(candidate: JSONObject?): String {
        val parts = candidate?.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            if (part.optBoolean("thought", false)) continue
            val t = part.optString("text", "")
            if (t.isNotBlank()) sb.append(t)
        }
        if (sb.isEmpty() && parts.length() > 0) {
            return parts.optJSONObject(0)?.optString("text", "") ?: ""
        }
        return sb.toString()
    }

    fun calculateDaysBetween(targetDateStr: String, startDate: Date = Date()): Int {
        if (targetDateStr.isBlank()) return 30
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedTarget = sdf.parse(targetDateStr) ?: return 30
            val startCal = Calendar.getInstance().apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val targetCal = Calendar.getInstance().apply {
                time = parsedTarget
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = targetCal.timeInMillis - startCal.timeInMillis
            val days = Math.round(diff.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()
            maxOf(1, days)
        } catch (e: Exception) {
            30
        }
    }

    /**
     * Reconciles an AI-generated contract to ensure the daily schedule strictly matches the requested target days.
     * If the AI generated fewer days than requested (e.g., generated 3 days when 23 days were requested),
     * this systematically fills the remaining days with the syllabus missions from AcademicPlanningEngine,
     * while preserving all of the AI's intelligent daily missions for the days it did generate.
     */
    /**
     * Reconciles an AI-generated contract to ensure the daily schedule strictly matches the requested target days.
     * Preserves AI's macro phasing, Day 1 tasks, and structural slots without overwriting them with rigid local defaults.
     */
    fun reconcileScheduleToTargetDays(
        parsedContract: StudySyncContract,
        profileId: String,
        rawInputs: StudyRawInputs,
        targetDays: Int
    ): StudySyncContract {
        val currentDays = parsedContract.dailySchedule

        if (currentDays.isEmpty()) {
            val nativePlan = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs)
            return nativePlan.copy(
                bookAnalysis = parsedContract.bookAnalysis ?: nativePlan.bookAnalysis
            )
        }

        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val reconciledSchedule = if (currentDays.size == targetDays) {
            currentDays
        } else if (currentDays.size > targetDays) {
            currentDays.take(targetDays)
        } else {
            // AI returned macro blueprint covering fewer days, expand slots cleanly for day-by-day AI generation
            val combinedList = currentDays.toMutableList()
            val lastKnown = currentDays.lastOrNull()

            for (d in (currentDays.size + 1)..targetDays) {
                val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, d - 1) }
                val dateStr = sdf.format(dayCal.time)
                val pct = (d.toFloat() / targetDays * 100).toInt()
                val phaseLabel = when {
                    pct <= 30 -> "第 1 阶段 · 基础奠基期"
                    pct <= 70 -> "第 2 阶段 · 强化做题期"
                    pct <= 90 -> "第 3 阶段 · 真题突破期"
                    else -> "第 4 阶段 · 冲刺模考期"
                }
                combinedList.add(
                    DailyMission(
                        day = d,
                        date = dateStr,
                        coreFocus = "第 $d 天统筹攻坚 · 点击召唤 AI 深度定制",
                        allocatedMinutes = (rawInputs.dailyHoursAvailable * 60).toInt().coerceAtLeast(60),
                        tasks = emptyList(), // Ready for on-demand day-by-day AI generation!
                        phaseLabel = phaseLabel,
                        phaseBookGoal = lastKnown?.phaseBookGoal ?: "多学科阶段性突破"
                    )
                )
            }
            combinedList
        }

        val totalHours = reconciledSchedule.sumOf { it.allocatedMinutes } / 60

        return parsedContract.copy(
            dailySchedule = reconciledSchedule,
            milestones = if (parsedContract.milestones.isNotEmpty()) parsedContract.milestones else AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs).milestones,
            planOverview = parsedContract.planOverview.copy(
                targetDate = rawInputs.targetDate.ifBlank { parsedContract.planOverview.targetDate },
                totalEstimatedHours = if (totalHours > 0) totalHours else parsedContract.planOverview.totalEstimatedHours
            ),
            bookAnalysis = parsedContract.bookAnalysis ?: AcademicPlanningEngine.analyzeBooksAndWorkload(rawInputs)
        )
    }

    /**
     * Calls AI to generate or recalibrate a single day's mission based on the established macro blueprint.
     * Takes into account the overall plan, previous days' tasks, and target day's focus.
     */
    suspend fun generateDayMissionWithAi(
        profileId: String,
        dayNumber: Int,
        blueprint: StudySyncContract,
        rawInputs: StudyRawInputs,
        config: AiModelConfig = AiModelConfig()
    ): AiDayResult = withContext(Dispatchers.IO) {
        val targetDayItem = blueprint.dailySchedule.firstOrNull { it.day == dayNumber }
        val targetDateStr = targetDayItem?.date ?: run {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayNumber - 1) }
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
        val phaseLabel = targetDayItem?.phaseLabel.orEmpty().ifBlank { "备考攻坚阶段" }
        val phaseBookGoal = targetDayItem?.phaseBookGoal.orEmpty()
        val coreFocus = targetDayItem?.coreFocus.orEmpty().ifBlank { "多学科核心推进与考点突破" }

        val previousMissions = blueprint.dailySchedule.filter { it.day < dayNumber && it.tasks.isNotEmpty() }.takeLast(2)
        val prevContext = if (previousMissions.isNotEmpty()) {
            "Previous Days Tasks:\n" + previousMissions.joinToString("\n") { dm ->
                "- Day ${dm.day} (${dm.coreFocus}): " + dm.tasks.joinToString("; ") { "${it.material} ${it.scope}" }
            }
        } else {
            "Starting phase of the study plan."
        }

        val availableMinutes = (rawInputs.dailyHoursAvailable * 60).toInt().coerceAtLeast(60)

        val prompt = """
            You are StudySync AI, a rigorous graduate entrance exam academic coach.
            Generate a precise, realistic, and high-impact DAILY STUDY MISSION for Day $dayNumber ($targetDateStr).
            
            Exam: ${rawInputs.examTitle}
            Target Books & Materials: ${rawInputs.books.joinToString("; ")}
            Problem Sets: ${rawInputs.problemSets.joinToString("; ")}
            Daily Study Time Budget: $availableMinutes minutes (${rawInputs.dailyHoursAvailable} hours)
            Phase: $phaseLabel
            Stage Book Goal: $phaseBookGoal
            Today's Core Focus: $coreFocus
            $prevContext
            
            REQUIREMENTS:
            1. If the student has multiple subjects (e.g. 408, Math, English, Politics), generate 3 to 5 balanced tasks covering morning, afternoon, and evening slots.
            2. Total estimatedMinutes of all tasks should sum up to approximately $availableMinutes minutes.
            3. Task types must be one of: "reading", "practice", "lecture", "review". Include specific authentic chapter numbers, page ranges, or problem counts.
            4. Include 1 spaced review / recap task (e.g. formula/flashcard review).
            5. Return ONLY a single JSON object inside ```json ... ``` matching this exact DailyMission schema:
            {
              "day": $dayNumber,
              "date": "$targetDateStr",
              "coreFocus": "$coreFocus",
              "allocatedMinutes": $availableMinutes,
              "phaseLabel": "$phaseLabel",
              "phaseBookGoal": "$phaseBookGoal",
              "tasks": [
                {
                  "id": "d${dayNumber}_t1",
                  "material": "书名或题集名",
                  "scope": "具体章节或题号范围",
                  "taskType": "reading",
                  "estimatedMinutes": 60,
                  "completed": false
                },
                {
                  "id": "d${dayNumber}_t2",
                  "material": "习题集名",
                  "scope": "专项做题 20道",
                  "taskType": "practice",
                  "estimatedMinutes": 90,
                  "completed": false
                },
                {
                  "id": "d${dayNumber}_t3",
                  "material": "核心笔记",
                  "scope": "艾宾浩斯复习与错题过筛",
                  "taskType": "review",
                  "estimatedMinutes": 30,
                  "completed": false
                }
              ]
            }
        """.trimIndent()

        if (config.provider == AiProvider.OFFLINE_ACADEMIC || !isApiKeyAvailable(config)) {
            val fallback = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs)
            val day = fallback.dailySchedule.firstOrNull { it.day == dayNumber }
                ?: fallback.dailySchedule.getOrNull(dayNumber - 1)
                ?: DailyMission(
                    day = dayNumber,
                    date = targetDateStr,
                    coreFocus = coreFocus,
                    allocatedMinutes = availableMinutes,
                    tasks = listOf(
                        ContractTask(
                            id = "d${dayNumber}_t1",
                            material = rawInputs.books.firstOrNull() ?: "考研科目核心教材",
                            scope = "第$dayNumber 单元精读与例题",
                            taskType = "reading",
                            estimatedMinutes = availableMinutes
                        )
                    ),
                    phaseLabel = phaseLabel,
                    phaseBookGoal = phaseBookGoal
                )
            return@withContext AiDayResult(
                mission = day.copy(day = dayNumber, date = targetDateStr),
                isSuccess = true,
                providerUsed = "本地学术引擎"
            )
        }

        try {
            when (config.provider) {
                AiProvider.GEMINI -> {
                    val rawKey = cleanApiKey(config.customGeminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY })
                    val candidateModels = getGeminiCandidateModels(config.geminiModelName)

                    val requestJson = JSONObject().apply {
                        val contentsArray = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArray = JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                }
                                put("parts", partsArray)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArray)
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.3)
                            put("maxOutputTokens", 1500)
                        })
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = requestJson.toString().toRequestBody(mediaType)

                    var lastCode = 0
                    var lastBody = ""
                    var parsedMission: DailyMission? = null

                    for (m in candidateModels) {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$rawKey"
                        val request = Request.Builder().url(url).post(body).build()
                        try {
                            val response = client.newCall(request).execute()
                            lastCode = response.code
                            lastBody = response.body?.string() ?: ""
                            if (response.isSuccessful) {
                                val rootJson = JSONObject(lastBody)
                                val firstCandidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
                                val rawText = extractCandidateText(firstCandidate)
                                val cleanedJson = extractJsonFromMarkdown(rawText)
                                val mission = StudySyncJsonParser.dailyMissionFromJson(cleanedJson)
                                if (mission != null && mission.tasks.isNotEmpty()) {
                                    parsedMission = mission
                                    break
                                }
                            }
                            if (response.code in listOf(400, 401, 403, 429)) break
                        } catch (e: Exception) {
                            lastBody = e.message ?: "Network error"
                        }
                    }

                    if (parsedMission != null) {
                        return@withContext AiDayResult(
                            mission = parsedMission,
                            isSuccess = true,
                            providerUsed = config.activeDisplayName
                        )
                    } else {
                        val fallback = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs)
                            .dailySchedule.firstOrNull { it.day == dayNumber }
                        return@withContext AiDayResult(
                            mission = fallback?.copy(day = dayNumber, date = targetDateStr) ?: targetDayItem!!,
                            isSuccess = false,
                            providerUsed = config.activeDisplayName,
                            errorMessage = humanizeApiError(lastCode, lastBody, config.provider)
                        )
                    }
                }
                AiProvider.DEEPSEEK, AiProvider.OPENAI_COMPATIBLE -> {
                    val effectiveModel = sanitizeModelName(config)
                    val isReasoner = effectiveModel.contains("reasoner", ignoreCase = true)
                    val endpoint = "${cleanBaseUrl(config.customBaseUrl)}/chat/completions"
                    val apiKey = cleanApiKey(config.customApiKey)

                    val messages = JSONArray().apply {
                        if (isReasoner) {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "[StudySync Day Mission Instructions]\nReturn ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.\n\n$prompt")
                            })
                        } else {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", "You are StudySync AI, a rigorous academic planning engine. Return ONLY valid JSON matching the requested schema inside ```json ... ``` codeblock.")
                            })
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                    }

                    val requestJson = JSONObject().apply {
                        put("model", effectiveModel)
                        put("messages", messages)
                        put("max_tokens", 2048)
                        if (!isReasoner) put("temperature", 0.3)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(requestJson.toString().toRequestBody(mediaType))
                        .build()

                    val response = client.newCall(req).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val rootJson = JSONObject(responseBody)
                        val content = rootJson.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                        val cleanedJson = extractJsonFromMarkdown(content)
                        val mission = StudySyncJsonParser.dailyMissionFromJson(cleanedJson)
                        if (mission != null && mission.tasks.isNotEmpty()) {
                            return@withContext AiDayResult(
                                mission = mission,
                                isSuccess = true,
                                providerUsed = config.activeDisplayName
                            )
                        }
                    }

                    val fallback = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs)
                        .dailySchedule.firstOrNull { it.day == dayNumber }
                    return@withContext AiDayResult(
                        mission = fallback?.copy(day = dayNumber, date = targetDateStr) ?: targetDayItem!!,
                        isSuccess = false,
                        providerUsed = config.activeDisplayName,
                        errorMessage = humanizeApiError(response.code, responseBody, config.provider)
                    )
                }
                else -> {
                    val fallback = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs)
                        .dailySchedule.firstOrNull { it.day == dayNumber }
                    return@withContext AiDayResult(
                        mission = fallback?.copy(day = dayNumber, date = targetDateStr) ?: targetDayItem!!,
                        isSuccess = true,
                        providerUsed = "本地学术引擎"
                    )
                }
            }
        } catch (e: Exception) {
            val fallback = AcademicPlanningEngine.generateRigorousPlan(profileId, rawInputs)
                .dailySchedule.firstOrNull { it.day == dayNumber }
            return@withContext AiDayResult(
                mission = fallback?.copy(day = dayNumber, date = targetDateStr) ?: targetDayItem!!,
                isSuccess = false,
                providerUsed = config.activeDisplayName,
                errorMessage = diagnoseNetworkException(e, config)
            )
        }
    }
}

data class AiDayResult(
    val mission: DailyMission,
    val isSuccess: Boolean,
    val providerUsed: String,
    val errorMessage: String? = null
)
