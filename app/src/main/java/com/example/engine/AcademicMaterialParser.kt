package com.example.engine

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.model.MaterialChapterInfo
import com.example.model.MaterialContentDensity
import com.example.model.MaterialReadingVolume
import com.example.model.ParsedAcademicMaterial
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.regex.Pattern

object AcademicMaterialParser {

    private const val TAG = "AcademicMaterialParser"

    /**
     * Parses a text or markdown string representing academic book outlines, sample chapters, or full texts.
     */
    fun parseTextContent(
        fileName: String,
        content: String,
        subjectHint: String = ""
    ): ParsedAcademicMaterial {
        val totalCharacters = content.length.toLong()
        val detectedSubject = if (subjectHint.isNotBlank()) subjectHint else inferSubjectFromContent(fileName, content)

        // 1. Extract Chapters
        val extractedChapters = extractChaptersFromText(content)

        val maxEndPage = extractedChapters.map { it.endPage }.maxOrNull() ?: 0
        val totalPages = if (maxEndPage > 0) {
            maxEndPage
        } else {
            // Standard Chinese academic print has roughly 650-800 characters per page
            maxOf(1, (totalCharacters / 700).toInt())
        }

        // 3. Calculate Content Density Metrics
        val contentDensity = calculateContentDensity(content, totalPages, detectedSubject)

        // 4. Align Chapters with calculated pages if missing
        val finalChapters = if (extractedChapters.isNotEmpty()) {
            distributeChapterPagesIfMissing(extractedChapters, totalPages, contentDensity.cognitiveLoadIndex)
        } else {
            generateSyntheticChapters(totalPages, detectedSubject, contentDensity.cognitiveLoadIndex)
        }

        val estimatedProblemsCount = finalChapters.sumOf { it.estimatedProblems }.coerceAtLeast(
            countOccurrencesRegex(content, "(?:例题|习题|思考题|单选|多选|简答题|真题|练习题|练\\s*习)")
        )

        val readingVolume = MaterialReadingVolume(
            totalPages = totalPages,
            totalCharacters = totalCharacters,
            totalChapters = finalChapters.size,
            estimatedProblemsCount = estimatedProblemsCount,
            chapters = finalChapters
        )

        val summary = generateAcademicSummary(fileName, detectedSubject, readingVolume, contentDensity)
        val paceTip = "建议以每日 ${maxOf(8, (totalPages / 30).coerceIn(10, 35))} 页节奏精读，单页预留 ${String.format("%.1f", contentDensity.recommendedMinutesPerPage)} 分钟"

        return ParsedAcademicMaterial(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            sourceType = "TEXT",
            subject = detectedSubject,
            readingVolume = readingVolume,
            contentDensity = contentDensity,
            aiSummary = summary,
            suggestedDailyPace = paceTip
        )
    }

    /**
     * Parses an imported PDF document via Android native PdfRenderer and text stream inspection.
     */
    fun parsePdfDocument(
        context: Context,
        uri: Uri,
        fileName: String,
        subjectHint: String = ""
    ): ParsedAcademicMaterial {
        var pageCount = 0
        val textSnippetBuilder = StringBuilder()

        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                pfd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    pageCount = renderer.pageCount
                    renderer.close()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "PdfRenderer could not read total pages: ${e.message}")
        }

        // Fallback or read text stream from PDF if text stream is embedded
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                var linesRead = 0
                while (linesRead < 2000) {
                    val line = reader.readLine() ?: break
                    // Filter out binary pdf headers and collect printable academic text
                    val printable = line.filter { it.isLetterOrDigit() || it.isWhitespace() || it in "()[]{}<>+-*/=,.;:!?" }
                    if (printable.length > 5) {
                        textSnippetBuilder.append(printable).append("\n")
                        linesRead++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract stream snippet: ${e.message}")
        }

        val snippet = textSnippetBuilder.toString()
        val detectedSubject = if (subjectHint.isNotBlank()) subjectHint else inferSubjectFromContent(fileName, snippet)

        val totalPages = if (pageCount > 0) pageCount else 320 // realistic default academic textbook pages
        val contentDensity = calculateContentDensity(snippet, totalPages, detectedSubject)

        // Try extracting chapter outlines from snippet, or synthesize realistic chapter divisions
        val extractedChapters = extractChaptersFromText(snippet)
        val finalChapters = if (extractedChapters.size >= 3) {
            distributeChapterPagesIfMissing(extractedChapters, totalPages, contentDensity.cognitiveLoadIndex)
        } else {
            generateSyntheticChapters(totalPages, detectedSubject, contentDensity.cognitiveLoadIndex)
        }

        val readingVolume = MaterialReadingVolume(
            totalPages = totalPages,
            totalCharacters = totalPages * 720L,
            totalChapters = finalChapters.size,
            estimatedProblemsCount = finalChapters.sumOf { it.estimatedProblems },
            chapters = finalChapters
        )

        val summary = generateAcademicSummary(fileName, detectedSubject, readingVolume, contentDensity)
        val paceTip = "PDF全书 ${totalPages}页，属于【${contentDensity.densityTier}】，建议单页研读 ${String.format("%.1f", contentDensity.recommendedMinutesPerPage)} 分钟"

        return ParsedAcademicMaterial(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            sourceType = "PDF",
            subject = detectedSubject,
            readingVolume = readingVolume,
            contentDensity = contentDensity,
            aiSummary = summary,
            suggestedDailyPace = paceTip
        )
    }

    /**
     * Calculates mathematical formula density, code algorithm density, and concept density.
     */
    fun calculateContentDensity(
        text: String,
        totalPages: Int,
        subject: String
    ): MaterialContentDensity {
        val sample = if (text.length > 50000) text.substring(0, 50000) else text
        val len = maxOf(1, sample.length)

        // 1. Formula & Mathematical symbols density
        val mathPattern = Pattern.compile("(?i)[∫∑∏√lim∂αβγθλμσωΔ±×÷≤≥≠≈∞]|[a-z]\\^\\d|\\b(dx|dy|dt|sin|cos|tan|matrix|det|rank|eigenvalue)\\b")
        val mathMatches = countMatches(mathPattern, sample)
        val formulaRawDensity = (mathMatches.toFloat() / len) * 800f
        val formulaDensity = formulaRawDensity.coerceIn(0f, 1f)

        // 2. Code & Algorithm density
        val codePattern = Pattern.compile("(?i)\\b(class|struct|void|int|float|double|return|for|while|if|malloc|sizeof|nullptr|NULL|typedef|enum|O\\(n\\)|O\\(log|stack|queue|tree|graph)\\b|->|==|!=|\\+\\+|--")
        val codeMatches = countMatches(codePattern, sample)
        val codeRawDensity = (codeMatches.toFloat() / len) * 750f
        val codeDensity = codeRawDensity.coerceIn(0f, 1f)

        // 3. Concept & Axiom density
        val conceptPattern = Pattern.compile("(?i)(?:定义|定理|引理|推论|准则|性质|公理|充分必要条件|考纲|核心考点|机理|架构|协议|模型|特征|假说|规范)")
        val conceptMatches = countMatches(conceptPattern, sample)
        val conceptRawDensity = (conceptMatches.toFloat() / len) * 600f
        val conceptDensity = conceptRawDensity.coerceIn(0f, 1f)

        // 4. Exercise & Problem density
        val exercisePattern = Pattern.compile("(?i)(?:【例题】|【习题】|【单选】|【多选】|【真题】|【练习】|习题|思考题|第\\s*\\d+\\s*题|\\(\\d+\\))")
        val exerciseMatches = countMatches(exercisePattern, sample)
        val exerciseDensity = ((exerciseMatches.toFloat() / len) * 500f).coerceIn(0f, 1f)

        // 5. Subject Bias Baseline
        val subjectBaseLoad = when {
            subject.contains("408") -> 1.75f
            subject.contains("数一") || subject.contains("数学") -> 1.85f
            subject.contains("英一") || subject.contains("英语") -> 1.25f
            subject.contains("政治") -> 1.15f
            subject.contains("汉语") -> 1.30f
            subject.contains("日语") -> 1.25f
            else -> 1.35f
        }

        // Weighted cognitive load index (0.8 ~ 2.5)
        val dynamicLoad = 1.0f + (formulaDensity * 0.9f) + (codeDensity * 0.8f) + (conceptDensity * 0.6f) + (exerciseDensity * 0.4f)
        val cognitiveLoadIndex = ((subjectBaseLoad * 0.6f) + (dynamicLoad * 0.4f)).coerceIn(0.9f, 2.4f)

        val (tier, paceMinutes) = when {
            cognitiveLoadIndex >= 1.75f -> "极高密度·硬核演算" to (5.0f * cognitiveLoadIndex / 1.75f)
            cognitiveLoadIndex >= 1.45f -> "高密度·推导演练" to (3.8f * cognitiveLoadIndex / 1.45f)
            cognitiveLoadIndex >= 1.20f -> "中密度·概念理解" to (2.8f * cognitiveLoadIndex / 1.20f)
            else -> "标准密度·常规识记" to 2.2f
        }

        val minutesPerChapter = (paceMinutes * (totalPages.toFloat() / 10f).coerceAtLeast(15f)).toInt()

        return MaterialContentDensity(
            formulaDensity = (formulaDensity * 100).toInt() / 100f,
            codeDensity = (codeDensity * 100).toInt() / 100f,
            conceptDensity = (conceptDensity * 100).toInt() / 100f,
            exerciseDensity = (exerciseDensity * 100).toInt() / 100f,
            cognitiveLoadIndex = (cognitiveLoadIndex * 100).toInt() / 100f,
            densityTier = tier,
            recommendedMinutesPerPage = (paceMinutes * 10).toInt() / 10f,
            recommendedMinutesPerChapter = minutesPerChapter
        )
    }

    private fun extractChaptersFromText(text: String): List<MaterialChapterInfo> {
        val list = mutableListOf<MaterialChapterInfo>()
        val lines = text.lines()
        val chapterPattern = Pattern.compile("(?i)^(?:第[一二三四五六七八九十0-9]+[章讲篇部单元]|Chapter\\s*\\d+|Unit\\s*\\d+|\\d+\\.\\d+)\\s*(.*)")
        val pageRangePattern = Pattern.compile("(?i)(?:p\\.?|页码|页)?\\s*(\\d+)\\s*[-~至到]\\s*(?:p\\.?|页码|页)?\\s*(\\d+)")
        val problemMatcher = Pattern.compile("(?:例题|习题|思考题|题|算法题|真题)?\\s*(\\d+)\\s*道")

        var chapterIdx = 1
        lines.forEach { line ->
            val trimmed = line.trim()
            val matcher = chapterPattern.matcher(trimmed)
            if (matcher.find()) {
                val title = trimmed
                var startP = 0
                var endP = 0

                val pageMatcher = pageRangePattern.matcher(trimmed)
                if (pageMatcher.find()) {
                    startP = pageMatcher.group(1)?.toIntOrNull() ?: 0
                    endP = pageMatcher.group(2)?.toIntOrNull() ?: 0
                }

                val probMatcher = problemMatcher.matcher(trimmed)
                val explicitProblems = if (probMatcher.find()) {
                    probMatcher.group(1)?.toIntOrNull() ?: 0
                } else 0

                val pageCount = if (endP > startP) endP - startP + 1 else 0
                val calcProblems = if (explicitProblems > 0) explicitProblems else (pageCount * 1.5f).toInt().coerceAtLeast(15)

                list.add(
                    MaterialChapterInfo(
                        chapterIndex = chapterIdx++,
                        title = title,
                        startPage = startP,
                        endPage = endP,
                        pageCount = pageCount,
                        estimatedProblems = calcProblems,
                        densityLevel = if (title.contains("算法") || title.contains("积分") || title.contains("流水线")) "高难" else "中密"
                    )
                )
            }
        }
        return list
    }

    private fun distributeChapterPagesIfMissing(
        chapters: List<MaterialChapterInfo>,
        totalPages: Int,
        loadIndex: Float
    ): List<MaterialChapterInfo> {
        val totalKnownPages = chapters.filter { it.pageCount > 0 }.sumOf { it.pageCount }
        val missingCount = chapters.count { it.pageCount <= 0 }

        if (missingCount == 0) return chapters

        val remainingPages = (totalPages - totalKnownPages).coerceAtLeast(missingCount * 10)
        val avgPages = maxOf(12, remainingPages / missingCount)

        var runningPage = 1
        return chapters.mapIndexed { idx, ch ->
            if (ch.pageCount > 0) {
                runningPage = ch.endPage + 1
                ch
            } else {
                val start = runningPage
                val pCount = avgPages
                val end = start + pCount - 1
                runningPage = end + 1
                val problems = (pCount * (if (loadIndex > 1.5f) 2.2f else 1.2f)).toInt().coerceAtLeast(15)
                ch.copy(
                    startPage = start,
                    endPage = end,
                    pageCount = pCount,
                    estimatedProblems = problems
                )
            }
        }
    }

    private fun generateSyntheticChapters(
        totalPages: Int,
        subject: String,
        loadIndex: Float
    ): List<MaterialChapterInfo> {
        val chapterTitles = when {
            subject.contains("408") -> listOf(
                "第1章 绪论与算法时空复杂度分析",
                "第2章 线性表与链表核心实战",
                "第3章 栈、队列与递归模型",
                "第4章 树与二叉树高频重难点",
                "第5章 图论算法 (最短路径/拓扑/最小生成树)",
                "第6章 查找算法与哈希散列表",
                "第7章 内部排序算法性能大比拼",
                "第8章 计算机系统概述与数据表示",
                "第9章 运算方法与算术逻辑单元",
                "第10章 指令系统与寻址方式",
                "第11章 CPU架构与指令流水线冲突",
                "第12章 存储系统与Cache虚拟内存"
            )
            subject.contains("数一") || subject.contains("数学") -> listOf(
                "第1章 函数、极限与连续性突破",
                "第2章 一元函数微分学与中值定理",
                "第3章 一元函数积分学与反常积分",
                "第4章 微分方程与差分方程求解",
                "第5章 多元函数微分学核心考点",
                "第6章 二重积分与三重积分计算技巧",
                "第7章 曲线与曲面积分 (格林/高斯定理)",
                "第8章 无穷级数敛散性判别与展开",
                "第9章 行列式与矩阵初等变换",
                "第10章 向量组线性相关性与向量空间",
                "第11章 线性方程组解的结构与特征值",
                "第12章 随机变量及其分布律与数理统计"
            )
            subject.contains("英一") || subject.contains("英语") -> listOf(
                "第1讲 考研词汇高频词根词缀与派生记忆",
                "第2讲 语法长难句主干剥离与修饰从句",
                "第3讲 历年真题核心细节题与主旨题解密",
                "第4讲 命题人逻辑陷阱与推断题攻坚",
                "第5讲 新题型七选五与排序专项速破",
                "第6讲 英译汉长难句拆分翻译技巧",
                "第7讲 小作文应用文规范格式与功能句",
                "第8讲 大作文图画图表论说文高分表达"
            )
            subject.contains("政治") -> listOf(
                "第1章 马克思主义哲学核心唯物论与辩证法",
                "第2章 认识论与唯物史观深度解析",
                "第3章 马克思主义政治经济学剩余价值论",
                "第4章 毛泽东思想及其历史地位",
                "第5章 邓小平理论、“三个代表”与科学发展观",
                "第6章 习近平新时代中国特色社会主义思想概论",
                "第7章 中国近现代史纲要重大历史节点",
                "第8章 思想道德与法治素养",
                "第9章 当代世界经济与政治热点专题"
            )
            else -> listOf(
                "第1章 基础概念与知识框架导论",
                "第2章 核心考点第一轮精研",
                "第3章 重难点公式与定理推导",
                "第4章 典型例题解题方法论",
                "第5章 综合习题与专项强化",
                "第6章 历年高频真题深度剖析",
                "第7章 易错混淆考点横向对比",
                "第8章 考前冲刺模拟与应试技巧"
            )
        }

        val chapterPages = maxOf(15, totalPages / chapterTitles.size)
        var curStart = 1

        return chapterTitles.mapIndexed { index, title ->
            val isLast = index == chapterTitles.lastIndex
            val end = if (isLast) totalPages else curStart + chapterPages - 1
            val pCount = end - curStart + 1
            val info = MaterialChapterInfo(
                chapterIndex = index + 1,
                title = title,
                startPage = curStart,
                endPage = end,
                pageCount = pCount,
                estimatedProblems = (pCount * (if (loadIndex > 1.5f) 2.0f else 1.2f)).toInt().coerceAtLeast(15),
                densityLevel = if (loadIndex > 1.6f) "高难" else "中密"
            )
            curStart = end + 1
            info
        }
    }

    private fun inferSubjectFromContent(fileName: String, content: String): String {
        val combo = "$fileName $content"
        return when {
            combo.contains("408") || combo.contains("数据结构") || combo.contains("计组") || combo.contains("操作系统") || combo.contains("计算机网络") -> "408"
            combo.contains("数一") || combo.contains("数学一") || combo.contains("高等数学") || combo.contains("高数") || combo.contains("微积分") || combo.contains("线性代数") -> "考研数一"
            combo.contains("英一") || combo.contains("英语一") || combo.contains("考研英语") || combo.contains("词汇") || combo.contains("长难句") -> "考研英一"
            combo.contains("政治") || combo.contains("肖秀荣") || combo.contains("徐涛") || combo.contains("马原") || combo.contains("毛中特") -> "政治"
            combo.contains("日语") || combo.contains("203") || combo.contains("N1") || combo.contains("N2") -> "日语"
            combo.contains("古代汉语") || combo.contains("现汉") || combo.contains("古代文选") || combo.contains("现代汉语") -> "现代汉语和古代汉语"
            else -> "专业课核心教材"
        }
    }

    private fun generateAcademicSummary(
        fileName: String,
        subject: String,
        volume: MaterialReadingVolume,
        density: MaterialContentDensity
    ): String {
        return buildString {
            append("【AI 学术材料解析报告】\n")
            append("• 材料名称: $fileName (${subject})\n")
            append("• 总阅读体量: ${volume.totalPages} 页 / 约 ${volume.totalCharacters / 1000}k 字 / ${volume.totalChapters} 大核心章节\n")
            append("• 配套习题量: 预估 ${volume.estimatedProblemsCount} 道精选题目\n")
            append("• 学术密度评定: ${density.densityTier} (认知负荷指数: ${density.cognitiveLoadIndex})\n")
            append("• 细分特征: 公式密度 ${(density.formulaDensity * 100).toInt()}% | 代码算法密度 ${(density.codeDensity * 100).toInt()}% | 核心概念密度 ${(density.conceptDensity * 100).toInt()}%\n")
            append("• 建议研读配速: 单页建议用时 ${String.format("%.1f", density.recommendedMinutesPerPage)} 分钟，单章平均预算约 ${density.recommendedMinutesPerChapter} 分钟。")
        }
    }

    private fun countMatches(pattern: Pattern, text: String): Int {
        val matcher = pattern.matcher(text)
        var count = 0
        while (matcher.find()) {
            count++
        }
        return count
    }

    private fun countOccurrencesRegex(text: String, regex: String): Int {
        val p = Pattern.compile(regex)
        return countMatches(p, text)
    }
}
