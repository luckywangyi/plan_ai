package com.example

import com.example.engine.AcademicMaterialParser
import com.example.engine.AcademicPlanningEngine
import com.example.model.StudyRawInputs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class AcademicMaterialParserTest {

    @Test
    fun testParseTextContentExtractsChaptersAndVolume() {
        val sampleOutline = """
            第1章 线性表与双向链表 (p.1 - p.40) 【例题】20道
            第2章 栈与队列的深度实现 (p.41 - p.95) 【例题】25道
            第3章 二叉树遍历与平衡树 (p.96 - p.160) 【习题】35道
            第4章 图的邻接表与Dijkstra算法 (p.161 - p.230) 【算法题】30道
        """.trimIndent()

        val parsed = AcademicMaterialParser.parseTextContent(
            fileName = "408数据结构讲义.txt",
            content = sampleOutline,
            subjectHint = "408"
        )

        assertEquals("408数据结构讲义.txt", parsed.fileName)
        assertEquals("TEXT", parsed.sourceType)
        assertEquals(4, parsed.readingVolume.totalChapters)
        assertTrue(parsed.readingVolume.totalPages >= 230)
        assertTrue(parsed.readingVolume.estimatedProblemsCount >= 100)
        assertTrue(parsed.contentDensity.cognitiveLoadIndex > 1.0f)
        assertNotNull(parsed.contentDensity.densityTier)
    }

    @Test
    fun testAcademicPlanningEngineWithParsedMaterials() {
        val sampleOutline = """
            第1章 极限与连续 (p.1 - p.35) 【习题】20道
            第2章 导数与微分应用 (p.36 - p.80) 【习题】25道
        """.trimIndent()

        val parsed = AcademicMaterialParser.parseTextContent(
            fileName = "数学一高等数学核心讲义.txt",
            content = sampleOutline,
            subjectHint = "数学一"
        )

        val rawInputs = StudyRawInputs(
            examTitle = "数学一统考",
            targetDate = "2026-12-25",
            dailyHoursAvailable = 6.0f,
            books = listOf("数学一高等数学核心讲义.txt"),
            problemSets = listOf("高数精选1000题"),
            videoLectures = emptyList(),
            parsedMaterials = listOf(parsed)
        )

        val contract = AcademicPlanningEngine.generateRigorousPlan("Profile_A", rawInputs)

        assertNotNull(contract)
        assertEquals(1, contract.parsedMaterials.size)
        // Verify monthly and weekly phases reflect the parsed material
        assertTrue(contract.monthlyPhases.isNotEmpty())
        assertTrue(contract.weeklyPhases.isNotEmpty())
        val firstDay = contract.dailySchedule.first()
        // Daily task should reference the extracted chapters and density
        val readingTask = firstDay.tasks.firstOrNull { it.taskType == "reading" }
        assertNotNull(readingTask)
        assertTrue(readingTask!!.scope.contains("第1章") || readingTask.scope.contains("研读"))
    }

    @Test
    fun testBookProgressParsingAndCatalogResolution() {
        val entry = com.example.engine.AcademicBookCatalog.resolveBook("王道数据结构")
        assertTrue(entry.canonicalTitle.contains("数据结构"))
        assertEquals(8, entry.totalChapters)
        assertTrue(entry.chapters.any { it.title.contains("树与二叉树") })

        val progressA = com.example.engine.BookLifecycleEngine.parseBookProgressFromText("王道数据结构(学到第3章)", "")
        assertEquals(3, progressA.startChapter)
        assertEquals("从第3章起 (已过2章)", progressA.stageLabel)

        val progressB = com.example.engine.BookLifecycleEngine.parseBookProgressFromText("操作系统", "操作系统已学完前4章")
        assertEquals(5, progressB.startChapter)
        assertEquals(48, progressB.completedPercent)

        val rawInputs = StudyRawInputs(
            examTitle = "408计算机统考",
            targetDate = "2026-12-25",
            dailyHoursAvailable = 6.0f,
            books = listOf("王道数据结构(学到第3章)"),
            problemSets = emptyList(),
            videoLectures = emptyList()
        )

        val contract = AcademicPlanningEngine.generateRigorousPlan("Profile_A", rawInputs)
        assertNotNull(contract)
        val firstDayReading = contract.dailySchedule.first().tasks.firstOrNull { it.material.contains("数据结构") }
        assertNotNull(firstDayReading)
        // Should start from Chapter 3
        assertTrue(firstDayReading!!.scope.contains("3"))
    }
}
