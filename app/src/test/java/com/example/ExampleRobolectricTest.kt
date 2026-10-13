package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.StudySyncJsonParser
import com.example.engine.A4MatrixExporter
import com.example.engine.AcademicPlanningEngine
import com.example.model.ExamPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StudySync", appName)
  }

  @Test
  fun `verify academic planning engine generates 4 milestones and valid contract`() {
    val preset = ExamPresets.presets[0]
    val contract = AcademicPlanningEngine.generateRigorousPlan("Profile_A", preset.rawInputs)

    assertEquals("Profile_A", contract.profileId)
    assertEquals(4, contract.milestones.size)
    assertTrue(contract.dailySchedule.isNotEmpty())
    assertNotNull(contract.adaptiveAdjustments)

    // Verify JSON schema serialization round-trip
    val json = StudySyncJsonParser.toJson(contract)
    val parsed = StudySyncJsonParser.fromJson(json)
    assertNotNull(parsed)
    assertEquals(contract.dailySchedule.size, parsed?.dailySchedule?.size)
    assertNotNull(parsed?.bookAnalysis)
    assertTrue(parsed?.bookAnalysis?.booksAnalyzed?.isNotEmpty() == true)
    assertTrue(parsed?.bookAnalysis?.workloadMetrics?.totalEstimatedHours ?: 0f > 0f)
    assertTrue(parsed?.bookAnalysis?.strategicAdvice?.isNotEmpty() == true)
  }

  @Test
  fun `verify book analysis engine calculates workload and pedagogical advice`() {
    val preset = ExamPresets.presets[1] // 考研计算机 408
    val report = AcademicPlanningEngine.analyzeBooksAndWorkload(preset.rawInputs)

    assertTrue(report.booksAnalyzed.isNotEmpty())
    assertTrue(report.workloadMetrics.totalEstimatedHours > 0f)
    assertTrue(report.workloadMetrics.dailyHoursRequired > 0f)
    assertNotNull(report.workloadMetrics.paceFeasibility)
    assertTrue(report.strategicAdvice.isNotEmpty())
    assertTrue(report.searchGroundingSources.isNotEmpty())
  }

  @Test
  fun `verify ai model config support for gemini and deepseek providers`() {
    val geminiConfig = com.example.model.AiModelConfig(
      provider = com.example.model.AiProvider.GEMINI,
      geminiModelName = "gemini-3.8-flash"
    )
    assertEquals("Gemini: gemini-3.8-flash", geminiConfig.activeDisplayName)

    val deepseekConfig = com.example.model.AiModelConfig(
      provider = com.example.model.AiProvider.DEEPSEEK,
      customModelName = "deepseek-v4-pro",
      customApiKey = "sk-test-token"
    )
    assertEquals("DeepSeek: deepseek-v4-pro", deepseekConfig.activeDisplayName)
  }

  @Test
  fun `verify multi-preset selection and unified multi-subject planning generation`() {
    val allPresets = ExamPresets.presets
    assertEquals(8, allPresets.size)

    // Select 4 core subjects: English 1 + CS 408 + Math 1 + Politics
    val selected = allPresets.filter {
      it.id in listOf("preset_english_one", "preset_cs_408", "preset_math_one", "preset_politics")
    }
    assertEquals(4, selected.size)

    val mergedInputs = ExamPresets.mergePresets(selected)
    assertTrue(mergedInputs.books.isNotEmpty())
    assertTrue(mergedInputs.problemSets.isNotEmpty())
    assertTrue(mergedInputs.dailyHoursAvailable >= 7.0f)
    assertTrue(mergedInputs.examTitle.contains("考研英一"))
    assertTrue(mergedInputs.examTitle.contains("408"))

    // Generate comprehensive rigorous plan
    val contract = AcademicPlanningEngine.generateRigorousPlan("Profile_A", mergedInputs)
    assertNotNull(contract)
    assertTrue(contract.dailySchedule.isNotEmpty())

    // Verify day 1 mission includes multi-subject tasks
    val day1 = contract.dailySchedule.first()
    assertTrue(day1.tasks.size >= 4)

    // Verify Stage-Based Monthly & Weekly Book Clearance Milestones
    assertTrue("Monthly phases must not be empty", contract.monthlyPhases.isNotEmpty())
    assertTrue("Weekly phases must not be empty", contract.weeklyPhases.isNotEmpty())

    val firstMonth = contract.monthlyPhases.first()
    assertEquals(1, firstMonth.phaseNumber)
    assertEquals("MONTH", firstMonth.phaseType)
    assertTrue(firstMonth.title.contains("第 1 个月"))
    assertTrue(firstMonth.coreObjective.isNotBlank())
    assertTrue("Monthly target books must not be empty", firstMonth.targetBooks.isNotEmpty())
    assertTrue("Monthly deliverables must not be empty", firstMonth.deliverables.isNotEmpty())

    val firstWeek = contract.weeklyPhases.first()
    assertEquals(1, firstWeek.phaseNumber)
    assertEquals("WEEK", firstWeek.phaseType)
    assertTrue(firstWeek.title.contains("第 1 周"))
    assertTrue("Weekly target books must not be empty", firstWeek.targetBooks.isNotEmpty())

    // Verify daily mission links to monthly/weekly phase goals
    assertTrue(day1.phaseLabel.isNotBlank())
    assertTrue(day1.phaseLabel.contains("第 1 月"))
    assertTrue(day1.phaseLabel.contains("第 1 周"))
    assertTrue(day1.phaseBookGoal.isNotBlank())
  }

  @Test
  fun `verify user specified textbooks and materials configured in presets`() {
    val csPreset = ExamPresets.presets.first { it.id == "preset_cs_408" }
    assertTrue(csPreset.rawInputs.books.any { it.contains("王道《数据结构复习指导》") })
    assertTrue(csPreset.rawInputs.books.any { it.contains("王道《计算机组成原理复习指导》") })
    assertTrue(csPreset.rawInputs.books.any { it.contains("王道《操作系统复习指导》") })
    assertTrue(csPreset.rawInputs.books.any { it.contains("王道《计算机网络复习指导》") })

    val mathPreset = ExamPresets.presets.first { it.id == "preset_math_one" }
    assertTrue(mathPreset.rawInputs.books.any { it.contains("武忠祥") })
    assertTrue(mathPreset.rawInputs.books.any { it.contains("概率论与数理统计基础讲义") })
    assertTrue(mathPreset.rawInputs.problemSets.any { it.contains("660题") })

    val politicsPreset = ExamPresets.presets.first { it.id == "preset_politics" }
    assertTrue(politicsPreset.rawInputs.problemSets.any { it.contains("1000题") })

    val modernChinese = ExamPresets.presets.first { it.id == "preset_modern_chinese" }
    assertTrue(modernChinese.rawInputs.books.any { it.contains("邢福义版《现代汉语》(第三版") })
    assertTrue(modernChinese.rawInputs.problemSets.any { it.contains("现代汉语考研与辅导习题集") })

    val ancientChinese = ExamPresets.presets.first { it.id == "preset_ancient_chinese" }
    assertTrue(ancientChinese.rawInputs.books.any { it.contains("郭锡良版《古代汉语》(上下两册)") })

    val intlChinese = ExamPresets.presets.first { it.id == "preset_international_chinese" }
    assertTrue(intlChinese.rawInputs.books.any { it.contains("程裕祯版《中国文化要略》(第五版)") })
    assertTrue(intlChinese.rawInputs.books.any { it.contains("刘珣《对外汉语教学引论》") })
    assertTrue(intlChinese.rawInputs.books.any { it.contains("孟昭毅版《外国文化史》(第二版)") })
    assertTrue(intlChinese.rawInputs.problemSets.any { it.contains("汉语国际教育冲关宝典") })

    val japanesePreset = ExamPresets.presets.first { it.id == "preset_japanese_203" }
    assertTrue(japanesePreset.rawInputs.books.any { it.contains("考研日语高分专项突破·阅读分册") })
  }

  @Test
  fun `verify A4MatrixExporter renders bitmap and exports pdf and image files`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preset = ExamPresets.presets[0]
    val contract = AcademicPlanningEngine.generateRigorousPlan("Profile_A", preset.rawInputs)

    // Test bitmap rendering
    val bitmap = A4MatrixExporter.renderMatrixBitmap(contract)
    assertNotNull(bitmap)
    assertEquals(1240, bitmap.width)
    assertTrue(bitmap.height >= 1754)

    // Test PNG image export
    val pngFile = A4MatrixExporter.exportToPng(context, contract)
    assertTrue(pngFile.exists())
    assertTrue(pngFile.length() > 0)
    assertTrue(pngFile.name.endsWith(".png"))

    // Test PDF export (handled natively on device; Robolectric JVM may lack native Skia PDF)
    try {
      val pdfFile = A4MatrixExporter.exportToPdf(context, contract)
      assertTrue(pdfFile.exists())
      assertTrue(pdfFile.length() > 0)
      assertTrue(pdfFile.name.endsWith(".pdf"))
    } catch (e: IllegalStateException) {
      // Expected in Robolectric JVM environment where native Android PDF serializer is mocked
      println("Robolectric PdfDocument notice: ${e.message}")
    }
  }
}

