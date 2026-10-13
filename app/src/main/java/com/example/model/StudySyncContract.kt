package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Core Data & Interaction Contract specified by StudySync AI schema.
 */
@JsonClass(generateAdapter = true)
data class StudySyncContract(
    @Json(name = "profileId") val profileId: String, // "Profile_A" | "Profile_B"
    @Json(name = "planOverview") val planOverview: PlanOverview,
    @Json(name = "dashboardComparison") val dashboardComparison: DashboardComparison,
    @Json(name = "milestones") val milestones: List<MilestoneItem>,
    @Json(name = "dailySchedule") val dailySchedule: List<DailyMission>,
    @Json(name = "adaptiveAdjustments") val adaptiveAdjustments: AdaptiveAdjustments,
    @Json(name = "bookAnalysis") val bookAnalysis: BookAnalysisReport? = null,
    @Json(name = "monthlyPhases") val monthlyPhases: List<PhaseMilestone> = emptyList(),
    @Json(name = "weeklyPhases") val weeklyPhases: List<PhaseMilestone> = emptyList(),
    @Json(name = "parsedMaterials") val parsedMaterials: List<ParsedAcademicMaterial> = emptyList(),
    @Json(name = "bookProgressProfiles") val bookProgressProfiles: List<BookProgressProfile> = emptyList(),
    @Json(name = "archivedCompletedBooks") val archivedCompletedBooks: List<String> = emptyList(),
    @Json(name = "stageTransitionNotes") val stageTransitionNotes: String = ""
)

@JsonClass(generateAdapter = true)
data class BookProgressProfile(
    @Json(name = "bookTitle") val bookTitle: String,
    @Json(name = "completedPercent") val completedPercent: Int = 0, // 0-100%
    @Json(name = "startChapter") val startChapter: Int = 1, // Start from chapter N (skips earlier chapters)
    @Json(name = "targetFinishDays") val targetFinishDays: Int? = null, // e.g. 15 means finish within 15 days
    @Json(name = "isCompleted") val isCompleted: Boolean = false, // Conquered / removed from future planning
    @Json(name = "stageLabel") val stageLabel: String = "全期推进", // e.g. "前15天突击结项" | "后15天进阶" | "全期推进"
    @Json(name = "naturalLanguageNote") val naturalLanguageNote: String = ""
)

@JsonClass(generateAdapter = true)
data class PhaseMilestone(
    @Json(name = "phaseId") val phaseId: String,
    @Json(name = "phaseType") val phaseType: String, // "MONTH" | "WEEK"
    @Json(name = "phaseNumber") val phaseNumber: Int,
    @Json(name = "title") val title: String, // e.g. "第 1 个月 · 基础筑基期"
    @Json(name = "timeRange") val timeRange: String, // e.g. "第 1-30 天"
    @Json(name = "targetBooks") val targetBooks: List<PhaseBookRequirement> = emptyList(),
    @Json(name = "coreObjective") val coreObjective: String = "",
    @Json(name = "deliverables") val deliverables: List<String> = emptyList(),
    @Json(name = "isCompleted") val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PhaseBookRequirement(
    @Json(name = "subject") val subject: String = "", // e.g. "408", "数学一"
    @Json(name = "bookTitle") val bookTitle: String,
    @Json(name = "targetScope") val targetScope: String, // e.g. "完成第 1-4 章精读"
    @Json(name = "associatedProblems") val associatedProblems: String = "", // e.g. "完成配套选择题200道"
    @Json(name = "status") val status: String = "进行中" // "未开始" | "进行中" | "已结项"
)

@JsonClass(generateAdapter = true)
data class PlanOverview(
    @Json(name = "title") val title: String,
    @Json(name = "targetDate") val targetDate: String, // YYYY-MM-DD
    @Json(name = "totalEstimatedHours") val totalEstimatedHours: Int,
    @Json(name = "currentPaceStatus") val currentPaceStatus: String, // "On Track" | "Lagging" | "Ahead"
    @Json(name = "completionRate") val completionRate: String // e.g. "42%"
)

@JsonClass(generateAdapter = true)
data class DashboardComparison(
    @Json(name = "profileA_Progress") val profileA_Progress: String,
    @Json(name = "profileB_Progress") val profileB_Progress: String,
    @Json(name = "velocityDiff") val velocityDiff: String
)

@JsonClass(generateAdapter = true)
data class MilestoneItem(
    @Json(name = "stage") val stage: String, // e.g. "25% Foundation Checkpoint"
    @Json(name = "targetDate") val targetDate: String,
    @Json(name = "assessmentTopic") val assessmentTopic: String,
    @Json(name = "completed") val completed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DailyMission(
    @Json(name = "day") val day: Int,
    @Json(name = "date") val date: String,
    @Json(name = "coreFocus") val coreFocus: String,
    @Json(name = "allocatedMinutes") val allocatedMinutes: Int,
    @Json(name = "tasks") val tasks: List<ContractTask>,
    @Json(name = "phaseLabel") val phaseLabel: String = "", // e.g. "第 1 月 · 第 1 周"
    @Json(name = "phaseBookGoal") val phaseBookGoal: String = "" // e.g. "本期结项: 《数据结构》1-3章 &《高数》导数"
)

@JsonClass(generateAdapter = true)
data class ContractTask(
    @Json(name = "id") val id: String = "",
    @Json(name = "material") val material: String,
    @Json(name = "scope") val scope: String,
    @Json(name = "taskType") val taskType: String, // "reading" | "practice" | "lecture" | "review"
    @Json(name = "estimatedMinutes") val estimatedMinutes: Int,
    @Json(name = "completed") val completed: Boolean = false,
    @Json(name = "actualMinutes") val actualMinutes: Int = 0,
    @Json(name = "errorRatePercent") val errorRatePercent: Int = 0,
    @Json(name = "difficultyFeedback") val difficultyFeedback: String = "Optimal" // "Easy" | "Optimal" | "Challenging" | "Overwhelming"
)

@JsonClass(generateAdapter = true)
data class AdaptiveAdjustments(
    @Json(name = "feedbackSummary") val feedbackSummary: String,
    @Json(name = "paceAdjustment") val paceAdjustment: String,
    @Json(name = "upcomingChanges") val upcomingChanges: List<String>
)

@JsonClass(generateAdapter = true)
data class MaterialCardItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String,
    @Json(name = "type") val type: String = "BOOK", // "BOOK" | "EXERCISE" | "LECTURE"
    @Json(name = "status") val status: String = "未开始", // "未开始" | "一轮精读" | "刷题强化" | "冲刺查漏" | "已结项"
    @Json(name = "isIncluded") val isIncluded: Boolean = true,
    @Json(name = "note") val note: String = "",
    @Json(name = "groundTruthCatalog") val groundTruthCatalog: String = "" // 真实章节目录/题量基准 Ground Truth
)

@JsonClass(generateAdapter = true)
data class StudyRawInputs(
    @Json(name = "examTitle") val examTitle: String,
    @Json(name = "targetDate") val targetDate: String,
    @Json(name = "dailyHoursAvailable") val dailyHoursAvailable: Float = 4.0f,
    @Json(name = "books") val books: List<String> = emptyList(),
    @Json(name = "problemSets") val problemSets: List<String> = emptyList(),
    @Json(name = "videoLectures") val videoLectures: List<String> = emptyList(),
    @Json(name = "customNotes") val customNotes: String = "",
    @Json(name = "parsedMaterials") val parsedMaterials: List<ParsedAcademicMaterial> = emptyList(),
    @Json(name = "bookProgressProfiles") val bookProgressProfiles: List<BookProgressProfile> = emptyList(),
    @Json(name = "archivedCompletedBooks") val archivedCompletedBooks: List<String> = emptyList(),
    @Json(name = "isPhaseAdditionMode") val isPhaseAdditionMode: Boolean = false, // If true, incremental additions without resetting existing progress
    @Json(name = "materialCards") val materialCards: List<MaterialCardItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BookAnalysisReport(
    @Json(name = "booksAnalyzed") val booksAnalyzed: List<AnalyzedBookItem> = emptyList(),
    @Json(name = "workloadMetrics") val workloadMetrics: WorkloadMetrics = WorkloadMetrics(),
    @Json(name = "strategicAdvice") val strategicAdvice: List<StrategicAdviceItem> = emptyList(),
    @Json(name = "searchGroundingSources") val searchGroundingSources: List<GroundingSourceItem> = emptyList(),
    @Json(name = "feasibilityAssessment") val feasibilityAssessment: String = ""
)

@JsonClass(generateAdapter = true)
data class AnalyzedBookItem(
    @Json(name = "title") val title: String,
    @Json(name = "bookType") val bookType: String = "核心教材", // "理论教材" | "真题精解" | "习题集" | "高频词汇/背诵" | "名师讲义"
    @Json(name = "estimatedTotalChapters") val estimatedTotalChapters: Int = 10,
    @Json(name = "estimatedQuestionCount") val estimatedQuestionCount: Int = 0,
    @Json(name = "difficultyLevel") val difficultyLevel: String = "高难重点", // "入门筑基" | "核心重点" | "高难冲刺"
    @Json(name = "recommendedReadingMinutesPerChapter") val recommendedReadingMinutesPerChapter: Int = 45,
    @Json(name = "recommendedMinutesPerProblem") val recommendedMinutesPerProblem: Float = 4.0f,
    @Json(name = "keyExamWeight") val keyExamWeight: String = "分值权重约 25-35%",
    @Json(name = "studyTip") val studyTip: String = "",
    @Json(name = "completedPercent") val completedPercent: Int = 0,
    @Json(name = "startChapter") val startChapter: Int = 1,
    @Json(name = "targetFinishDays") val targetFinishDays: Int? = null,
    @Json(name = "isCompleted") val isCompleted: Boolean = false,
    @Json(name = "stageLabel") val stageLabel: String = "全期推进"
)

@JsonClass(generateAdapter = true)
data class WorkloadMetrics(
    @Json(name = "totalReadingHours") val totalReadingHours: Float = 0f,
    @Json(name = "totalPracticeHours") val totalPracticeHours: Float = 0f,
    @Json(name = "totalReviewHours") val totalReviewHours: Float = 0f,
    @Json(name = "totalEstimatedHours") val totalEstimatedHours: Float = 0f,
    @Json(name = "dailyHoursRequired") val dailyHoursRequired: Float = 0f,
    @Json(name = "dailyHoursAvailable") val dailyHoursAvailable: Float = 0f,
    @Json(name = "paceFeasibility") val paceFeasibility: String = "负荷适中", // "负荷适中" | "节奏紧凑" | "存在赤字需加时"
    @Json(name = "paceDeficitOrSurplusHours") val paceDeficitOrSurplusHours: Float = 0f,
    @Json(name = "recommendedDailyRatio") val recommendedDailyRatio: String = "30% 理论研读 : 50% 刷题内化 : 20% 艾宾浩斯复盘"
)

@JsonClass(generateAdapter = true)
data class StrategicAdviceItem(
    @Json(name = "category") val category: String, // "刷题战术" | "遗忘防范" | "精力管理" | "考纲动向"
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "priority") val priority: String = "HIGH" // "HIGH" | "MEDIUM" | "INFO"
)

@JsonClass(generateAdapter = true)
data class GroundingSourceItem(
    @Json(name = "title") val title: String,
    @Json(name = "url") val url: String = "",
    @Json(name = "snippet") val snippet: String = ""
)

