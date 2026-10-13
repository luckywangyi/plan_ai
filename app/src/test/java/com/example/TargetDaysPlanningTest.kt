package com.example

import com.example.engine.AcademicPlanningEngine
import com.example.engine.GeminiStudyService
import com.example.model.DailyMission
import com.example.model.PlanOverview
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TargetDaysPlanningTest {

    @Test
    fun testPlanningEngineExact23Days() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 23)
        val targetDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val rawInputs = StudyRawInputs(
            examTitle = "23天冲刺计划",
            targetDate = targetDateStr,
            dailyHoursAvailable = 5.0f,
            books = listOf("数据结构"),
            problemSets = listOf("王道单科"),
            videoLectures = emptyList()
        )

        val contract = AcademicPlanningEngine.generateRigorousPlan("test_profile", rawInputs)

        assertEquals(23, contract.dailySchedule.size)
        assertEquals(1, contract.dailySchedule.first().day)
        assertEquals(23, contract.dailySchedule.last().day)
    }

    @Test
    fun testPlanningEngineSmallDaysAllowed() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val targetDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val rawInputs = StudyRawInputs(
            examTitle = "3天模考冲刺",
            targetDate = targetDateStr,
            dailyHoursAvailable = 6.0f,
            books = listOf("计算机网络"),
            problemSets = emptyList(),
            videoLectures = emptyList()
        )

        val contract = AcademicPlanningEngine.generateRigorousPlan("test_profile", rawInputs)

        assertEquals(3, contract.dailySchedule.size)
        assertEquals(1, contract.dailySchedule.first().day)
        assertEquals(3, contract.dailySchedule.last().day)
    }

    @Test
    fun testReconcileScheduleWhenAiTruncatesTo3DaysFor23DayPlan() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 23)
        val targetDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val rawInputs = StudyRawInputs(
            examTitle = "23天考研强化计划",
            targetDate = targetDateStr,
            dailyHoursAvailable = 6.0f,
            books = listOf("操作系统", "数据结构"),
            problemSets = listOf("408真题"),
            videoLectures = emptyList()
        )

        // Mock an AI contract that prematurely truncated to 3 days
        val truncatedAiContract = StudySyncContract(
            profileId = "profile_a",
            planOverview = PlanOverview(
                title = "AI生成的3天计划",
                targetDate = targetDateStr,
                totalEstimatedHours = 18,
                currentPaceStatus = "按期推进",
                completionRate = "0%"
            ),
            dashboardComparison = com.example.model.DashboardComparison(
                profileA_Progress = "0%",
                profileB_Progress = "0%",
                velocityDiff = "同步基线"
            ),
            milestones = emptyList(),
            dailySchedule = listOf(
                DailyMission(day = 1, date = "2026-09-15", coreFocus = "操作系统导论", allocatedMinutes = 360, phaseLabel = "阶段1", phaseBookGoal = "目标1", tasks = emptyList()),
                DailyMission(day = 2, date = "2026-09-16", coreFocus = "进程与线程", allocatedMinutes = 360, phaseLabel = "阶段1", phaseBookGoal = "目标2", tasks = emptyList()),
                DailyMission(day = 3, date = "2026-09-17", coreFocus = "内存管理", allocatedMinutes = 360, phaseLabel = "阶段1", phaseBookGoal = "目标3", tasks = emptyList())
            ),
            adaptiveAdjustments = com.example.model.AdaptiveAdjustments(
                feedbackSummary = "初始生成",
                paceAdjustment = "按期推进",
                upcomingChanges = emptyList()
            )
        )

        val reconciled = GeminiStudyService.reconcileScheduleToTargetDays(
            parsedContract = truncatedAiContract,
            profileId = "profile_a",
            rawInputs = rawInputs,
            targetDays = 23
        )

        // Verifications:
        // 1. Reconciled schedule must have all 23 days
        assertEquals(23, reconciled.dailySchedule.size)
        // 2. Day 1, 2, 3 should preserve the AI's custom focus
        assertEquals("操作系统导论", reconciled.dailySchedule[0].coreFocus)
        assertEquals("进程与线程", reconciled.dailySchedule[1].coreFocus)
        assertEquals("内存管理", reconciled.dailySchedule[2].coreFocus)
        // 3. Days 4 through 23 should be filled in by the academic syllabus engine
        assertEquals(4, reconciled.dailySchedule[3].day)
        assertEquals(23, reconciled.dailySchedule[22].day)
        assertTrue(reconciled.dailySchedule[3].tasks.isNotEmpty() || reconciled.dailySchedule[3].coreFocus.isNotBlank())
        // 4. Milestones should be properly populated
        assertTrue(reconciled.milestones.isNotEmpty())
    }
}
