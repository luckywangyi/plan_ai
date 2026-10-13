package com.example.engine

import com.example.model.BookProgressProfile
import com.example.model.StudyRawInputs
import java.util.regex.Pattern

/**
 * Engine for managing book learning progress, phased pacing (e.g. 15-day sprint),
 * natural language extraction, and automatic completion elimination/archiving.
 */
object BookLifecycleEngine {

    private val COMPLETED_PATTERNS = listOf(
        Pattern.compile("(?:已学完|已读完|已刷完|已结项|已过完|已通关|已做完|已背完|学完了|读完了|搞定了|已结项归档)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:完成度|进度)\\s*(?:100%|已满)", Pattern.CASE_INSENSITIVE)
    )

    private val CHAPTER_START_PATTERNS = listOf(
        Pattern.compile("(?:已)?(?:学到|学至|学了|看到|读到|复习到|目前学到|目前在|进度在|进度至|进度到|当前在|当前已到|当前第?)(?:了)?(?:第)?\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("已(?:学完|完成|过完|读完|做完)(?:前|了第|第)?\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:从)?第\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)(?:开始|起)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("剩余后\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)", Pattern.CASE_INSENSITIVE)
    )

    private val DEGREE_PATTERNS = listOf(
        Pattern.compile("(?:已学完|完成|做|学了?)\\s*(?:一大半|大半)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:已学|完成|做|学了?)\\s*(?:一半|半本)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:完成|已学)\\s*(?:三分之二|2/3)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:完成|已学|进度|已刷)\\s*(\\d{1,3})\\s*%", Pattern.CASE_INSENSITIVE)
    )

    private val TARGET_DAYS_PATTERNS = listOf(
        Pattern.compile("(\\d+)\\s*天(?:内)?(?:学完|刷完|搞定|通关|结项|完成|突击)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("打算\\s*(\\d+)\\s*天(?:学完|搞定|完成)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("目标\\s*(\\d+)\\s*天", Pattern.CASE_INSENSITIVE),
        Pattern.compile("计划\\s*(\\d+)\\s*天", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Cleans display title by removing bracketed natural language annotations.
     */
    fun cleanBookTitle(rawText: String): String {
        return rawText
            .replace(Regex("[(（][^()（）]*(?:已学|学到|学至|学了|看到|读到|复习到|目前|当前|已完|进度|天内|开始|章起|讲起|结项|大半|一半|一轮|强化|冲刺|备考)[^()（）]*[)）]"), "")
            .replace(Regex("\\[(已学完|已结项|15天|突击|一轮精读|刷题强化|冲刺查漏|未开始)\\]"), "")
            .replace(Regex("[(（]\\s*(?:一轮精读|刷题强化|冲刺查漏|未开始|已结项)\\s*[)）]"), "")
            .trim()
    }

    /**
     * Extracts book progress, start chapter, target finish days, and completion status
     * from book title string, user notes, and global custom strategy notes.
     */
    fun parseBookProgressFromText(
        rawBookText: String,
        globalNotes: String = ""
    ): BookProgressProfile {
        val cleanTitle = cleanBookTitle(rawBookText)
        val combinedText = "$rawBookText $globalNotes"

        // 1. Check if completed
        var isCompleted = false
        for (pattern in COMPLETED_PATTERNS) {
            if (pattern.matcher(rawBookText).find()) {
                isCompleted = true
                break
            }
        }

        // 2. Check starting chapter (prioritize user explicit markers: "学到第3章", "学到了第4章", "看到第2讲", etc.)
        var startChapter = 1
        var completedPercent = 0

        // Case A: User explicitly marked current chapter: e.g. "学到第3章", "学到了第4讲", "目前在第5章", "看到第2章", "读到第3讲", "学至第4章", "当前第3章", "进度第2章"
        val mCurrentCh = Pattern.compile(
            "(?:已)?(?:学到|学至|学了|看到|读到|复习到|目前学到|目前在|进度在|进度至|进度到|当前在|当前已到|当前第?)(?:了)?(?:第)?\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)",
            Pattern.CASE_INSENSITIVE
        ).matcher(combinedText)

        if (mCurrentCh.find()) {
            val curCh = mCurrentCh.group(1)?.toIntOrNull() ?: 1
            startChapter = maxOf(1, curCh)
            completedPercent = minOf(90, maxOf(10, (startChapter - 1) * 12))
        } else {
            // Case B: User marked completed prior chapters: e.g. "已学完前3章", "完成前2讲", "学完了第3章", "已学完第2章"
            val mFinishedCh = Pattern.compile(
                "已(?:学完|完成|过完|读完|做完)(?:前|了第|第)?\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)",
                Pattern.CASE_INSENSITIVE
            ).matcher(combinedText)

            if (mFinishedCh.find()) {
                val finishedChs = mFinishedCh.group(1)?.toIntOrNull() ?: 0
                startChapter = finishedChs + 1
                completedPercent = minOf(95, finishedChs * 12)
            } else {
                // Case C: User marked start from: e.g. "从第3章开始", "第4章起", "从第2讲起"
                val mStartFrom = Pattern.compile(
                    "(?:从)?第\\s*(\\d+)\\s*(?:章|讲|单元|篇|节)(?:开始|起)",
                    Pattern.CASE_INSENSITIVE
                ).matcher(combinedText)

                if (mStartFrom.find()) {
                    startChapter = mStartFrom.group(1)?.toIntOrNull() ?: 1
                    completedPercent = minOf(90, maxOf(0, (startChapter - 1) * 12))
                }
            }
        }

        // 3. Check degree / percentage
        if (completedPercent == 0 && !isCompleted) {
            val mPct = Pattern.compile("(?:完成|已学|进度|已刷)\\s*(\\d{1,3})\\s*%").matcher(combinedText)
            if (mPct.find()) {
                completedPercent = mPct.group(1)?.toIntOrNull()?.coerceIn(0, 100) ?: 0
                if (completedPercent >= 100) {
                    isCompleted = true
                } else {
                    startChapter = maxOf(1, (completedPercent / 12) + 1)
                }
            } else if (Pattern.compile("(?:已学完|完成|做|学了?)\\s*(?:一大半|大半)").matcher(combinedText).find()) {
                completedPercent = 65
                startChapter = 6
            } else if (Pattern.compile("(?:已学|完成|做|学了?)\\s*(?:一半|半本)").matcher(combinedText).find()) {
                completedPercent = 50
                startChapter = 5
            } else if (Pattern.compile("(?:完成|已学)\\s*(?:三分之二|2/3)").matcher(combinedText).find()) {
                completedPercent = 66
                startChapter = 7
            }
        }

        if (isCompleted) {
            completedPercent = 100
        }

        // 4. Target days (e.g. 15 天学完)
        var targetFinishDays: Int? = null

        // Check in book text first
        for (pattern in TARGET_DAYS_PATTERNS) {
            val m = pattern.matcher(rawBookText)
            if (m.find()) {
                targetFinishDays = m.group(1)?.toIntOrNull()
                break
            }
        }

        // If not found in book text, check global notes with keyword correlation
        if (targetFinishDays == null && globalNotes.isNotBlank()) {
            val shortKeyword = cleanTitle.take(6).trim('《', '》', '[', ']')
            if (shortKeyword.isNotBlank() && globalNotes.contains(shortKeyword)) {
                for (pattern in TARGET_DAYS_PATTERNS) {
                    val m = pattern.matcher(globalNotes)
                    if (m.find()) {
                        targetFinishDays = m.group(1)?.toIntOrNull()
                        break
                    }
                }
            } else if (globalNotes.contains("15天学完") || globalNotes.contains("十五天学完")) {
                // If the user specified general "15天学完" in custom notes
                if (rawBookText.contains("王道") || rawBookText.contains("660") || rawBookText.contains("讲义")) {
                    targetFinishDays = 15
                }
            }
        }

        val stageLabel = when {
            isCompleted -> "已结项归档"
            startChapter > 1 -> "从第${startChapter}章起 (已过${startChapter - 1}章)"
            targetFinishDays != null && targetFinishDays <= 15 -> "第1阶段·${targetFinishDays}天突击结项"
            targetFinishDays != null -> "专项攻坚·${targetFinishDays}天目标"
            completedPercent >= 50 -> "后半程攻坚·从第${startChapter}章起"
            else -> "全期稳步推进"
        }

        val naturalNote = buildString {
            if (isCompleted) {
                append("已学完结项，自动从每日任务中排除")
            } else {
                if (startChapter > 1) {
                    append("已学至第 $startChapter 章 (跳过前 ${startChapter - 1} 章)")
                } else if (completedPercent > 0) {
                    append("已学完 $completedPercent% (从第 $startChapter 章开始)")
                }
                if (targetFinishDays != null) {
                    if (isNotEmpty()) append("，")
                    append("目标 $targetFinishDays 天内全面学完")
                }
            }
        }

        return BookProgressProfile(
            bookTitle = cleanTitle,
            completedPercent = completedPercent,
            startChapter = startChapter,
            targetFinishDays = targetFinishDays,
            isCompleted = isCompleted,
            stageLabel = stageLabel,
            naturalLanguageNote = naturalNote
        )
    }

    /**
     * Resolves inputs into active books to be planned and archived books to be excluded.
     * Prevents re-planning books that are already completed when new materials are added!
     */
    fun resolveInputsWithProgress(
        rawInputs: StudyRawInputs
    ): ResolvedStudyInputs {
        val existingArchived = rawInputs.archivedCompletedBooks.toMutableSet()
        val activeBooks = mutableListOf<String>()
        val activeProblems = mutableListOf<String>()
        val progressProfiles = mutableListOf<BookProgressProfile>()

        // 1. Process books
        rawInputs.books.forEach { rawBook ->
            val profile = rawInputs.bookProgressProfiles.firstOrNull { 
                it.bookTitle == cleanBookTitle(rawBook) || cleanBookTitle(rawBook).contains(it.bookTitle)
            } ?: parseBookProgressFromText(rawBook, rawInputs.customNotes)

            if (profile.isCompleted || existingArchived.contains(profile.bookTitle)) {
                existingArchived.add(profile.bookTitle)
            } else {
                activeBooks.add(cleanBookTitle(rawBook))
                progressProfiles.add(profile)
            }
        }

        // 2. Process problem sets
        rawInputs.problemSets.forEach { rawProb ->
            val profile = parseBookProgressFromText(rawProb, rawInputs.customNotes)
            if (profile.isCompleted || existingArchived.contains(profile.bookTitle)) {
                existingArchived.add(profile.bookTitle)
            } else {
                activeProblems.add(cleanBookTitle(rawProb))
            }
        }

        return ResolvedStudyInputs(
            activeBooks = activeBooks,
            activeProblemSets = activeProblems,
            archivedCompletedBooks = existingArchived.toList(),
            progressProfiles = progressProfiles
        )
    }
}

data class ResolvedStudyInputs(
    val activeBooks: List<String>,
    val activeProblemSets: List<String>,
    val archivedCompletedBooks: List<String>,
    val progressProfiles: List<BookProgressProfile>
)
