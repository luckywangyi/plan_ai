package com.example.data

import com.example.engine.AcademicBookCatalog
import com.example.model.AdaptiveAdjustments
import com.example.model.ContractTask
import com.example.model.DailyMission
import com.example.model.DashboardComparison
import com.example.model.MilestoneItem
import com.example.model.PlanOverview
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject

object StudySyncJsonParser {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(StudySyncContract::class.java).indent("  ")
    private val rawInputsAdapter = moshi.adapter(StudyRawInputs::class.java).indent("  ")
    private val dailyMissionAdapter = moshi.adapter(DailyMission::class.java).indent("  ")

    fun toJson(contract: StudySyncContract): String {
        return adapter.toJson(contract)
    }

    fun fromJson(json: String): StudySyncContract? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun dailyMissionToJson(mission: DailyMission): String {
        return dailyMissionAdapter.toJson(mission)
    }

    fun dailyMissionFromJson(json: String): DailyMission? {
        return try {
            dailyMissionAdapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rawInputsToJson(rawInputs: StudyRawInputs): String {
        return rawInputsAdapter.toJson(rawInputs)
    }

    fun rawInputsFromJson(json: String): StudyRawInputs? {
        return try {
            rawInputsAdapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Highly tolerant parser that handles markdown code blocks, partial JSON,
     * and external LLM formats (ChatGPT / Claude / DeepSeek / Kimi).
     */
    fun lenientParseContract(rawText: String, fallbackProfileId: String = "Profile_A"): StudySyncContract? {
        if (rawText.isBlank()) return null
        var cleaned = rawText.trim()
        if (cleaned.contains("```json")) {
            cleaned = cleaned.substringAfter("```json").substringBefore("```").trim()
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substringAfter("```").substringBefore("```").trim()
        }
        val firstBrace = cleaned.indexOf('{')
        val lastBrace = cleaned.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1)
        }

        // 1. Try standard adapter first
        val standard = fromJson(cleaned)
        if (standard != null && standard.dailySchedule.isNotEmpty()) {
            return standard
        }

        // 2. Lenient parsing via JSONObject
        return try {
            val root = JSONObject(cleaned)
            val profileId = root.optString("profileId").ifBlank { fallbackProfileId }

            val overviewObj = root.optJSONObject("planOverview")
            val title = overviewObj?.optString("title")
                ?: root.optString("title", "考研备考规划")
            val targetDate = overviewObj?.optString("targetDate")
                ?: root.optString("targetDate", "2026-12-20")
            val totalHours = overviewObj?.optInt("totalEstimatedHours", 300)
                ?: root.optInt("totalEstimatedHours", 300)
            val paceStatus = overviewObj?.optString("currentPaceStatus", "On Track") ?: "On Track"
            val compRate = overviewObj?.optString("completionRate", "0%") ?: "0%"

            val planOverview = PlanOverview(
                title = title,
                targetDate = targetDate,
                totalEstimatedHours = totalHours,
                currentPaceStatus = paceStatus,
                completionRate = compRate
            )

            val scheduleArray = root.optJSONArray("dailySchedule")
                ?: root.optJSONArray("schedule")
                ?: root.optJSONArray("days")
                ?: JSONArray()

            val dailySchedule = mutableListOf<DailyMission>()
            for (i in 0 until scheduleArray.length()) {
                val dayObj = scheduleArray.optJSONObject(i) ?: continue
                val dayNum = dayObj.optInt("day", i + 1)
                val dayDate = dayObj.optString("date", "第 $dayNum 天")
                val coreFocus = dayObj.optString("coreFocus", dayObj.optString("focus", "核心攻坚"))
                val allocMin = dayObj.optInt("allocatedMinutes", 360)
                val phaseLabel = dayObj.optString("phaseLabel", "")
                val phaseBookGoal = dayObj.optString("phaseBookGoal", "")

                val tasksArray = dayObj.optJSONArray("tasks") ?: JSONArray()
                val tasks = mutableListOf<ContractTask>()
                for (j in 0 until tasksArray.length()) {
                    val tObj = tasksArray.optJSONObject(j) ?: continue
                    val tId = tObj.optString("id", "$dayNum-${j + 1}")
                    val mat = tObj.optString("material", tObj.optString("subject", "考研科目"))
                    val scope = tObj.optString("scope", tObj.optString("content", "章节攻坚"))
                    val type = tObj.optString("taskType", "practice")
                    val estMin = tObj.optInt("estimatedMinutes", 60)
                    val completed = tObj.optBoolean("completed", false)

                    tasks.add(
                        ContractTask(
                            id = tId,
                            material = mat,
                            scope = scope,
                            taskType = type,
                            estimatedMinutes = estMin,
                            completed = completed
                        )
                    )
                }

                dailySchedule.add(
                    DailyMission(
                        day = dayNum,
                        date = dayDate,
                        coreFocus = coreFocus,
                        allocatedMinutes = allocMin,
                        tasks = tasks,
                        phaseLabel = phaseLabel,
                        phaseBookGoal = phaseBookGoal
                    )
                )
            }

            if (dailySchedule.isEmpty()) {
                return null
            }

            // Milestones
            val milestones = mutableListOf<MilestoneItem>()
            val msArray = root.optJSONArray("milestones")
            if (msArray != null && msArray.length() > 0) {
                for (k in 0 until msArray.length()) {
                    val mObj = msArray.optJSONObject(k) ?: continue
                    milestones.add(
                        MilestoneItem(
                            stage = mObj.optString("stage", "阶段里程碑"),
                            targetDate = mObj.optString("targetDate", ""),
                            assessmentTopic = mObj.optString("assessmentTopic", "综合自测与错题复查"),
                            completed = mObj.optBoolean("completed", false)
                        )
                    )
                }
            } else {
                val totalDays = dailySchedule.size
                milestones.add(MilestoneItem("25% 阶段性诊断", "第 ${maxOf(1, totalDays / 4)} 天", "第一阶段核心基础排查", false))
                milestones.add(MilestoneItem("50% 强化中继站", "第 ${maxOf(2, totalDays / 2)} 天", "学科难点深度演练", false))
                milestones.add(MilestoneItem("75% 模拟演练点", "第 ${maxOf(3, totalDays * 3 / 4)} 天", "综合成套限时真题测试", false))
                milestones.add(MilestoneItem("100% 决战冲刺", "第 $totalDays 天", "全科错题终极回顾与保温", false))
            }

            val adaptive = AdaptiveAdjustments(
                feedbackSummary = "已成功从外部导入规划契约，结构已校验，可按日高效打卡执行与打印。",
                paceAdjustment = "保持专注节奏，每日任务按时打卡。",
                upcomingChanges = listOf("每日复盘完成后自适应更新进度", "支持随时导出 A4 纸质打卡表")
            )

            val comparison = DashboardComparison(
                profileA_Progress = "0%",
                profileB_Progress = "0%",
                velocityDiff = "待多日打卡测算"
            )

            StudySyncContract(
                profileId = profileId,
                planOverview = planOverview,
                dashboardComparison = comparison,
                milestones = milestones,
                dailySchedule = dailySchedule,
                adaptiveAdjustments = adaptive
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Dynamically generates a customized AI prompt incorporating the user's selected books,
     * progress status, detail notes, study duration, and target exam dates.
     */
    fun buildCustomizedAiPrompt(
        inputs: StudyRawInputs?,
        contract: StudySyncContract? = null,
        targetProfileId: String = "Profile_A"
    ): String {
        val examTitle = inputs?.examTitle?.ifBlank { null }
            ?: contract?.planOverview?.title?.ifBlank { null }
            ?: "2027考研专业课与公共课核心攻坚"

        val targetDate = inputs?.targetDate?.ifBlank { null }
            ?: contract?.planOverview?.targetDate?.ifBlank { null }
            ?: "2026-12-20"

        val dailyHours = inputs?.dailyHoursAvailable?.takeIf { it > 0 }
            ?: (contract?.dailySchedule?.firstOrNull()?.allocatedMinutes?.let { it / 60f } ?: 6.0f)
        val dailyMinutes = (dailyHours * 60).toInt()

        val planDays = contract?.dailySchedule?.size?.takeIf { it > 0 } ?: 14

        // Gather material and book descriptions
        val materialDescriptions = mutableListOf<String>()

        if (inputs != null) {
            val hasCards = inputs.materialCards.isNotEmpty()
            val includedCards = if (hasCards) inputs.materialCards.filter { it.isIncluded } else emptyList()

            if (hasCards) {
                if (includedCards.isNotEmpty()) {
                    includedCards.forEach { card ->
                        val typeLabel = when (card.type) {
                            "BOOK" -> "理论教材"
                            "EXERCISE" -> "题集演算"
                            "LECTURE" -> "网课讲义"
                            else -> "备考资料"
                        }
                        val notePart = if (card.note.isNotBlank()) "【重点标注/要求：${card.note}】" else ""
                        val statusPart = if (card.status.isNotBlank()) "（当前阶段：${card.status}）" else ""
                        val catalogPart = if (card.groundTruthCatalog.isNotBlank()) {
                            "\n   [真实目录/题量基准 Ground Truth]:\n   " + card.groundTruthCatalog.lines().joinToString("\n   ")
                        } else {
                            val autoGt = AcademicBookCatalog.getAutoGeneratedCatalogGroundTruth(card.title)
                            "\n   [学术大纲事实基准 Ground Truth]:\n   " + autoGt.lines().joinToString("\n   ")
                        }
                        materialDescriptions.add("• 📖 $typeLabel：《${card.title}》$statusPart $notePart$catalogPart")
                    }
                } else {
                    materialDescriptions.add("• （提示：当前所有资料均未勾选，请在备考资料库中勾选需要纳入的书目）")
                }
            } else {
                inputs.books.forEach { b ->
                    materialDescriptions.add("• 📘 核心教材：《$b》")
                }
                inputs.problemSets.forEach { p ->
                    materialDescriptions.add("• 📝 习题演算：《$p》")
                }
                inputs.videoLectures.forEach { v ->
                    materialDescriptions.add("• 🎬 视频讲义：《$v》")
                }
            }

            // Progress profiles notes: Strictly filter to books that are actually included/selected!
            val validTitles = if (hasCards) {
                includedCards.map { it.title.trim() }
            } else {
                (inputs.books + inputs.problemSets + inputs.videoLectures).map { it.trim() }
            }

            inputs.bookProgressProfiles
                .filter { profile -> validTitles.any { vt -> vt.isNotEmpty() && (profile.bookTitle.contains(vt) || vt.contains(profile.bookTitle)) } }
                .forEach { profile ->
                    if (profile.naturalLanguageNote.isNotBlank() || profile.completedPercent > 0 || profile.startChapter > 1) {
                        val detail = buildString {
                            append("• 📌 进度与细节标注：《${profile.bookTitle}》")
                            if (profile.completedPercent > 0) append(" 已完成 ${profile.completedPercent}%；")
                            if (profile.startChapter > 1) append(" 从第 ${profile.startChapter} 章开始推进；")
                            if (profile.stageLabel.isNotBlank()) append(" 阶段标签：${profile.stageLabel}；")
                            if (profile.naturalLanguageNote.isNotBlank()) append(" 专属要求：${profile.naturalLanguageNote}；")
                        }
                        materialDescriptions.add(detail)
                    }
                }
        } else if (contract != null) {
            contract.bookAnalysis?.booksAnalyzed?.forEach { book ->
                val tip = if (book.studyTip.isNotBlank()) "【备考策略：${book.studyTip}】" else ""
                materialDescriptions.add("• 📘 ${book.bookType}：《${book.title}》（难度：${book.difficultyLevel}，预计${book.estimatedTotalChapters}章）$tip")
            }
            if (materialDescriptions.isEmpty()) {
                val mats = contract.dailySchedule.flatMap { it.tasks.map { t -> t.material } }.distinct()
                mats.forEach { m ->
                    materialDescriptions.add("• 📘 核心科目资料：《$m》")
                }
            }
        }

        if (materialDescriptions.isEmpty()) {
            materialDescriptions.add("• 📘 核心数学：《张宇高等数学基础30讲》与《1000题》")
            materialDescriptions.add("• 💻 专业课：《王道408数据结构》与课后习题")
            materialDescriptions.add("• 📖 英语：《考研英语红宝书必考词》与历年真题长难句")
        }

        val customNotes = inputs?.customNotes?.ifBlank { null }
            ?: contract?.adaptiveAdjustments?.feedbackSummary?.ifBlank { null }
            ?: "根据个人吸收进度自适应推进，兼顾做题演算与遗忘复盘"

        val materialsText = materialDescriptions.joinToString("\n")

        return """
你是一位深耕中国考研（全国硕士研究生统一招生考试）超过 10 年的资深学术导师与排期专家。
请根据我已选定的参考书目、个性化复习要求与每日可用时长，为我制定一套严谨、可执行、具体到章节和题号的每日任务规划契约。

【我的备考背景与核心要求】
- 考试主题：$examTitle
- 备战目标日期：$targetDate
- 建议规划周期：未来 $planDays 天（第 1 天 至 第 $planDays 天）
- 每日可用复习总时长：${dailyHours} 小时（即每日 ${dailyMinutes} 分钟）

【排期资料范围与严格边界（未勾选书籍绝不纳入！）】
以下【我已选定的参考书目、教材与进度标注】是我本次规划的全部资料范围。
重要禁令：凡未在下方列表中出现的书籍、题集或资料，一律属于未选或已排除资料，绝对不可擅自编排到任何一天的任务中！所有任务的 material 字段必须来自于下方已选清单：
$materialsText

- 我指定的个性化战略备考要求与细节备注：
$customNotes

【排期硬性执行原则】
1. 任务颗粒度极致细化：严禁输出模糊任务（如“高数第一章复习”），必须精确到【书名】+【具体讲数/章节】+【具体题号范围/任务量】（例如：“第1讲 极限与连续 例题1-10推演与知识点精读” 或 “基础篇第1章 极限运算 1-15 题限时演算”）。
2. 任务类型 taskType 规范：只能为 "practice" (做题演算/限时刷题)、"reading" (概念精读/长难句拆分/词汇默写)、"lecture" (视频讲义/难点攻坚) 或 "review" (错题复盘归纳/艾宾浩斯复盘)。
3. 每日时长闭环：每日各任务的 estimatedMinutes 之和必须严格等于每日可用总时长（即 ${dailyMinutes} 分钟）。
4. 严格资料范围限定（未勾选书籍严禁纳入）：所有任务的 material 字段必须精确对应上述已选书目，严禁在日程中编排任何未列出的额外教材或习题。
5. 格式输出规范：不要返回任何客套话或废话，请仅返回单个合法的 JSON 代码块（使用 ```json 与 ``` 包裹）。
6. 必须严格遵循以下 JSON Schema 结构：

```json
{
  "profileId": "$targetProfileId",
  "planOverview": {
    "title": "$examTitle",
    "targetDate": "$targetDate",
    "totalEstimatedHours": ${(planDays * dailyHours).toInt()},
    "currentPaceStatus": "On Track",
    "completionRate": "0%"
  },
  "milestones": [
    {
      "stage": "25% 阶段性诊断",
      "targetDate": "第 ${maxOf(1, planDays / 4)} 天",
      "assessmentTopic": "核心基础概念与典型错题深度排查",
      "completed": false
    },
    {
      "stage": "50% 强化中继站",
      "targetDate": "第 ${maxOf(2, planDays / 2)} 天",
      "assessmentTopic": "阶段性重点题型演练与自测",
      "completed": false
    }
  ],
  "dailySchedule": [
    {
      "day": 1,
      "date": "第 1 天",
      "coreFocus": "首日核心突破模块",
      "allocatedMinutes": $dailyMinutes,
      "phaseLabel": "第一阶段 · 夯实推进",
      "phaseBookGoal": "核心教材第一模块攻坚",
      "tasks": [
        {
          "id": "1-1",
          "material": "核心教材名称",
          "scope": "具体章节与知识点精读",
          "taskType": "reading",
          "estimatedMinutes": ${minOf(120, dailyMinutes / 3)},
          "completed": false
        },
        {
          "id": "1-2",
          "material": "习题集名称",
          "scope": "配套习题 1-15 题限时演算与错题订正",
          "taskType": "practice",
          "estimatedMinutes": ${dailyMinutes - minOf(120, dailyMinutes / 3)},
          "completed": false
        }
      ]
    }
  ]
}
```

请根据上述书目清单、标注细节和可用时间，直接为我输出这 $planDays 天的完整 JSON 代码块！
""".trimIndent()
    }

    /**
     * Complete copyable AI prompt template to send to ChatGPT / Claude / DeepSeek web.
     */
    fun generateAiPromptTemplate(inputs: StudyRawInputs? = null, contract: StudySyncContract? = null): String {
        return buildCustomizedAiPrompt(inputs, contract)
    }

    /**
     * Ready-to-use sample contract JSON for quick preview/testing.
     */
    fun getSampleContractJson(): String {
        return """
{
  "profileId": "Profile_A",
  "planOverview": {
    "title": "2027考研专业课与数英冲刺规划 (外部导入)",
    "targetDate": "2026-12-20",
    "totalEstimatedHours": 320,
    "currentPaceStatus": "On Track",
    "completionRate": "0%"
  },
  "milestones": [
    {
      "stage": "25% 阶段性诊断",
      "targetDate": "第 7 天",
      "assessmentTopic": "第一轮核心基础概念与典型错题排查",
      "completed": false
    },
    {
      "stage": "50% 强化中继站",
      "targetDate": "第 15 天",
      "assessmentTopic": "强化题型攻坚与中等难度真题演练",
      "completed": false
    }
  ],
  "dailySchedule": [
    {
      "day": 1,
      "date": "2026-09-16",
      "coreFocus": "高等数学极限计算与数据结构时间复杂度",
      "allocatedMinutes": 360,
      "phaseLabel": "第 1 月 · 基础夯实",
      "phaseBookGoal": "数学《30讲》与408《数据结构》",
      "tasks": [
        {
          "id": "1-1",
          "material": "张宇高等数学30讲",
          "scope": "第1讲 极限与连续 例题1-12",
          "taskType": "practice",
          "estimatedMinutes": 120,
          "completed": false
        },
        {
          "id": "1-2",
          "material": "王道408数据结构",
          "scope": "第1章 绪论 算法时间与空间复杂度分析",
          "taskType": "reading",
          "estimatedMinutes": 90,
          "completed": false
        },
        {
          "id": "1-3",
          "material": "考研英语红宝书",
          "scope": "Unit 1-2 核心高频词默写与例句精读",
          "taskType": "reading",
          "estimatedMinutes": 60,
          "completed": false
        }
      ]
    },
    {
      "day": 2,
      "date": "2026-09-17",
      "coreFocus": "导数与微分运算 & 线性表顺序存储实战",
      "allocatedMinutes": 360,
      "phaseLabel": "第 1 月 · 基础夯实",
      "phaseBookGoal": "数学《30讲》与408《数据结构》",
      "tasks": [
        {
          "id": "2-1",
          "material": "张宇高等数学30讲",
          "scope": "第2讲 导数与微分 高阶导数习题",
          "taskType": "practice",
          "estimatedMinutes": 120,
          "completed": false
        },
        {
          "id": "2-2",
          "material": "王道408数据结构",
          "scope": "第2章 线性表 顺序表操作代码实现与题库",
          "taskType": "practice",
          "estimatedMinutes": 100,
          "completed": false
        },
        {
          "id": "2-3",
          "material": "考研英语真题",
          "scope": "2015年阅读理解Section A Text 1 长难句翻译",
          "taskType": "reading",
          "estimatedMinutes": 60,
          "completed": false
        }
      ]
    },
    {
      "day": 3,
      "date": "2026-09-18",
      "coreFocus": "链表设计实操与中值定理证明初步",
      "allocatedMinutes": 360,
      "phaseLabel": "第 1 月 · 基础夯实",
      "phaseBookGoal": "数学《30讲》与408《数据结构》",
      "tasks": [
        {
          "id": "3-1",
          "material": "张宇高等数学30讲",
          "scope": "第3讲 一元积分学基本定理与换元法",
          "taskType": "practice",
          "estimatedMinutes": 130,
          "completed": false
        },
        {
          "id": "3-2",
          "material": "王道408数据结构",
          "scope": "单链表与双向循环链表核心算法题",
          "taskType": "practice",
          "estimatedMinutes": 110,
          "completed": false
        }
      ]
    }
  ]
}
""".trimIndent()
    }
}

