package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.StudySyncRepository
import com.example.data.entity.ExecutionLogEntity
import com.example.engine.AcademicPlanningEngine
import com.example.engine.GeminiStudyService
import com.example.model.AiModelConfig
import com.example.model.AiProvider
import com.example.model.BookAnalysisReport
import com.example.model.ExamPreset
import com.example.model.ExamPresets
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudySyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudySyncRepository(
        dao = AppDatabase.getDatabase(application).studySyncDao(),
        scope = viewModelScope
    )

    private val prefs = application.getSharedPreferences("studysync_ai_settings", Context.MODE_PRIVATE)

    private val _aiConfig = MutableStateFlow(loadAiConfig())
    val aiConfig: StateFlow<AiModelConfig> = _aiConfig.asStateFlow()

    private val _activeProfileId = MutableStateFlow("Profile_A")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    private val _activeContract = MutableStateFlow<StudySyncContract?>(null)
    val activeContract: StateFlow<StudySyncContract?> = _activeContract.asStateFlow()

    private val _contractA = MutableStateFlow<StudySyncContract?>(null)
    val contractA: StateFlow<StudySyncContract?> = _contractA.asStateFlow()

    private val _contractB = MutableStateFlow<StudySyncContract?>(null)
    val contractB: StateFlow<StudySyncContract?> = _contractB.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<ExecutionLogEntity>>(emptyList())
    val executionLogs: StateFlow<List<ExecutionLogEntity>> = _executionLogs.asStateFlow()

    private val _selectedDay = MutableStateFlow(1)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isGeneratingDay = MutableStateFlow(false)
    val isGeneratingDay: StateFlow<Boolean> = _isGeneratingDay.asStateFlow()

    private val _generatingDayNumber = MutableStateFlow<Int?>(null)
    val generatingDayNumber: StateFlow<Int?> = _generatingDayNumber.asStateFlow()

    private val _progressiveGenProgress = MutableStateFlow<String?>(null)
    val progressiveGenProgress: StateFlow<String?> = _progressiveGenProgress.asStateFlow()

    private val _isAnalyzingBooks = MutableStateFlow(false)
    val isAnalyzingBooks: StateFlow<Boolean> = _isAnalyzingBooks.asStateFlow()

    private val _bookAnalysisReport = MutableStateFlow<BookAnalysisReport?>(null)
    val bookAnalysisReport: StateFlow<BookAnalysisReport?> = _bookAnalysisReport.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _aiLastError = MutableStateFlow<String?>(null)
    val aiLastError: StateFlow<String?> = _aiLastError.asStateFlow()

    fun clearAiLastError() {
        _aiLastError.value = null
    }

    private val _savedRawInputsA = MutableStateFlow<StudyRawInputs?>(loadSavedRawInputs("Profile_A"))
    val savedRawInputsA: StateFlow<StudyRawInputs?> = _savedRawInputsA.asStateFlow()

    private val _savedRawInputsB = MutableStateFlow<StudyRawInputs?>(loadSavedRawInputs("Profile_B"))
    val savedRawInputsB: StateFlow<StudyRawInputs?> = _savedRawInputsB.asStateFlow()

    fun importContract(contract: StudySyncContract) {
        viewModelScope.launch {
            val targetProfile = _activeProfileId.value
            val finalContract = if (contract.profileId != targetProfile) {
                contract.copy(profileId = targetProfile)
            } else {
                contract
            }
            repository.saveContract(finalContract)
            _bookAnalysisReport.value = finalContract.bookAnalysis ?: _bookAnalysisReport.value
            val profileName = if (targetProfile == "Profile_A") "画像 A (本人)" else "画像 B (学友)"
            _statusMessage.value = "✓ 成功导入规划至 $profileName：《${finalContract.planOverview.title}》（共 ${finalContract.dailySchedule.size} 天日程，${finalContract.dailySchedule.sumOf { it.tasks.size }} 个任务）！"
        }
    }

    fun getSavedRawInputs(profileId: String): StudyRawInputs? {
        return if (profileId == "Profile_A") _savedRawInputsA.value else _savedRawInputsB.value
    }

    fun saveRawInputs(profileId: String, inputs: StudyRawInputs) {
        if (profileId == "Profile_A") {
            _savedRawInputsA.value = inputs
        } else {
            _savedRawInputsB.value = inputs
        }
        try {
            val json = com.example.data.StudySyncJsonParser.rawInputsToJson(inputs)
            prefs.edit().putString("saved_raw_inputs_$profileId", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearSavedRawInputs(profileId: String) {
        if (profileId == "Profile_A") {
            _savedRawInputsA.value = null
        } else {
            _savedRawInputsB.value = null
        }
        prefs.edit().remove("saved_raw_inputs_$profileId").apply()
        _statusMessage.value = "已重置备考资料为默认预设。"
    }

    private fun buildCardsFromRawInputs(inputs: StudyRawInputs): List<com.example.model.MaterialCardItem> {
        val result = mutableListOf<com.example.model.MaterialCardItem>()
        var counter = 1
        inputs.books.forEach { b ->
            val isArchived = inputs.archivedCompletedBooks.any { b.contains(it) }
            val status = if (isArchived) "已结项" else "一轮精读"
            result.add(com.example.model.MaterialCardItem(id = "c_${counter++}", title = b, type = "BOOK", status = status, isIncluded = true))
        }
        inputs.problemSets.forEach { p ->
            result.add(com.example.model.MaterialCardItem(id = "c_${counter++}", title = p, type = "EXERCISE", status = "刷题强化", isIncluded = true))
        }
        inputs.videoLectures.forEach { v ->
            result.add(com.example.model.MaterialCardItem(id = "c_${counter++}", title = v, type = "LECTURE", status = "一轮精读", isIncluded = true))
        }
        return result
    }

    private fun loadSavedRawInputs(profileId: String): StudyRawInputs? {
        val json = prefs.getString("saved_raw_inputs_$profileId", null)
        if (json != null) {
            val parsed = com.example.data.StudySyncJsonParser.rawInputsFromJson(json)
            if (parsed != null) {
                if (parsed.materialCards.isEmpty() && (parsed.books.isNotEmpty() || parsed.problemSets.isNotEmpty() || parsed.videoLectures.isNotEmpty())) {
                    val cards = buildCardsFromRawInputs(parsed)
                    val migrated = parsed.copy(materialCards = cards)
                    saveRawInputs(profileId, migrated)
                    return migrated
                }
                return parsed
            }
        }
        val presetIds = listOf("preset_english_one", "preset_cs_408", "preset_math_one", "preset_politics")
        val selected = ExamPresets.presets.filter { it.id in presetIds }
        val merged = if (selected.isNotEmpty()) ExamPresets.mergePresets(selected) else ExamPresets.presets.first().rawInputs
        val cards = buildCardsFromRawInputs(merged)
        val defaultInputs = merged.copy(materialCards = cards)
        try {
            val defaultJson = com.example.data.StudySyncJsonParser.rawInputsToJson(defaultInputs)
            prefs.edit().putString("saved_raw_inputs_$profileId", defaultJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultInputs
    }

    init {
        // Observe Contract A
        viewModelScope.launch {
            repository.getContractFlow("Profile_A").collectLatest { contract ->
                _contractA.value = contract
                if (_activeProfileId.value == "Profile_A") {
                    _activeContract.value = contract
                    _bookAnalysisReport.value = contract?.bookAnalysis ?: _bookAnalysisReport.value
                }
            }
        }

        // Observe Contract B
        viewModelScope.launch {
            repository.getContractFlow("Profile_B").collectLatest { contract ->
                _contractB.value = contract
                if (_activeProfileId.value == "Profile_B") {
                    _activeContract.value = contract
                    _bookAnalysisReport.value = contract?.bookAnalysis ?: _bookAnalysisReport.value
                }
            }
        }

        // Observe Execution Logs for active profile
        viewModelScope.launch {
            _activeProfileId.collectLatest { profileId ->
                repository.getExecutionLogsFlow(profileId).collectLatest { logs ->
                    _executionLogs.value = logs
                }
            }
        }
    }

    private fun loadAiConfig(): AiModelConfig {
        val providerStr = prefs.getString("ai_provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
        val provider = try { AiProvider.valueOf(providerStr) } catch (e: Exception) { AiProvider.GEMINI }
        val loadedModel = prefs.getString("custom_model_name", "deepseek-chat") ?: "deepseek-chat"
        val sanitizedModel = if (loadedModel == "deepseek-v4-pro" || loadedModel.isBlank()) "deepseek-chat" else loadedModel
        val loadedGeminiModel = prefs.getString("gemini_model_name", "gemini-3.6-flash") ?: "gemini-3.6-flash"
        val sanitizedGeminiModel = if (loadedGeminiModel.isBlank() || loadedGeminiModel == "gemini-2.5-flash") "gemini-3.6-flash" else loadedGeminiModel
        return AiModelConfig(
            provider = provider,
            geminiModelName = sanitizedGeminiModel,
            customGeminiApiKey = prefs.getString("custom_gemini_key", "") ?: "",
            customBaseUrl = prefs.getString("custom_base_url", "https://api.deepseek.com/v1") ?: "https://api.deepseek.com/v1",
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            customModelName = sanitizedModel
        )
    }

    fun updateAiConfig(newConfig: AiModelConfig) {
        _aiConfig.value = newConfig
        prefs.edit()
            .putString("ai_provider", newConfig.provider.name)
            .putString("gemini_model_name", newConfig.geminiModelName)
            .putString("custom_gemini_key", newConfig.customGeminiApiKey)
            .putString("custom_base_url", newConfig.customBaseUrl)
            .putString("custom_api_key", newConfig.customApiKey)
            .putString("custom_model_name", newConfig.customModelName)
            .apply()
        _statusMessage.value = "AI 引擎已配置为: ${newConfig.activeDisplayName}"
    }

    suspend fun testAiConnection(config: AiModelConfig): Pair<Boolean, String> {
        return GeminiStudyService.testConnection(config)
    }

    fun switchProfile(profileId: String) {
        _activeProfileId.value = profileId
        val contract = if (profileId == "Profile_A") _contractA.value else _contractB.value
        _activeContract.value = contract
        _bookAnalysisReport.value = contract?.bookAnalysis ?: _bookAnalysisReport.value
        _selectedDay.value = 1
        val profileName = if (profileId == "Profile_A") "画像 A (本人)" else "画像 B (学友)"
        _statusMessage.value = "已切换至 $profileName 独立内存隔离分区。"
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun toggleTaskCompletion(dayNumber: Int, taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(_activeProfileId.value, dayNumber, taskId)
        }
    }

    fun recordExecution(
        dayNumber: Int,
        taskId: String,
        actualMinutes: Int,
        errorRatePercent: Int,
        difficultyFeedback: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordTaskExecution(
                profileId = _activeProfileId.value,
                dayNumber = dayNumber,
                taskId = taskId,
                actualMinutes = actualMinutes,
                errorRatePercent = errorRatePercent,
                difficultyFeedback = difficultyFeedback,
                notes = notes
            )
            _statusMessage.value = "做题执行数据已记录，已触发自适应动态重校！"
        }
    }

    private fun buildInputsFromContract(contract: StudySyncContract): StudyRawInputs {
        val books = contract.bookAnalysis?.booksAnalyzed?.map { it.title } ?: emptyList()
        val calculatedHours = if (contract.dailySchedule.isNotEmpty()) {
            (contract.dailySchedule.firstOrNull()?.allocatedMinutes?.toFloat() ?: 360f) / 60f
        } else {
            6.0f
        }
        return StudyRawInputs(
            examTitle = contract.planOverview.title,
            targetDate = contract.planOverview.targetDate,
            dailyHoursAvailable = calculatedHours,
            books = books,
            customNotes = contract.adaptiveAdjustments?.feedbackSummary ?: ""
        )
    }

    fun triggerAiRecalibration() {
        viewModelScope.launch {
            val current = _activeContract.value ?: return@launch
            _isGenerating.value = true
            try {
                val profileId = _activeProfileId.value
                val logs = _executionLogs.value
                val rawInputs = getSavedRawInputs(profileId) ?: buildInputsFromContract(current)

                val result = GeminiStudyService.recalibratePlanWithAi(
                    profileId = profileId,
                    current = current,
                    rawInputs = rawInputs,
                    logs = logs,
                    config = _aiConfig.value
                )
                repository.saveContract(result.contract)
                _bookAnalysisReport.value = result.contract.bookAnalysis ?: _bookAnalysisReport.value

                if (result.isAiSuccess) {
                    _aiLastError.value = null
                    _statusMessage.value = "✓ 自适应重校完成 (${_aiConfig.value.activeDisplayName})：已根据最新资料与做题日志更新日程与配速！"
                } else {
                    _aiLastError.value = "自适应重校提示 (${result.providerUsed}):\n${result.errorMessage}\n\n已采用内置高精度学术引擎完成自适应安全校准。可在右上角「AI 配置」中输入自备 Key、切换 DeepSeek 或选择内置离线引擎。"
                    _statusMessage.value = "💡 ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _aiLastError.value = "自适应重校执行异常: ${e.localizedMessage ?: e.message}"
                _statusMessage.value = "自适应重校异常: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun analyzeBooks(rawInputs: StudyRawInputs) {
        viewModelScope.launch {
            _isAnalyzingBooks.value = true
            try {
                val profileId = _activeProfileId.value
                saveRawInputs(profileId, rawInputs)
                val report = GeminiStudyService.analyzeBooksWithAi(profileId, rawInputs, _aiConfig.value)
                _bookAnalysisReport.value = report
                
                // If a contract already exists, attach the report and persist
                val current = _activeContract.value
                if (current != null) {
                    val updated = current.copy(bookAnalysis = report)
                    repository.saveContract(updated)
                }
                _statusMessage.value = "AI 教材深度解析完成 (${_aiConfig.value.activeDisplayName})：已测算刷题时间与每日配比！"
            } catch (e: Exception) {
                _statusMessage.value = "教材分析异常: ${e.message}"
            } finally {
                _isAnalyzingBooks.value = false
            }
        }
    }

    fun ingestPlan(rawInputs: StudyRawInputs) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val profileId = _activeProfileId.value
                saveRawInputs(profileId, rawInputs)
                val current = _activeContract.value
                val combinedArchived = (rawInputs.archivedCompletedBooks + (current?.archivedCompletedBooks ?: emptyList())).distinct()
                val finalInputs = rawInputs.copy(archivedCompletedBooks = combinedArchived)

                val result = GeminiStudyService.generatePlanWithAi(profileId, finalInputs, _aiConfig.value)
                repository.saveContract(result.contract)
                _bookAnalysisReport.value = result.contract.bookAnalysis
                _selectedDay.value = 1
                val profileName = if (profileId == "Profile_A") "画像 A (本人)" else "画像 B (学友)"
                val phaseNotice = if (finalInputs.isPhaseAdditionMode || combinedArchived.isNotEmpty()) {
                    "（已自动排除 ${combinedArchived.size} 本已学完书目，阶段新增内容已就绪）"
                } else ""

                if (result.isAiSuccess) {
                    _aiLastError.value = null
                    _statusMessage.value = "✓ 规划生成完成 (${_aiConfig.value.activeDisplayName})，已为 $profileName 生成每日精准任务与战略大纲！$phaseNotice"
                } else {
                    _aiLastError.value = "规划提示 (${result.providerUsed}):\n${result.errorMessage}\n\n系统已启用内置学术规则引擎为您兜底生成了科学排期。可在右上角「AI 配置」中检查 Key、切换 DeepSeek 或选择内置离线引擎。"
                    _statusMessage.value = "💡 ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _aiLastError.value = "规划生成异常: ${e.localizedMessage ?: e.message}"
                _statusMessage.value = "生成规划异常: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Marks an individual book as completed, permanently archiving it from daily scheduling.
     */
    fun markBookCompleted(bookTitle: String) {
        val current = _activeContract.value ?: return
        val clean = com.example.engine.BookLifecycleEngine.cleanBookTitle(bookTitle)
        val updatedArchived = (current.archivedCompletedBooks + clean).distinct()

        // Filter out future uncompleted tasks for this book
        val updatedSchedule = current.dailySchedule.map { day ->
            val filteredTasks = day.tasks.filterNot { task ->
                !task.completed && (task.material.contains(clean) || clean.contains(task.material))
            }
            day.copy(tasks = filteredTasks)
        }

        // Update book analysis items
        val updatedAnalysis = current.bookAnalysis?.let { r ->
            val updatedItems = r.booksAnalyzed.map { item ->
                if (item.title == clean || item.title.contains(clean) || clean.contains(item.title)) {
                    item.copy(isCompleted = true, completedPercent = 100, stageLabel = "已结项归档")
                } else {
                    item
                }
            }
            r.copy(booksAnalyzed = updatedItems)
        }

        val updatedProfiles = current.bookProgressProfiles.map { prof ->
            if (prof.bookTitle == clean) {
                prof.copy(isCompleted = true, completedPercent = 100, stageLabel = "已结项归档")
            } else {
                prof
            }
        }

        val updatedContract = current.copy(
            archivedCompletedBooks = updatedArchived,
            dailySchedule = updatedSchedule,
            bookAnalysis = updatedAnalysis,
            bookProgressProfiles = updatedProfiles,
            stageTransitionNotes = "已将《$clean》全书攻克结项！后续每日任务已自动剔除该书，学习负荷已减轻释放。"
        )

        viewModelScope.launch {
            repository.saveContract(updatedContract)
            _statusMessage.value = "已结项归档《$clean》！该书已从后续每日任务中自动移除，不再重复排期。"
        }
    }

    fun applyPreset(preset: ExamPreset) {
        ingestPlan(preset.rawInputs)
    }

    /**
     * Incrementally invokes AI to generate specific tasks for a single day based on the macro blueprint.
     */
    fun generateDayMission(dayNumber: Int) {
        viewModelScope.launch {
            val current = _activeContract.value ?: return@launch
            val profileId = _activeProfileId.value
            val rawInputs = getSavedRawInputs(profileId) ?: return@launch

            _isGeneratingDay.value = true
            _generatingDayNumber.value = dayNumber
            _progressiveGenProgress.value = "AI 正在深度推演第 $dayNumber 天任务细节..."

            try {
                val res = GeminiStudyService.generateDayMissionWithAi(
                    profileId = profileId,
                    dayNumber = dayNumber,
                    blueprint = current,
                    rawInputs = rawInputs,
                    config = _aiConfig.value
                )

                val updatedSchedule = current.dailySchedule.map { day ->
                    if (day.day == dayNumber) {
                        res.mission
                    } else {
                        day
                    }
                }

                val updatedContract = current.copy(dailySchedule = updatedSchedule)
                repository.saveContract(updatedContract)

                if (res.isSuccess) {
                    _statusMessage.value = "✓ 第 $dayNumber 天任务已由 AI 智能生成就绪！"
                } else {
                    _statusMessage.value = "⚠️ 第 $dayNumber 天任务已生成 (${res.providerUsed})"
                    if (res.errorMessage != null) {
                        _aiLastError.value = res.errorMessage
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "生成第 $dayNumber 天任务异常: ${e.message}"
            } finally {
                _isGeneratingDay.value = false
                _generatingDayNumber.value = null
                _progressiveGenProgress.value = null
            }
        }
    }

    /**
     * Incrementally invokes AI to generate missions for the next batch of unpopulated days.
     */
    fun generateNextDaysBatch(count: Int = 3) {
        viewModelScope.launch {
            val current = _activeContract.value ?: return@launch
            val profileId = _activeProfileId.value
            val rawInputs = getSavedRawInputs(profileId) ?: return@launch

            val daysToGen = current.dailySchedule
                .filter { it.tasks.isEmpty() }
                .take(count)

            if (daysToGen.isEmpty()) {
                _statusMessage.value = "所有日程任务已生成完毕！"
                return@launch
            }

            _isGeneratingDay.value = true

            var workingContract = current
            for (idx in daysToGen.indices) {
                val targetDay = daysToGen[idx]
                _generatingDayNumber.value = targetDay.day
                _progressiveGenProgress.value = "AI 正在连续推演: 第 ${targetDay.day} 天 (${idx + 1}/${daysToGen.size})..."

                val res = GeminiStudyService.generateDayMissionWithAi(
                    profileId = profileId,
                    dayNumber = targetDay.day,
                    blueprint = workingContract,
                    rawInputs = rawInputs,
                    config = _aiConfig.value
                )

                val updatedSchedule = workingContract.dailySchedule.map { day ->
                    if (day.day == targetDay.day) res.mission else day
                }
                workingContract = workingContract.copy(dailySchedule = updatedSchedule)
                repository.saveContract(workingContract)
            }

            _isGeneratingDay.value = false
            _generatingDayNumber.value = null
            _progressiveGenProgress.value = null
            _statusMessage.value = "✓ AI 已成功按天推演生成后续 ${daysToGen.size} 天任务！"
        }
    }
}
