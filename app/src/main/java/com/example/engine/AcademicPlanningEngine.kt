package com.example.engine

import com.example.model.AdaptiveAdjustments
import com.example.model.AnalyzedBookItem
import com.example.model.BookAnalysisReport
import com.example.model.ContractTask
import com.example.model.DailyMission
import com.example.model.DashboardComparison
import com.example.model.GroundingSourceItem
import com.example.model.MilestoneItem
import com.example.model.PlanOverview
import com.example.model.StrategicAdviceItem
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import com.example.model.WorkloadMetrics
import com.example.data.entity.ExecutionLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

object AcademicPlanningEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun generateRigorousPlan(
        profileId: String,
        rawInputs: StudyRawInputs,
        startDate: Date = Date()
    ): StudySyncContract {
        val calendar = Calendar.getInstance().apply { time = startDate }
        val targetCalendar = Calendar.getInstance()

        val targetDateStr: String
        val totalDays: Int

        if (rawInputs.targetDate.isNotBlank()) {
            val parsedTarget = try {
                dateFormat.parse(rawInputs.targetDate)
            } catch (e: Exception) {
                null
            }
            if (parsedTarget != null) {
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
                val diffMillis = targetCal.timeInMillis - startCal.timeInMillis
                val calculatedDays = Math.round(diffMillis.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()
                totalDays = maxOf(1, calculatedDays)
                targetDateStr = rawInputs.targetDate
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 42)
                targetDateStr = dateFormat.format(calendar.time)
                calendar.time = startDate
                totalDays = 42
            }
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 42)
            targetDateStr = dateFormat.format(calendar.time)
            calendar.time = startDate
            totalDays = 42
        }

        // Milestone checkpoints at exactly 25%, 50%, 75%, and 100%
        val m25Day = maxOf(1, (totalDays * 0.25).toInt().coerceAtMost(totalDays))
        val m50Day = maxOf(1, (totalDays * 0.50).toInt().coerceAtMost(totalDays))
        val m75Day = maxOf(1, (totalDays * 0.75).toInt().coerceAtMost(totalDays))
        val m100Day = totalDays

        fun getDateForDay(dayOffset: Int): String {
            val c = Calendar.getInstance().apply { time = startDate }
            c.add(Calendar.DAY_OF_YEAR, dayOffset - 1)
            return dateFormat.format(c.time)
        }

        val milestones = listOf(
            MilestoneItem(
                stage = "25% 基础筑基诊断",
                targetDate = getDateForDay(m25Day),
                assessmentTopic = "核心考点梳理与基线摸底诊断测验",
                completed = false
            ),
            MilestoneItem(
                stage = "50% 半程强化评估",
                targetDate = getDateForDay(m50Day),
                assessmentTopic = "全真限时题型测试与知识盲区排查",
                completed = false
            ),
            MilestoneItem(
                stage = "75% 高阶综合冲刺",
                targetDate = getDateForDay(m75Day),
                assessmentTopic = "高难度跨章节综合题与解题速度突破",
                completed = false
            ),
            MilestoneItem(
                stage = "100% 终极模拟全真考场",
                targetDate = getDateForDay(m100Day),
                assessmentTopic = "严格考场环境全真模考与终极复盘归纳",
                completed = false
            )
        )

        // Build Daily Missions with granular time breakdown, Spaced Repetition, and Buffer Days
        val dailySchedule = mutableListOf<DailyMission>()
        // Resolve book progress, stage sprints, and eliminate completed/archived books
        val resolved = BookLifecycleEngine.resolveInputsWithProgress(rawInputs)
        val booksList = if (resolved.activeBooks.isNotEmpty()) resolved.activeBooks else listOf("考研统考核心教材与讲义", "高阶综合真题解密")
        val problemsList = if (resolved.activeProblemSets.isNotEmpty()) resolved.activeProblemSets else listOf("核心考点限时强化题集 (300题)")
        val lecturesList = if (rawInputs.videoLectures.isNotEmpty()) rawInputs.videoLectures else listOf("高频重难点网课精讲")

        // Partition materials by subject tracks if multiple subjects exist
        val subjectTracks = partitionIntoSubjectTracks(booksList, problemsList, lecturesList)
        val isMultiSubject = subjectTracks.size >= 2
        val targetDailyMinutes = (rawInputs.dailyHoursAvailable * 60).toInt().coerceIn(90, 660)

        // Generate Stage-Based Monthly & Weekly Book Clearance Milestones
        val monthlyPhases = generateMonthlyPhases(totalDays, startDate, booksList, problemsList, rawInputs.parsedMaterials)
        val weeklyPhases = generateWeeklyPhases(totalDays, startDate, booksList, problemsList, rawInputs.parsedMaterials)

        var bookIndex = 0
        var problemIndex = 0
        var lectureIndex = 0

        for (day in 1..totalDays) {
            val dayDate = getDateForDay(day)
            val tasks = mutableListOf<ContractTask>()

            // Compute current week and month context for phase-level book clearance tracking
            val currentWeekNum = ((day - 1) / 7) + 1
            val currentMonthNum = ((day - 1) / 30) + 1
            val phaseLabel = "第 ${currentMonthNum} 月 · 第 ${currentWeekNum} 周"

            val currentWeekPhase = weeklyPhases.getOrNull(currentWeekNum - 1)
            val phaseBookGoal = currentWeekPhase?.let { w ->
                val booksShort = w.targetBooks.take(2).joinToString(" & ") { b ->
                    val cleanTitle = b.bookTitle.substringBefore("(").trim()
                    "$cleanTitle: ${b.targetScope.substringBefore("，")}"
                }
                "本周结项: $booksShort"
            } ?: "阶段书目结项推进中"

            val isMilestone25 = (day == m25Day)
            val isMilestone50 = (day == m50Day)
            val isMilestone75 = (day == m75Day)
            val isMilestone100 = (day == m100Day)
            val isBufferDay = (day == m25Day - 1 || day == m50Day - 1 || day == m75Day - 1 || day == m100Day - 1) && day > 2

            val coreFocus: String

            if (isMilestone100) {
                coreFocus = "里程碑: 100% 终极全真模拟考场与考前能力核验"
                if (isMultiSubject) {
                    subjectTracks.forEach { track ->
                        val prob = track.problemSets.firstOrNull() ?: track.books.firstOrNull() ?: rawInputs.examTitle
                        tasks.add(
                            ContractTask(
                                id = UUID.randomUUID().toString().take(8),
                                material = "[${track.name}] 终极全真模拟",
                                scope = "严格实战考场限时环境下全真套卷模拟考核",
                                taskType = "practice",
                                estimatedMinutes = (180 / subjectTracks.size).coerceAtLeast(45),
                                completed = false
                            )
                        )
                    }
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "全科考后深度错题复盘归纳",
                            scope = "逐题剖析错题成因与各科最后知识盲区紧急修补",
                            taskType = "review",
                            estimatedMinutes = 60,
                            completed = false
                        )
                    )
                } else {
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = rawInputs.examTitle,
                            scope = "严格实战考场限时环境下全真模拟考核",
                            taskType = "practice",
                            estimatedMinutes = 210,
                            completed = false
                        )
                    )
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "考后深度错题复盘归纳",
                            scope = "逐题剖析错题成因与最终知识漏洞紧急修补",
                            taskType = "review",
                            estimatedMinutes = 60,
                            completed = false
                        )
                    )
                }
            } else if (isMilestone50) {
                coreFocus = "里程碑: 50% 半程基准测试与限时专项诊断"
                if (isMultiSubject) {
                    subjectTracks.forEach { track ->
                        val prob = track.problemSets.firstOrNull() ?: track.books.firstOrNull() ?: "半程限时套题"
                        tasks.add(
                            ContractTask(
                                id = UUID.randomUUID().toString().take(8),
                                material = "[${track.name}] 半程强化评估",
                                scope = "核心题型与大题限时摸底诊断测试",
                                taskType = "practice",
                                estimatedMinutes = (160 / subjectTracks.size).coerceAtLeast(35),
                                completed = false
                            )
                        )
                    }
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "多学科盲审复盘与薄弱点诊断",
                            scope = "全面复盘各科命题陷阱与做题耗时瓶颈",
                            taskType = "review",
                            estimatedMinutes = 45,
                            completed = false
                        )
                    )
                } else {
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = problemsList[problemIndex % problemsList.size],
                            scope = "半程强化限时套题综合诊断训练",
                            taskType = "practice",
                            estimatedMinutes = 120,
                            completed = false
                        )
                    )
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "盲审复盘与薄弱点诊断",
                            scope = "全面复盘题目陷阱与解题耗时瓶颈",
                            taskType = "review",
                            estimatedMinutes = 60,
                            completed = false
                        )
                    )
                }
            } else if (isMilestone25) {
                coreFocus = "里程碑: 25% 基础夯实阶段评估测验"
                if (isMultiSubject) {
                    subjectTracks.forEach { track ->
                        val bk = track.books.firstOrNull() ?: "核心考点"
                        tasks.add(
                            ContractTask(
                                id = UUID.randomUUID().toString().take(8),
                                material = "[${track.name}] 基础夯实检测",
                                scope = "核心概念掌握度与基础题限时摸底",
                                taskType = "practice",
                                estimatedMinutes = (140 / subjectTracks.size).coerceAtLeast(30),
                                completed = false
                            )
                        )
                    }
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "跨学科基础理论梳理",
                            scope = "整理基础公式与概念盲区清单",
                            taskType = "review",
                            estimatedMinutes = 40,
                            completed = false
                        )
                    )
                } else {
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = booksList[bookIndex % booksList.size],
                            scope = "基础概念掌握度检验: 40道限时专项诊断题",
                            taskType = "practice",
                            estimatedMinutes = 90,
                            completed = false
                        )
                    )
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "核心理论与公式梳理",
                            scope = "回顾核心公式、定理推导与未完模块补全",
                            taskType = "review",
                            estimatedMinutes = 45,
                            completed = false
                        )
                    )
                }
            } else if (isMilestone75) {
                coreFocus = "里程碑: 75% 高阶综合串联与抗压能力实训"
                if (isMultiSubject) {
                    subjectTracks.forEach { track ->
                        val prob = track.problemSets.lastOrNull() ?: track.books.firstOrNull() ?: "综合题"
                        tasks.add(
                            ContractTask(
                                id = UUID.randomUUID().toString().take(8),
                                material = "[${track.name}] 压轴综合冲刺",
                                scope = "高难度跨章节真题与压轴题攻坚演练",
                                taskType = "practice",
                                estimatedMinutes = (160 / subjectTracks.size).coerceAtLeast(40),
                                completed = false
                            )
                        )
                    }
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "全科元认知复盘与提速优化",
                            scope = "错题率动态再校准与考场策略优化",
                            taskType = "review",
                            estimatedMinutes = 45,
                            completed = false
                        )
                    )
                } else {
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = problemsList[problemIndex % problemsList.size],
                            scope = "高难度跨章节交叉综合题组 (压轴题突破)",
                            taskType = "practice",
                            estimatedMinutes = 140,
                            completed = false
                        )
                    )
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "元认知复盘与提速策略",
                            scope = "错题率动态再校准与做题节奏战术优化",
                            taskType = "review",
                            estimatedMinutes = 50,
                            completed = false
                        )
                    )
                }
            } else if (isBufferDay) {
                coreFocus = "自适应缓冲调整与薄弱点强化日"
                tasks.add(
                    ContractTask(
                        id = UUID.randomUUID().toString().take(8),
                        material = if (isMultiSubject) "各学科历史错题沉淀本" else "历史错题沉淀本",
                        scope = "重做过去7天所有科目错题并核实根本成因",
                        taskType = "review",
                        estimatedMinutes = 60,
                        completed = false
                    )
                )
                tasks.add(
                    ContractTask(
                        id = UUID.randomUUID().toString().take(8),
                        material = "艾宾浩斯多学科复习矩阵",
                        scope = "全科高频考点闪卡主动回忆训练与核心记忆固化",
                        taskType = "review",
                        estimatedMinutes = 45,
                        completed = false
                    )
                )
            } else {
                // Regular Mission Day
                if (isMultiSubject) {
                    // MULTI-SUBJECT SYNTHESIZED DISPATCH: Include every enrolled subject in this day's mission
                    coreFocus = "第 $day 天多学科协同推进: " + subjectTracks.joinToString(" + ") { it.name }
                    val reviewMins = if (day % 7 == 0) 40 else 25
                    val remainingMins = (targetDailyMinutes - reviewMins).coerceAtLeast(60 * subjectTracks.size)
                    val baseMinsPerSubject = remainingMins / subjectTracks.size

                    // Sort tracks by academic rhythm: Language/Memory morning -> Logic/Math midday -> Practice afternoon -> Evening review
                    val sortedTracks = subjectTracks.sortedBy { it.prioritySlot }

                    sortedTracks.forEachIndexed { idx, track ->
                        // Determine books that are active on this day (respecting target sprint days e.g. 15-day target)
                        val activeTrackBooks = track.books.filter { b ->
                            val clean = BookLifecycleEngine.cleanBookTitle(b)
                            val prof = resolved.progressProfiles.firstOrNull { it.bookTitle == clean }
                            if (prof?.targetFinishDays != null) day <= prof.targetFinishDays else true
                        }

                        val allocatedForSubject = if (idx == sortedTracks.lastIndex) {
                            remainingMins - (baseMinsPerSubject * (sortedTracks.size - 1))
                        } else {
                            baseMinsPerSubject
                        }

                        if (activeTrackBooks.isEmpty() && track.books.isNotEmpty()) {
                            // Target reached (e.g. 15-day sprint finished) -> slot released for Phase 2 additions!
                            val cleanFirst = BookLifecycleEngine.cleanBookTitle(track.books.first())
                            val prof = resolved.progressProfiles.firstOrNull { it.bookTitle == cleanFirst }
                            val sprintDays = prof?.targetFinishDays ?: 15
                            tasks.add(
                                ContractTask(
                                    id = UUID.randomUUID().toString().take(8),
                                    material = "[${track.name}] 阶段结项完成",
                                    scope = "【前${sprintDays}天攻坚大捷】《$cleanFirst》全书已冲刺结项并自动归档！时间槽已释放，支持通过【阶段追加新书】无缝接入进阶内容！",
                                    taskType = "review",
                                    estimatedMinutes = minOf(30, allocatedForSubject),
                                    completed = false
                                )
                            )
                        } else {
                            val bk = activeTrackBooks.getOrNull(day % maxOf(1, activeTrackBooks.size)) 
                                ?: activeTrackBooks.firstOrNull() ?: track.books.firstOrNull() ?: "[${track.name}] 核心资料"
                            val cleanBk = BookLifecycleEngine.cleanBookTitle(bk)
                            val prof = resolved.progressProfiles.firstOrNull { it.bookTitle == cleanBk }
                            val startCh = prof?.startChapter ?: 1
                            val targetDays = prof?.targetFinishDays
                            val isSprintFinish = (targetDays != null && day == targetDays)
                            val sprintTag = if (isSprintFinish) "【15天冲刺达成·今日结项】" else if (targetDays != null) "【${targetDays}天阶段冲刺】" else ""
                            val skipTag = if (startCh > 1) " (已跳过前${startCh - 1}章已学内容)" else ""

                            val pb = track.problemSets.getOrNull(day % maxOf(1, track.problemSets.size)) 
                                ?: track.problemSets.firstOrNull() ?: "[${track.name}] 经典习题"
                            val lc = track.videoLectures.getOrNull(day % maxOf(1, track.videoLectures.size))

                            // Inspect if there is parsed academic material with extracted reading volumes & content density
                            val matchedMaterial = rawInputs.parsedMaterials.firstOrNull { pm ->
                                pm.subject.contains(track.name, ignoreCase = true) ||
                                track.name.contains(pm.subject, ignoreCase = true) ||
                                pm.fileName.contains(track.name, ignoreCase = true)
                            } ?: rawInputs.parsedMaterials.getOrNull(idx % maxOf(1, rawInputs.parsedMaterials.size))

                            if (matchedMaterial != null && matchedMaterial.readingVolume.chapters.isNotEmpty()) {
                                // DENSITY & VOLUME INFORMED GENERATOR: Exact pages, chapters & cognitive load
                                val chapters = matchedMaterial.readingVolume.chapters
                                val startOffset = maxOf(0, minOf(chapters.size - 1, startCh - 1))
                                val remainingChs = chapters.drop(startOffset)
                                val curChapter = if (remainingChs.isNotEmpty()) remainingChs[(day - 1) % remainingChs.size] else chapters[(day - 1) % chapters.size]
                                val density = matchedMaterial.contentDensity

                                val pageSpan = if (curChapter.pageCount > 0) {
                                    "(p.${curChapter.startPage} - p.${curChapter.endPage}, 共${curChapter.pageCount}页)"
                                } else {
                                    "(精读${maxOf(15, curChapter.pageCount)}页)"
                                }

                                val densityTag = "【${density.densityTier} · 认知系数${density.cognitiveLoadIndex}】"
                                val dynamicMinutes = (allocatedForSubject * (density.cognitiveLoadIndex / 1.4f)).toInt().coerceIn(30, 90)

                                tasks.add(
                                    ContractTask(
                                        id = UUID.randomUUID().toString().take(8),
                                        material = "[${track.name}] ${matchedMaterial.fileName}",
                                        scope = "${sprintTag}研读 ${curChapter.title} $pageSpan $densityTag$skipTag",
                                        taskType = "reading",
                                        estimatedMinutes = dynamicMinutes,
                                        completed = false
                                    )
                                )

                                val exCount = minOf(25, curChapter.estimatedProblems)
                                val practiceDesc = "${curChapter.title} 配套题第 1-$exCount 题"

                                tasks.add(
                                    ContractTask(
                                        id = UUID.randomUUID().toString().take(8),
                                        material = pb,
                                        scope = practiceDesc,
                                        taskType = "practice",
                                        estimatedMinutes = (allocatedForSubject * 0.9f).toInt().coerceAtLeast(30),
                                        completed = false
                                    )
                                )
                            } else {
                                // Fallback to standard track generation if no PDF/text parsed material
                                when (track.prioritySlot) {
                                    1 -> { // Language / Memory (English, Japanese, Ancient Chinese, Politics)
                                        val subChapter = startCh + ((day - 1) % maxOf(1, 16 - startCh + 1))
                                        val chapterTitle = AcademicBookCatalog.getChapterTitle(bk, subChapter)
                                        tasks.add(
                                            ContractTask(
                                                id = UUID.randomUUID().toString().take(8),
                                                material = bk,
                                                scope = "${sprintTag}${chapterTitle}$skipTag",
                                                taskType = "reading",
                                                estimatedMinutes = allocatedForSubject,
                                                completed = false
                                            )
                                        )
                                    }
                                    2 -> { // Math / Intensive Logic (Math 1, 408 CS core)
                                        val subChapter = startCh + ((day - 1) % maxOf(1, 14 - startCh + 1))
                                        val chapterTitle = AcademicBookCatalog.getChapterTitle(bk, subChapter)
                                        tasks.add(
                                            ContractTask(
                                                id = UUID.randomUUID().toString().take(8),
                                                material = bk,
                                                scope = "${sprintTag}${chapterTitle}$skipTag",
                                                taskType = "reading",
                                                estimatedMinutes = allocatedForSubject,
                                                completed = false
                                            )
                                        )
                                    }
                                    3 -> { // Practice / Major Deep Dive (408 practice, Modern Chinese, Specialized problems)
                                        val subSet = (day % 12) + 1
                                        tasks.add(
                                            ContractTask(
                                                id = UUID.randomUUID().toString().take(8),
                                                material = pb,
                                                scope = "第 $subSet 组重点综合大题",
                                                taskType = "practice",
                                                estimatedMinutes = allocatedForSubject,
                                                completed = false
                                            )
                                        )
                                    }
                                    else -> { // Politics or General Practice
                                        val subSet = (day % 15) + 1
                                        tasks.add(
                                            ContractTask(
                                                id = UUID.randomUUID().toString().take(8),
                                                material = pb,
                                                scope = "第 $subSet 组核心选择题 30 道专项演练",
                                                taskType = "practice",
                                                estimatedMinutes = allocatedForSubject,
                                                completed = false
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Multi-subject Spaced Repetition Review
                    val srLabel = when {
                        day == 2 -> "D+1 间隔复习: 【${subjectTracks.joinToString("/") { it.name }}】昨日错题与易错概念回放"
                        day % 7 == 0 -> "D+7 间隔复习: 全学科周度交叉累积式滚动回顾"
                        day % 14 == 0 -> "D+14 间隔复习: 多学科双周知识网络掌握度大盘点"
                        day % 3 == 0 -> "D+3 间隔复习: 跨学科薄弱点主动回忆闪卡演练"
                        else -> "微复习: 每日15分钟多学科错题本沉淀与关键概念抽测"
                    }
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "艾宾浩斯全科记忆引擎",
                            scope = srLabel,
                            taskType = "review",
                            estimatedMinutes = reviewMins,
                            completed = false
                        )
                    )
                } else {
                    // Single Subject Regular Mission Day with Interleaved Tasks & Spaced Repetition
                    val matchedMaterial = rawInputs.parsedMaterials.firstOrNull()

                    val (readingMaterial, readingScope, readingMins) = if (matchedMaterial != null && matchedMaterial.readingVolume.chapters.isNotEmpty()) {
                        val chapters = matchedMaterial.readingVolume.chapters
                        val curChapter = chapters[(day - 1) % chapters.size]
                        val density = matchedMaterial.contentDensity
                        val pageSpan = if (curChapter.pageCount > 0) "(p.${curChapter.startPage} - p.${curChapter.endPage}, 共${curChapter.pageCount}页)" else "(精读${curChapter.pageCount}页)"
                        val mins = (curChapter.pageCount * density.recommendedMinutesPerPage * 0.45f).toInt().coerceIn(40, 75)
                        Triple(
                            matchedMaterial.fileName,
                            "研读 ${curChapter.title} $pageSpan · 【${density.densityTier}】",
                            mins
                        )
                    } else {
                        val bk = booksList[bookIndex % booksList.size]
                        val cleanBk = BookLifecycleEngine.cleanBookTitle(bk)
                        val prof = resolved.progressProfiles.firstOrNull { it.bookTitle == cleanBk }
                        val startCh = prof?.startChapter ?: 1
                        val chNum = startCh + ((day - 1) % maxOf(1, 14 - startCh + 1))
                        val skipTag = if (startCh > 1) " (已跳过前${startCh - 1}章已学内容)" else ""
                        val sprintTag = if (prof?.targetFinishDays != null) "【${prof.targetFinishDays}天突击】" else ""
                        Triple(
                            bk,
                            "${sprintTag}第 $chNum 章: 核心考点解析与重点例题精读$skipTag",
                            50
                        )
                    }

                    val problem = problemsList[problemIndex % problemsList.size]
                    val lecture = lecturesList[lectureIndex % lecturesList.size]

                    coreFocus = "第 $day 天复习: ${readingMaterial.substringBefore("(").trim()} 核心攻坚"

                    // 1. Reading Task (Informed by extracted volume & density)
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = readingMaterial,
                            scope = readingScope,
                            taskType = "reading",
                            estimatedMinutes = readingMins,
                            completed = false
                        )
                    )

                    // 2. Lecture / Concept Deconstruction (35 - 50 min)
                    if (day % 2 == 1) {
                        tasks.add(
                            ContractTask(
                                id = UUID.randomUUID().toString().take(8),
                                material = lecture,
                                scope = "第 ${(day % 8) + 1} 讲: 重点考点剖析与例题精讲",
                                taskType = "lecture",
                                estimatedMinutes = 40,
                                completed = false
                            )
                        )
                    }

                    // 3. Deliberate Practice (50 - 75 min)
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = problem,
                            scope = "第 ${(day % 15) + 1} 组: 20道真题/模拟题专项练习",
                            taskType = "practice",
                            estimatedMinutes = 65,
                            completed = false
                        )
                    )

                    // 4. Spaced Repetition Review (Interleaved intervals: D+1, D+3, D+7, D+14)
                    val srLabel = when {
                        day == 2 -> "D+1 间隔复习: 第1天核心公式与易错陷阱回放"
                        day % 7 == 0 -> "D+7 间隔复习: 周度交叉累积式滚动回顾"
                        day % 14 == 0 -> "D+14 间隔复习: 双周知识体系掌握度大盘点"
                        day % 3 == 0 -> "D+3 间隔复习: 薄弱知识点主动回忆闪卡演练"
                        else -> "微复习: 每日15分钟错题本沉淀与关键概念抽测"
                    }

                    val srMinutes = if (day % 7 == 0) 45 else 25
                    tasks.add(
                        ContractTask(
                            id = UUID.randomUUID().toString().take(8),
                            material = "艾宾浩斯记忆引擎",
                            scope = srLabel,
                            taskType = "review",
                            estimatedMinutes = srMinutes,
                            completed = false
                        )
                    )

                    bookIndex++
                    problemIndex++
                    lectureIndex++
                }
            }

            val allocatedMinutes = tasks.sumOf { it.estimatedMinutes }
            dailySchedule.add(
                DailyMission(
                    day = day,
                    date = dayDate,
                    coreFocus = coreFocus,
                    allocatedMinutes = allocatedMinutes,
                    tasks = tasks,
                    phaseLabel = phaseLabel,
                    phaseBookGoal = phaseBookGoal
                )
            )
        }

        val totalEstimatedMinutes = dailySchedule.sumOf { it.allocatedMinutes }
        val totalEstimatedHours = (totalEstimatedMinutes / 60.0).toInt()

        val initialAdjustments = AdaptiveAdjustments(
            feedbackSummary = "备考规划初始化完成，已嵌入按月/按周分阶段书目结项体系、艾宾浩斯间隔复习矩阵及 4 级阶段诊断里程碑。",
            paceAdjustment = "基准复习配速设定为每天 ${(totalEstimatedHours.toFloat() / totalDays).let { String.format(Locale.US, "%.1f", it) }} 小时。已科学设置周度/月度阶段验收节点，严格保障按期结项。",
            upcomingChanges = listOf(
                "当前处于 ${monthlyPhases.firstOrNull()?.title ?: "第 1 阶段"}，重点完成第一批指定书目章节。",
                "每周末自动触发【周度书目结项与错题清零】验收，动态校准后续周度进度。"
            )
        )

        return StudySyncContract(
            profileId = profileId,
            planOverview = PlanOverview(
                title = rawInputs.examTitle.ifBlank { "研究生入学考试备考规划引擎" },
                targetDate = targetDateStr,
                totalEstimatedHours = totalEstimatedHours,
                currentPaceStatus = "按期推进",
                completionRate = "0%"
            ),
            dashboardComparison = DashboardComparison(
                profileA_Progress = "0%",
                profileB_Progress = "0%",
                velocityDiff = "同步基线 (进度差异 0%)"
            ),
            milestones = milestones,
            dailySchedule = dailySchedule,
            adaptiveAdjustments = initialAdjustments,
            bookAnalysis = analyzeBooksAndWorkload(rawInputs),
            monthlyPhases = monthlyPhases,
            weeklyPhases = weeklyPhases,
            parsedMaterials = rawInputs.parsedMaterials,
            bookProgressProfiles = resolved.progressProfiles,
            archivedCompletedBooks = resolved.archivedCompletedBooks,
            stageTransitionNotes = if (resolved.archivedCompletedBooks.isNotEmpty()) {
                "已智能结项归档 ${resolved.archivedCompletedBooks.size} 本书目（免除后续每日排期）：" +
                resolved.archivedCompletedBooks.joinToString("、")
            } else {
                "当前书目全部处于活跃推进中，支持在备注中随手写'XX书已学完/15天刷完'或在管理面板中结项归档。"
            }
        )
    }

    /**
     * Dynamically recalibrates the plan based on historical logs (actual time, error rates, difficulty).
     * Strictly preserves the hard target deadline while rebalancing workload, injecting remediation, or tuning buffer days.
     */
    fun recalibratePlan(
        contract: StudySyncContract,
        logs: List<ExecutionLogEntity>,
        rawInputs: StudyRawInputs? = null
    ): StudySyncContract {
        val allTasks = contract.dailySchedule.flatMap { it.tasks }
        val totalTasks = allTasks.size
        val completedTasks = allTasks.count { it.completed }
        val completionRatePercent = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

        // Analyze historical logs
        val recentLogs = logs.take(15)
        val avgErrorRate = if (recentLogs.isNotEmpty()) recentLogs.map { it.errorRatePercent }.average().toInt() else 0
        val highDifficultyCount = recentLogs.count { 
            it.difficultyFeedback == "Challenging" || it.difficultyFeedback == "Overwhelming" || 
            it.difficultyFeedback == "偏难" || it.difficultyFeedback == "吃力" || it.difficultyFeedback == "严重超负荷" 
        }
        val timeRatio = if (recentLogs.isNotEmpty()) {
            val totalActual = recentLogs.sumOf { it.actualMinutes }
            val totalExpected = recentLogs.sumOf { it.expectedMinutes }
            if (totalExpected > 0) totalActual.toFloat() / totalExpected else 1.0f
        } else 1.0f

        // Determine current pace status
        val currentPaceStatus = when {
            completionRatePercent >= 75 -> "进度超前"
            avgErrorRate > 30 || timeRatio > 1.25f -> "进度滞后"
            avgErrorRate < 15 && timeRatio <= 0.95f -> "进度超前"
            else -> "按期推进"
        }

        val targetDeadline = rawInputs?.targetDate?.ifBlank { null } ?: contract.planOverview.targetDate

        // Generate rigorous adaptive adjustments
        val feedbackSummary = buildString {
            append("已结合最新导入备考资料分析 ${recentLogs.size} 条历史执行数据。")
            if (avgErrorRate > 25) {
                append("近期做题错题率偏高 ($avgErrorRate%)，系统已触发知识点薄弱预警。")
            } else {
                append("做题错题率处于健康区间 ($avgErrorRate%)，核心基础知识扎实。")
            }
            if (timeRatio > 1.15f) {
                append("实际学习耗时超出计划预算 ${(timeRatio * 100 - 100).toInt()}%。")
            } else if (timeRatio < 0.90f) {
                append("推进速率良好，复习任务提前高效完成。")
            }
            if (rawInputs != null && rawInputs.materialCards.isNotEmpty()) {
                val includedCount = rawInputs.materialCards.count { it.isIncluded }
                append("已基于最新 ${includedCount} 项已纳入资料重新测算权重。")
            }
        }

        val paceAdjustment = when (currentPaceStatus) {
            "进度滞后" -> "任务负荷智能精简：将次要泛读转为主动回忆，扩大后续缓冲窗口以吸纳延误，严格捍卫考前硬性截止日 $targetDeadline。"
            "进度超前" -> "学习速率领跑：在不延长总时长的前提下，适度注入高难度跨章节综合大题与限时演练。"
            else -> "保持最优复习配速：合理维持 50% 刻意刷题与 50% 艾宾浩斯间隔复习的科学平衡。"
        }

        val upcomingChanges = mutableListOf<String>()
        if (avgErrorRate > 25) {
            upcomingChanges.add("下一轮训练已自动注入针对高频易错考点的专项强化任务。")
            upcomingChanges.add("后续练习题预估耗时已上调 15%，鼓励深入做题复盘与错因深挖。")
        } else {
            upcomingChanges.add("继续保持标准化刻意练习节奏平稳推进。")
        }
        if (highDifficultyCount >= 3) {
            upcomingChanges.add("在下一个里程碑考核前已插入 30 分钟认知负荷缓冲期。")
        } else {
            upcomingChanges.add("阶段里程碑考核时间节点已核验锁定。")
        }
        upcomingChanges.add("严格依靠动态缓冲日重新平衡机制，坚决捍卫考前硬性截止日 ($targetDeadline)。")

        val updatedBookAnalysis = if (rawInputs != null && (rawInputs.books.isNotEmpty() || rawInputs.materialCards.isNotEmpty())) {
            analyzeBooksAndWorkload(rawInputs)
        } else {
            contract.bookAnalysis
        }

        // If rawInputs provided with updated cards or dates, generate updated schedule incorporating new materials
        val finalSchedule: List<DailyMission> = if (rawInputs != null && (rawInputs.materialCards.isNotEmpty() || rawInputs.books.isNotEmpty())) {
            val freshPlan = generateRigorousPlan(contract.profileId, rawInputs)
            val completedDayMap = contract.dailySchedule.associateBy { it.day }
            freshPlan.dailySchedule.map { freshDay ->
                val oldDay = completedDayMap[freshDay.day]
                if (oldDay != null && oldDay.tasks.isNotEmpty() && oldDay.tasks.all { it.completed }) {
                    oldDay
                } else {
                    val tunedTasks = freshDay.tasks.map { task ->
                        val oldTask = oldDay?.tasks?.firstOrNull { it.id == task.id || (it.material == task.material && it.scope == task.scope) }
                        if (oldTask?.completed == true) {
                            task.copy(completed = true, actualMinutes = oldTask.actualMinutes, errorRatePercent = oldTask.errorRatePercent)
                        } else if (avgErrorRate > 25 && task.taskType == "practice") {
                            task.copy(
                                scope = if (task.scope.startsWith("[重点复盘]")) task.scope else "[重点复盘] ${task.scope}",
                                estimatedMinutes = (task.estimatedMinutes * 1.15).toInt().coerceAtLeast(task.estimatedMinutes + 5)
                            )
                        } else {
                            task
                        }
                    }
                    freshDay.copy(tasks = tunedTasks)
                }
            }
        } else {
            contract.dailySchedule.map { day ->
                val isDayAllDone = day.tasks.isNotEmpty() && day.tasks.all { it.completed }
                if (isDayAllDone) {
                    day
                } else {
                    val tunedTasks = day.tasks.map { task ->
                        if (!task.completed && avgErrorRate > 25 && task.taskType == "practice") {
                            task.copy(
                                scope = if (task.scope.startsWith("[重点复盘]")) task.scope else "[重点复盘] ${task.scope}",
                                estimatedMinutes = (task.estimatedMinutes * 1.15).toInt().coerceAtLeast(task.estimatedMinutes + 5)
                            )
                        } else {
                            task
                        }
                    }
                    day.copy(tasks = tunedTasks)
                }
            }
        }

        val allTunedTasks = finalSchedule.flatMap { it.tasks }
        val newDoneCount = allTunedTasks.count { it.completed }
        val newCompletionRate = if (allTunedTasks.isNotEmpty()) "${(newDoneCount * 100 / allTunedTasks.size)}%" else "0%"
        val newTotalHours = (finalSchedule.sumOf { it.allocatedMinutes } / 60.0).toInt()

        // Update milestone completion state
        val updatedMilestones = contract.milestones.map { m ->
            val stageThreshold = when {
                m.stage.contains("25%") -> 25
                m.stage.contains("50%") -> 50
                m.stage.contains("75%") -> 75
                m.stage.contains("100%") -> 100
                else -> 50
            }
            m.copy(completed = completionRatePercent >= stageThreshold)
        }

        val updatedPlanOverview = if (rawInputs != null) {
            contract.planOverview.copy(
                title = rawInputs.examTitle.ifBlank { contract.planOverview.title },
                targetDate = targetDeadline,
                totalEstimatedHours = if (newTotalHours > 0) newTotalHours else contract.planOverview.totalEstimatedHours,
                completionRate = newCompletionRate,
                currentPaceStatus = currentPaceStatus
            )
        } else {
            contract.planOverview.copy(
                completionRate = "$completionRatePercent%",
                currentPaceStatus = currentPaceStatus
            )
        }

        return contract.copy(
            planOverview = updatedPlanOverview,
            bookAnalysis = updatedBookAnalysis,
            dailySchedule = finalSchedule,
            milestones = updatedMilestones,
            adaptiveAdjustments = AdaptiveAdjustments(
                feedbackSummary = feedbackSummary,
                paceAdjustment = paceAdjustment,
                upcomingChanges = upcomingChanges
            )
        )
    }

    /**
     * Generates unified dashboard comparison between Profile A and Profile B.
     */
    fun computeComparison(
        contractA: StudySyncContract?,
        contractB: StudySyncContract?
    ): DashboardComparison {
        val rateA = contractA?.planOverview?.completionRate?.replace("%", "")?.toIntOrNull() ?: 0
        val rateB = contractB?.planOverview?.completionRate?.replace("%", "")?.toIntOrNull() ?: 0

        val diff = rateA - rateB
        val velocityDiff = when {
            diff > 0 -> "+$diff% 领先 (画像 A 每周领先约 ${(diff * 0.4).let { String.format(Locale.US, "%.1f", it) }} 小时)"
            diff < 0 -> "-${-diff}% 滞后 (画像 B 每周领先约 ${(-diff * 0.4).let { String.format(Locale.US, "%.1f", it) }} 小时)"
            else -> "同步基线 (进度差异 0%)"
        }

        return DashboardComparison(
            profileA_Progress = "$rateA%",
            profileB_Progress = "$rateB%",
            velocityDiff = velocityDiff
        )
    }

    /**
     * Deeply analyzes imported books/materials, questions density, and study time requirements.
     * Computes workload metrics, feasibility assessment, and provides actionable pedagogical advice.
     */
    fun analyzeBooksAndWorkload(rawInputs: StudyRawInputs): BookAnalysisReport {
        val allItems = mutableListOf<AnalyzedBookItem>()

        // 1. Analyze Books & Textbooks
        val rawBooks = if (rawInputs.books.isNotEmpty()) rawInputs.books else listOf("核心学科理论讲义与教材")
        rawBooks.forEach { title ->
            val lower = title.lowercase(Locale.ROOT)
            val isJapanese = lower.contains("日语") || lower.contains("203") || lower.contains("标日") || lower.contains("高分专项突破")
            val isCultureOrTeaching = lower.contains("文化要略") || lower.contains("对外汉语") || lower.contains("外国文化史") || lower.contains("冲关宝典") || lower.contains("汉硕") || lower.contains("国际中文") || lower.contains("程裕祯") || lower.contains("刘珣") || lower.contains("孟昭毅")
            val isModernChinese = lower.contains("现代汉语") || lower.contains("现汉") || lower.contains("邢福义")
            val isAncientChinese = lower.contains("古代汉语") || lower.contains("古汉") || lower.contains("郭锡良") || lower.contains("王力")
            val isDataStructure = lower.contains("数据结构") || (lower.contains("算法") && !isCultureOrTeaching)
            val isOsOrNet = lower.contains("操作系统") || lower.contains("网络") || lower.contains("计网") || lower.contains("组成原理") || lower.contains("计组")
            val isMath = lower.contains("数学") || lower.contains("高数") || lower.contains("线代") || lower.contains("概率") || lower.contains("武忠祥") || lower.contains("武钟祥") || lower.contains("660") || lower.contains("张宇") || lower.contains("汤家凤") || lower.contains("李永乐")
            val isEnglish = !isJapanese && (lower.contains("英语") || lower.contains("词汇") || lower.contains("长难句") || lower.contains("阅读") || lower.contains("gre"))
            val isPolitics = lower.contains("政治") || lower.contains("肖秀荣") || lower.contains("1000题") || lower.contains("肖1000") || lower.contains("考点")
            val isMedicine = lower.contains("生理") || lower.contains("生化") || lower.contains("病理") || lower.contains("西医") || lower.contains("内科") || lower.contains("外科")

            val (type, chapters, questions, diff, readMin, probMin, weight, tip) = when {
                isDataStructure -> Tuple8(
                    "核心理论与算法 (王道)", 10, 320, "高难必考", 50, 6.0f,
                    "408统考分值 45分 (核心拉分项)",
                    "紧扣王道数据结构复习指导，必须在纸上手写核心算法 (二叉树遍历/平衡树/图最短路径/排序)，重视算法大题时空复杂度推导。"
                )
                isOsOrNet -> Tuple8(
                    "硬核系统架构 (王道)", 8, 280, "高难重点", 45, 4.5f,
                    "408统考分值 35-45分",
                    "死锁检测、虚拟内存分页与 TCP 拥塞控制为兵家必争之地；计组务必理清数据通路与指令流水线，结合王道单科课后题筑牢防线。"
                )
                isMath -> Tuple8(
                    "数学核心讲义 (武忠祥/概率论)", 18, 660, "极高难度", 55, 8.0f,
                    "考研数学 150分 (决定性科目)",
                    "以武忠祥高数讲义为骨架构建完整概念网，主攻武忠祥660题强化细节甄别与计算精度，必须在草稿纸上规范算到底；概率论基础讲义强化随机变量分布与数字特征。"
                )
                isJapanese -> Tuple8(
                    "二外与专业日语专项", 14, 300, "细致规范", 35, 8.0f,
                    "考研日语 100分",
                    "以《考研日语高分专项突破·阅读分册》为主阵地，每日坚持长难句精读与限时阅读；系统梳理动词活用变形与敬语系统，保持高分答题敏锐度。"
                )
                isModernChinese -> Tuple8(
                    "现代汉语理论与习题", 14, 420, "严谨规范", 45, 6.5f,
                    "专业课分值 75-150分",
                    "以邢福义第三版现代汉语为标准，严格记忆概念定义；紧扣《现代汉语考研与辅导习题集》，熟练掌握国际音标、声韵配合与句法层次分析法树形图绘制。"
                )
                isAncientChinese -> Tuple8(
                    "古代汉语通论与文选精读", 16, 350, "深厚积淀", 50, 7.0f,
                    "专业课分值 75-150分",
                    "立足郭锡良版古代汉语上下两册，坚持每日诵读名篇文选，牢固掌握古汉语词类活用规律与虚词，强化无标点断句手感。"
                )
                isCultureOrTeaching -> Tuple8(
                    "中外文化与教引理论综合", 16, 450, "广博识记", 45, 5.0f,
                    "汉硕专业综合 150分",
                    "精读程裕祯《中国文化要略》(第五版)与孟昭毅《外国文化史》(第二版)梳理中外文明脉络；深入掌握刘珣《对外汉语教学引论》二语习得框架；利用《冲关宝典》刷透大题案例分析。"
                )
                isEnglish -> Tuple8(
                    "语言精读与词汇", 15, 200, "重在积累", 35, 12.0f,
                    "考研英语 100分 (单科防受限)",
                    "阅读重点在于真题长难句剖析与命题人设坑逻辑 (偷换概念/正反倒置)，坚持艾宾浩斯高频词复盘。"
                )
                isPolitics -> Tuple8(
                    "考点精讲与肖1000题", 12, 1000, "强化背诵", 30, 2.5f,
                    "思想政治理论 100分",
                    "紧扣肖秀荣1000题夯实客观选择题基础，前期重点突破马原哲学逻辑和史纲脉络，利用碎片时间二刷错题，后期冲刺默写肖四肖八。"
                )
                isMedicine -> Tuple8(
                    "临床医学综合专著", 16, 600, "高密度记忆", 50, 4.0f,
                    "西医综合 300分 (满分权重高)",
                    "代谢通路与鉴别诊断需画横向思维导图对比，强化多选题综合诊断敏感度。"
                )
                else -> Tuple8(
                    "专业研读教材", 12, 300, "核心重点", 40, 4.5f,
                    "专业课核心主干考核",
                    "建议采用'考点速览 + 真题倒逼定位'策略，跳过过时繁复内容，聚焦历年常考高频题型。"
                )
            }

            val cleanTitle = BookLifecycleEngine.cleanBookTitle(title)
            val catalogEntry = AcademicBookCatalog.resolveBook(cleanTitle)
            val prof = rawInputs.bookProgressProfiles.firstOrNull { 
                it.bookTitle == cleanTitle 
            } ?: BookLifecycleEngine.parseBookProgressFromText(title, rawInputs.customNotes)
            val isArchived = rawInputs.archivedCompletedBooks.contains(cleanTitle) || prof.isCompleted

            val startCh = prof.startChapter
            val totalChs = if (catalogEntry.totalChapters > 0) catalogEntry.totalChapters else chapters
            val remainingChapters = if (isArchived) 0 else maxOf(1, totalChs - (startCh - 1))
            val remainingQuestions = if (isArchived) 0 else ((questions * (100 - prof.completedPercent)) / 100).coerceAtLeast(0)

            allItems.add(
                AnalyzedBookItem(
                    title = if (cleanTitle.length < 8 && catalogEntry.canonicalTitle.isNotBlank()) catalogEntry.canonicalTitle else cleanTitle,
                    bookType = catalogEntry.subjectCategory.ifBlank { type },
                    estimatedTotalChapters = remainingChapters,
                    estimatedQuestionCount = remainingQuestions,
                    difficultyLevel = catalogEntry.difficultyTier.ifBlank { diff },
                    recommendedReadingMinutesPerChapter = readMin,
                    recommendedMinutesPerProblem = probMin,
                    keyExamWeight = catalogEntry.examWeightScore.ifBlank { weight },
                    studyTip = when {
                        isArchived -> "【已攻克结项归档】全书已学完，已自动从后续排期与负荷中完全排除。"
                        prof.completedPercent > 0 -> "【已学完 ${prof.completedPercent}% · 从第 ${startCh} 章起】 ${catalogEntry.pedagogicalAdvice.ifBlank { tip }}"
                        else -> catalogEntry.pedagogicalAdvice.ifBlank { tip }
                    },
                    completedPercent = if (isArchived) 100 else prof.completedPercent,
                    startChapter = startCh,
                    targetFinishDays = prof.targetFinishDays,
                    isCompleted = isArchived,
                    stageLabel = if (isArchived) "已结项归档" else prof.stageLabel
                )
            )
        }

        // 2. Analyze Problem Sets
        rawInputs.problemSets.forEach { title ->
            val cleanTitle = BookLifecycleEngine.cleanBookTitle(title)
            val prof = BookLifecycleEngine.parseBookProgressFromText(title, rawInputs.customNotes)
            val isArchived = rawInputs.archivedCompletedBooks.contains(cleanTitle) || prof.isCompleted

            val lower = title.lowercase(Locale.ROOT)
            val isRealExam = lower.contains("真题") || lower.contains("全真")
            val isBigProblem = lower.contains("大题") || lower.contains("专项") || lower.contains("算法")

            val (type, chapters, questions, diff, readMin, probMin, weight, tip) = when {
                isRealExam -> Tuple8(
                    "历年真题分类集", 15, 600, "高难全真", 20, 5.0f,
                    "100% 贴合考场原题风格",
                    "近10年真题必须全真限时模考！严格控制时间，保留近3年真题作为考前三周终极能力诊断。"
                )
                isBigProblem -> Tuple8(
                    "主观综合大题集", 8, 180, "高难攻坚", 25, 14.0f,
                    "专业课及数学拉开分差的关键",
                    "大题需要规范书写步骤，按踩分点严格评分，特别注意证明题的逻辑链条闭环。"
                )
                else -> Tuple8(
                    "核心题库专项演练", 10, 450, "巩固提分", 15, 4.0f,
                    "快速排查盲区与解题提速",
                    "做题时务必用红笔标记一次性未做出的题目，第2天与第7天必须进行闭卷二次重做。"
                )
            }

            val remainingChapters = if (isArchived) 0 else maxOf(1, chapters - (prof.startChapter - 1))
            val remainingQuestions = if (isArchived) 0 else ((questions * (100 - prof.completedPercent)) / 100).coerceAtLeast(0)

            allItems.add(
                AnalyzedBookItem(
                    title = cleanTitle,
                    bookType = type,
                    estimatedTotalChapters = remainingChapters,
                    estimatedQuestionCount = remainingQuestions,
                    difficultyLevel = diff,
                    recommendedReadingMinutesPerChapter = readMin,
                    recommendedMinutesPerProblem = probMin,
                    keyExamWeight = weight,
                    studyTip = if (isArchived) "【已攻克结项归档】全书已学完，已自动免除后续排期。" else tip,
                    completedPercent = if (isArchived) 100 else prof.completedPercent,
                    startChapter = prof.startChapter,
                    targetFinishDays = prof.targetFinishDays,
                    isCompleted = isArchived,
                    stageLabel = if (isArchived) "已结项归档" else prof.stageLabel
                )
            )
        }

        // 3. Compute Granular Workload Metrics
        var totalReadingMins = 0
        var totalPracticeMins = 0
        allItems.forEach { item ->
            totalReadingMins += item.estimatedTotalChapters * item.recommendedReadingMinutesPerChapter
            totalPracticeMins += (item.estimatedQuestionCount * item.recommendedMinutesPerProblem).toInt()
        }

        val lectureHours = rawInputs.videoLectures.size * 14.0f
        val readingHours = totalReadingMins / 60.0f
        val practiceHours = totalPracticeMins / 60.0f
        // Spaced repetition review overhead (approx 25% of active study)
        val reviewHours = (readingHours + practiceHours) * 0.25f
        val totalEstimatedHours = readingHours + practiceHours + reviewHours + lectureHours

        // Target timeline parsing
        val totalDays = 45 // Baseline 45 days
        val dailyHoursRequired = totalEstimatedHours / totalDays.toFloat()
        val dailyHoursAvailable = if (rawInputs.dailyHoursAvailable > 0.5f) rawInputs.dailyHoursAvailable else 4.0f
        val paceDiff = dailyHoursAvailable - dailyHoursRequired

        val paceFeasibility = when {
            paceDiff >= 0.6f -> "负荷适中 (每日预计余量 +${String.format(Locale.US, "%.1f", paceDiff)} 小时，容错率充足)"
            paceDiff >= -0.3f -> "节奏紧凑 (每日基本打平，需高度自律严守番茄钟)"
            else -> "存在时间赤字 (每日缺口 ${String.format(Locale.US, "%.1f", -paceDiff)} 小时！建议每天加时或精简次要辅导书)"
        }

        val workloadMetrics = WorkloadMetrics(
            totalReadingHours = String.format(Locale.US, "%.1f", readingHours).toFloat(),
            totalPracticeHours = String.format(Locale.US, "%.1f", practiceHours).toFloat(),
            totalReviewHours = String.format(Locale.US, "%.1f", reviewHours).toFloat(),
            totalEstimatedHours = String.format(Locale.US, "%.1f", totalEstimatedHours).toFloat(),
            dailyHoursRequired = String.format(Locale.US, "%.1f", dailyHoursRequired).toFloat(),
            dailyHoursAvailable = dailyHoursAvailable,
            paceFeasibility = paceFeasibility,
            paceDeficitOrSurplusHours = String.format(Locale.US, "%.1f", paceDiff).toFloat(),
            recommendedDailyRatio = "25% 核心教材精读 : 55% 限时刷题内化 : 20% 艾宾浩斯闭卷复盘"
        )

        // 4. Strategic Advice
        val strategicAdvice = listOf(
            StrategicAdviceItem(
                category = "刷题战术",
                title = "真题限时与草稿规范化",
                content = "选择题严格限定 3-4 分钟/题，大题限时 12-15 分钟。做题时严禁翻阅解析，草稿纸按题号分区书写，便于错题复盘时精确还原考场思维断点。",
                priority = "HIGH"
            ),
            StrategicAdviceItem(
                category = "遗忘防范",
                title = "严格执行艾宾浩斯 D+1/D+3/D+7 滚动重查",
                content = "做错题目当天用红笔高亮，次日早晨进行 15 分钟极速回想；第 3 天遮挡答案重新独立书写；第 7 天抽取 30% 进行综合混编重测，彻底打断遗忘曲线。",
                priority = "HIGH"
            ),
            StrategicAdviceItem(
                category = "精力与缓冲",
                title = "科学设立缓冲机动日 (Buffer Day)",
                content = "系统将在 25%、50%、75% 里程碑前夕自动安排缓冲机动日。严禁将机动日排满新课，专门用于消化高难度错题与心理韧性充能，防止备考中途倦怠。",
                priority = "MEDIUM"
            ),
            StrategicAdviceItem(
                category = "考纲动态",
                title = "紧扣近 3 年综合命题与跨章节考法",
                content = "最新命题趋势弱化了单纯死记硬背概念，大幅增加了综合应用与边界条件判断题型。各科目复习后期应注重知识图谱跨章节串联。",
                priority = "MEDIUM"
            )
        )

        // 5. Search Grounding Sources (Verified standards)
        val groundingSources = listOf(
            GroundingSourceItem(
                title = "教育部教育考试院 全国硕士研究生招生考试学科考查大纲",
                url = "https://yz.chsi.com.cn",
                snippet = "考研专业课统一命题与大纲分值分布标准，覆盖考查目标与题型分值占比规范。"
            ),
            GroundingSourceItem(
                title = "全国高校计算机专业核心课程与统考 408 知识点解析标准",
                url = "https://www.chinagrad.edu.cn",
                snippet = "针对数据结构、组成原理、操作系统与计算机网络经典教材重难点章节耗时基准参考。"
            )
        )

        return BookAnalysisReport(
            booksAnalyzed = allItems,
            workloadMetrics = workloadMetrics,
            strategicAdvice = strategicAdvice,
            searchGroundingSources = groundingSources,
            feasibilityAssessment = "基于导入的 ${allItems.size} 种书目与题集，总测算学习量为 ${String.format(Locale.US, "%.1f", totalEstimatedHours)} 小时。当前配速可行性：$paceFeasibility。"
        )
    }

    private data class Tuple8<A, B, C, D, E, F, G, H>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H
    )

    data class SubjectTrack(
        val name: String,
        val books: List<String>,
        val problemSets: List<String>,
        val videoLectures: List<String>,
        val prioritySlot: Int // 1: 晨间记忆/外语, 2: 上午数理, 3: 下午专业大题, 4: 晚间政治/练习
    )

    private fun partitionIntoSubjectTracks(
        books: List<String>,
        problemSets: List<String>,
        videoLectures: List<String>
    ): List<SubjectTrack> {
        val tracks = mutableListOf<SubjectTrack>()
        val allItems = (books + problemSets + videoLectures)

        // Helper to extract subject tag from string like "[考研英一] 考研英语大纲..."
        fun detectSubject(s: String): String? {
            val trimmed = s.trim()
            if (trimmed.startsWith("[") && trimmed.contains("]")) {
                return trimmed.substringAfter("[").substringBefore("]").trim()
            }
            val lower = trimmed.lowercase(Locale.ROOT)
            return when {
                lower.contains("英一") || lower.contains("英语") || lower.contains("english") -> "考研英一"
                lower.contains("408") || lower.contains("王道") || lower.contains("计算机") || lower.contains("数据结构") || lower.contains("组成原理") || lower.contains("计组") || lower.contains("操作系统") || lower.contains("计网") -> "408"
                lower.contains("数一") || lower.contains("高数") || lower.contains("线代") || lower.contains("概率") || lower.contains("数学") || lower.contains("武忠祥") || lower.contains("武钟祥") || lower.contains("660") -> "考研数一"
                lower.contains("政治") || lower.contains("肖秀荣") || lower.contains("1000题") || lower.contains("肖1000") || lower.contains("马原") || lower.contains("毛中特") || lower.contains("史纲") -> "政治"
                lower.contains("日语") || lower.contains("203") || lower.contains("高分专项突破") -> "日语"
                lower.contains("现代汉语") || lower.contains("现汉") || lower.contains("邢福义") -> "现代汉语"
                lower.contains("古代汉语") || lower.contains("古汉") || lower.contains("郭锡良") || lower.contains("王力") -> "古代汉语"
                lower.contains("文化要略") || lower.contains("对外汉语") || lower.contains("外国文化史") || lower.contains("冲关宝典") || lower.contains("汉硕") || lower.contains("汉语国际教育") || lower.contains("国际中文") -> "汉语国际教育"
                else -> null
            }
        }

        // Collect distinct subject names
        val detectedNames = allItems.mapNotNull { detectSubject(it) }.distinct()
        if (detectedNames.size < 2) {
            return emptyList()
        }

        detectedNames.forEach { subjectName ->
            fun matches(item: String): Boolean {
                val found = detectSubject(item)
                return found != null && found.equals(subjectName, ignoreCase = true)
            }

            val subBooks = books.filter { matches(it) }
            val subProblems = problemSets.filter { matches(it) }
            val subLectures = videoLectures.filter { matches(it) }

            val slot = when {
                subjectName.contains("英") || subjectName.contains("日") || subjectName.contains("古") || subjectName.contains("文化") -> 1 // 晨间语言记忆与文化常识
                subjectName.contains("数") || subjectName.contains("408") -> 2 // 黄金数理与系统逻辑
                subjectName.contains("现") || subjectName.contains("汉语国际教育") || subjectName.contains("专业") -> 3 // 下午专业大题攻坚
                else -> 4 // 晚间选择题刷题与政治错题复盘
            }

            tracks.add(
                SubjectTrack(
                    name = subjectName,
                    books = if (subBooks.isNotEmpty()) subBooks else listOf("[$subjectName] 核心理论讲义"),
                    problemSets = if (subProblems.isNotEmpty()) subProblems else listOf("[$subjectName] 经典题型专项训练"),
                    videoLectures = subLectures,
                    prioritySlot = slot
                )
            )
        }

        return tracks
    }

    /**
     * Generates Stage-Based Monthly Book Clearance Milestones.
     * Divides the entire exam preparation journey into structured monthly phases with specific book clearance deliverables.
     */
    fun generateMonthlyPhases(
        totalDays: Int,
        startDate: Date,
        books: List<String>,
        problemSets: List<String>,
        parsedMaterials: List<com.example.model.ParsedAcademicMaterial> = emptyList()
    ): List<com.example.model.PhaseMilestone> {
        val totalMonths = maxOf(1, (totalDays + 29) / 30)
        val phases = mutableListOf<com.example.model.PhaseMilestone>()

        for (m in 1..totalMonths) {
            val startDay = (m - 1) * 30 + 1
            val endDay = minOf(totalDays, m * 30)
            val dateRange = "${formatDateOffset(startDate, startDay)} ~ ${formatDateOffset(startDate, endDay)}"
            val progressRatio = m.toFloat() / totalMonths

            val (title, coreObjective, deliverables) = when {
                progressRatio <= 0.35f -> Triple(
                    "第 $m 个月 · 基础筑基与核心教材第一轮扫盲",
                    "深研所有已选学科的基石理论教材与核心概念，建立完整各学科知识骨架，完成课后基线习题与例题。",
                    listOf(
                        "全学科核心思维导图与知识骨架绘制完毕，不留夹生概念",
                        "基础客观选择题与核心例题正确率稳定在 70% 以上",
                        "沉淀第一批高频易错知识点并录入艾宾浩斯复习流"
                    )
                )
                progressRatio <= 0.65f -> Triple(
                    "第 $m 个月 · 核心分值重点突破与进阶题库强化",
                    "攻克高分值重难点科目（系统架构/线代概率/语言学深度分析），全面进阶中高难度真题分类题型。",
                    listOf(
                        "各学科核心大题答题规范模板成型，熟练写出推导与算法",
                        "中高阶专项题库刷题完成率达 85% 以上",
                        "半程模拟测试评估达标，梳理学科间知识交叉网络"
                    )
                )
                progressRatio <= 0.88f -> Triple(
                    "第 $m 个月 · 历年真题分类超精解与大题解题速度突破",
                    "刷透历年分类真题，攻克算法手写、综合证明大题与长篇阅读，提升限时答题速度与抗压应试技巧。",
                    listOf(
                        "近 10-15 年统考真题无死角完成一刷与分类归纳",
                        "综合证明大题与算法代码手写正确率达 80%",
                        "完成至少 2 轮艾宾浩斯滚动错题闭环，消灭典型题型盲区"
                    )
                )
                else -> Triple(
                    "第 $m 个月 · 考前终极全真计时模考与终极背诵闭环",
                    "严格按考研实战作息全真计时模考，强化主观题与大题背诵默写，错题本全量清零。",
                    listOf(
                        "严格按照考场作息完成 5-8 套全真模考套卷并精细批改",
                        "主观题核心考点与必背模板全量默写通关",
                        "考前全科错题集全部清零，稳定考试心态"
                    )
                )
            }

            // Generate concrete book requirements for this month informed by parsed materials if available
            val targetBooks = if (parsedMaterials.isNotEmpty()) {
                parsedMaterials.map { mat ->
                    val totalCh = mat.readingVolume.totalChapters
                    val chPerMonth = maxOf(1, (totalCh + totalMonths - 1) / totalMonths)
                    val chStart = ((m - 1) * chPerMonth) + 1
                    val chEnd = minOf(totalCh, m * chPerMonth)
                    val pStart = mat.readingVolume.chapters.getOrNull(chStart - 1)?.startPage ?: 1
                    val pEnd = mat.readingVolume.chapters.getOrNull(chEnd - 1)?.endPage ?: (mat.readingVolume.totalPages * m / totalMonths)
                    val scope = "结项研读第 $chStart-$chEnd 章 (p.$pStart - p.$pEnd，体量共 ${pEnd - pStart + 1} 页 · ${mat.contentDensity.densityTier})"
                    val problems = "完成配套例题与习题约 ${(mat.readingVolume.estimatedProblemsCount * chPerMonth / maxOf(1, totalCh))} 道"
                    com.example.model.PhaseBookRequirement(
                        subject = mat.subject,
                        bookTitle = mat.fileName,
                        targetScope = scope,
                        associatedProblems = problems,
                        status = if (m == 1) "进行中" else "未开始"
                    )
                }
            } else {
                books.map { book ->
                    val (scope, problems) = when {
                        progressRatio <= 0.35f -> Pair(
                            "通读精解前半部核心章节（经典算法/基础定理与概念）",
                            "完成配套课后习题与基础选择题"
                        )
                        progressRatio <= 0.65f -> Pair(
                            "攻坚精读后半部重难点章节（系统架构/综合大题专题）",
                            "完成强化讲义拔高习题，专项大题手写训练"
                        )
                        progressRatio <= 0.88f -> Pair(
                            "全书知识点串联与历年真题分类汇编超精解",
                            "完成近 10 年对应题型真题，错题反复重做"
                        )
                        else -> Pair(
                            "考前核心考点速记与终极模拟卷大题定位查漏",
                            "全套模拟卷限时演练，攻克考前压轴大题"
                        )
                    }

                    com.example.model.PhaseBookRequirement(
                        subject = extractSubjectFromBook(book),
                        bookTitle = book,
                        targetScope = scope,
                        associatedProblems = problems,
                        status = if (m == 1) "进行中" else "未开始"
                    )
                }
            }

            phases.add(
                com.example.model.PhaseMilestone(
                    phaseId = "phase_month_$m",
                    phaseType = "MONTH",
                    phaseNumber = m,
                    title = title,
                    timeRange = "第 $startDay-$endDay 天 · $dateRange",
                    targetBooks = targetBooks,
                    coreObjective = coreObjective,
                    deliverables = deliverables,
                    isCompleted = false
                )
            )
        }
        return phases
    }

    /**
     * Generates Stage-Based Weekly Book Clearance Milestones.
     * Each week has explicit book chapters to complete and concrete clearance checklists.
     */
    fun generateWeeklyPhases(
        totalDays: Int,
        startDate: Date,
        books: List<String>,
        problemSets: List<String>,
        parsedMaterials: List<com.example.model.ParsedAcademicMaterial> = emptyList()
    ): List<com.example.model.PhaseMilestone> {
        val totalWeeks = maxOf(1, (totalDays + 6) / 7)
        val phases = mutableListOf<com.example.model.PhaseMilestone>()

        for (w in 1..totalWeeks) {
            val startDay = (w - 1) * 7 + 1
            val endDay = minOf(totalDays, w * 7)
            val dateRange = "${formatDateOffset(startDate, startDay)} ~ ${formatDateOffset(startDate, endDay)}"
            val weekProgress = w.toFloat() / totalWeeks

            val weekTitle = "第 $w 周 · " + when {
                w == 1 -> "备考启航与各科基石起步"
                w == totalWeeks -> "考前终极临考状态调适与盲区扫雷"
                w % 4 == 0 -> "月度阶段综合验收与错题大盘点"
                weekProgress <= 0.4f -> "基础各章节精读推进"
                weekProgress <= 0.75f -> "强化解题技巧与重点专题攻关"
                else -> "真题实战与综合套卷冲刺"
            }

            val weekObjective = "本周目标：严格按作息执行，按期啃完指定书目章节，周日进行周度错题清零与自我复盘。"

            val deliverables = listOf(
                "完成本周指定章节研读与配套习题打卡",
                "周日完成错题集梳理，标注错误成因并录入艾宾浩斯复习矩阵",
                "本周有效净学时达成率 > 85%"
            )

            val targetBooks = if (parsedMaterials.isNotEmpty()) {
                parsedMaterials.map { mat ->
                    val totalCh = mat.readingVolume.totalChapters
                    val chIdx = (w - 1) % maxOf(1, totalCh)
                    val chapter = mat.readingVolume.chapters.getOrNull(chIdx) ?: mat.readingVolume.chapters.first()
                    val pSpan = if (chapter.pageCount > 0) "(p.${chapter.startPage}-p.${chapter.endPage}, 共${chapter.pageCount}页)" else "(精读章节)"
                    val scope = "研读 ${chapter.title} $pSpan · 【${mat.contentDensity.densityTier}】"
                    com.example.model.PhaseBookRequirement(
                        subject = mat.subject,
                        bookTitle = mat.fileName,
                        targetScope = scope,
                        associatedProblems = "配套习题演练 ${minOf(35, chapter.estimatedProblems)} 题",
                        status = if (w == 1) "进行中" else "未开始"
                    )
                }
            } else {
                books.map { book ->
                    val clean = BookLifecycleEngine.cleanBookTitle(book)
                    val prof = BookLifecycleEngine.parseBookProgressFromText(book)
                    val isSprint15 = (prof.targetFinishDays != null && prof.targetFinishDays <= 15)

                    val chapterStart = prof.startChapter + ((w - 1) * 2 % maxOf(1, 14 - prof.startChapter + 1))
                    val chapterEnd = chapterStart + 1
                    val (scope, status) = when {
                        isSprint15 && w <= 2 -> Pair("【15天突击】攻坚研读核心考点与配套题，第2周末全书结项", "15天冲刺中")
                        isSprint15 && w > 2 -> Pair("【已攻克结项】全书已结项归档，时间槽已腾出供追加新书", "已结项归档")
                        weekProgress > 0.85f -> Pair("全书知识盲区回归排查与考前重点速记背诵", if (w == 1) "进行中" else "排期中")
                        weekProgress > 0.65f -> Pair("对应章节真题分类精解与大题专项突破", if (w == 1) "进行中" else "排期中")
                        else -> Pair("研读第 ${chapterStart}-${chapterEnd} 章并完成核心例题", if (w == 1) "进行中" else "排期中")
                    }

                    com.example.model.PhaseBookRequirement(
                        subject = extractSubjectFromBook(clean),
                        bookTitle = clean,
                        targetScope = scope,
                        associatedProblems = if (isSprint15 && w > 2) "时间槽已释放" else "配套习题演练 30-50 题",
                        status = status
                    )
                }
            }

            phases.add(
                com.example.model.PhaseMilestone(
                    phaseId = "phase_week_$w",
                    phaseType = "WEEK",
                    phaseNumber = w,
                    title = weekTitle,
                    timeRange = "第 $startDay-$endDay 天 · $dateRange",
                    targetBooks = targetBooks,
                    coreObjective = weekObjective,
                    deliverables = deliverables,
                    isCompleted = false
                )
            )
        }
        return phases
    }

    private fun formatDateOffset(startDate: Date, dayOffset: Int): String {
        val c = Calendar.getInstance().apply { time = startDate }
        c.add(Calendar.DAY_OF_YEAR, dayOffset - 1)
        return dateFormat.format(c.time)
    }

    private fun extractSubjectFromBook(book: String): String {
        if (book.startsWith("[") && book.contains("]")) {
            return book.substringAfter("[").substringBefore("]").trim()
        }
        val lower = book.lowercase(Locale.ROOT)
        return when {
            lower.contains("408") || lower.contains("王道") || lower.contains("数据结构") || lower.contains("操作系统") || lower.contains("组成原理") || lower.contains("计网") -> "408"
            lower.contains("数一") || lower.contains("高数") || lower.contains("线代") || lower.contains("概率") || lower.contains("武忠祥") || lower.contains("武钟祥") || lower.contains("660") -> "考研数一"
            lower.contains("政治") || lower.contains("肖秀荣") || lower.contains("1000题") || lower.contains("肖1000") -> "政治"
            lower.contains("现代汉语") || lower.contains("现汉") || lower.contains("邢福义") -> "现代汉语"
            lower.contains("古代汉语") || lower.contains("古汉") || lower.contains("郭锡良") || lower.contains("王力") -> "古代汉语"
            lower.contains("文化要略") || lower.contains("对外汉语") || lower.contains("外国文化史") || lower.contains("冲关宝典") || lower.contains("汉硕") || lower.contains("汉语国际教育") || lower.contains("国际中文") -> "汉语国际教育"
            lower.contains("日语") || lower.contains("203") || lower.contains("高分专项突破") -> "日语"
            lower.contains("英语") || lower.contains("词汇") || lower.contains("长难句") -> "考研英一"
            else -> "专业课"
        }
    }
}


