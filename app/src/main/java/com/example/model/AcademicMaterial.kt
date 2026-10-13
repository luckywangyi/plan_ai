package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents academic material parsed from PDF or text files,
 * extracting reading volumes and content density metrics to guide daily task generation.
 */
@JsonClass(generateAdapter = true)
data class ParsedAcademicMaterial(
    @Json(name = "id") val id: String,
    @Json(name = "fileName") val fileName: String,
    @Json(name = "sourceType") val sourceType: String, // "PDF" | "TEXT" | "OUTLINE"
    @Json(name = "subject") val subject: String = "408", // e.g. "408", "数学一", "英语一", "政治"
    @Json(name = "readingVolume") val readingVolume: MaterialReadingVolume,
    @Json(name = "contentDensity") val contentDensity: MaterialContentDensity,
    @Json(name = "aiSummary") val aiSummary: String = "",
    @Json(name = "suggestedDailyPace") val suggestedDailyPace: String = ""
)

@JsonClass(generateAdapter = true)
data class MaterialReadingVolume(
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "totalCharacters") val totalCharacters: Long = 0,
    @Json(name = "totalChapters") val totalChapters: Int = 1,
    @Json(name = "estimatedProblemsCount") val estimatedProblemsCount: Int = 0,
    @Json(name = "chapters") val chapters: List<MaterialChapterInfo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MaterialChapterInfo(
    @Json(name = "chapterIndex") val chapterIndex: Int,
    @Json(name = "title") val title: String,
    @Json(name = "startPage") val startPage: Int,
    @Json(name = "endPage") val endPage: Int,
    @Json(name = "pageCount") val pageCount: Int,
    @Json(name = "estimatedProblems") val estimatedProblems: Int = 0,
    @Json(name = "densityLevel") val densityLevel: String = "中密" // "高难" | "中密" | "基础"
)

@JsonClass(generateAdapter = true)
data class MaterialContentDensity(
    @Json(name = "formulaDensity") val formulaDensity: Float = 0f, // 0.0 - 1.0 (LaTeX/math symbols)
    @Json(name = "codeDensity") val codeDensity: Float = 0f, // 0.0 - 1.0 (Code syntax/algorithms)
    @Json(name = "conceptDensity") val conceptDensity: Float = 0f, // 0.0 - 1.0 (Definitions/theorems)
    @Json(name = "exerciseDensity") val exerciseDensity: Float = 0f, // 0.0 - 1.0 (Exercise/problem ratio)
    @Json(name = "cognitiveLoadIndex") val cognitiveLoadIndex: Float = 1.0f, // 0.8 - 2.5 scale
    @Json(name = "densityTier") val densityTier: String = "标准密度", // "极高密度·硬核演算" | "高密度·算法逻辑" | "中密度·概念理解" | "标准密度·常规识记"
    @Json(name = "recommendedMinutesPerPage") val recommendedMinutesPerPage: Float = 3.0f,
    @Json(name = "recommendedMinutesPerChapter") val recommendedMinutesPerChapter: Int = 90
)
