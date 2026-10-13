package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.ContractTask
import com.example.model.DailyMission
import com.example.model.StudySyncContract
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A4 Matrix Exporter:
 * Generates vector-sharp PDF documents and complete, un-truncated high-resolution PNG images
 * for the StudySync Check-in & Habit Tracking Matrix.
 *
 * Core Guarantee:
 * All tasks and specific content to complete are displayed in full without truncation or ellipsis.
 */
object A4MatrixExporter {

    const val A4_WIDTH = 1240
    const val A4_HEIGHT = 1754

    data class TaskBadgeStyle(
        val label: String,
        val bgColor: Int,
        val textColor: Int
    )

    data class MeasuredTask(
        val task: ContractTask,
        val badgeStyle: TaskBadgeStyle,
        val fullContentText: String,
        val lines: List<String>,
        val itemHeight: Float
    )

    data class MeasuredDay(
        val mission: DailyMission,
        val tasks: List<MeasuredTask>,
        val rowHeight: Float
    )

    /**
     * Cleans material titles by removing redundant subject bracket tags (e.g. "[高等数学] 张宇30讲" -> "张宇30讲")
     */
    fun cleanMaterialTitle(material: String): String {
        val stripped = material.replace(Regex("^\\[.*?\\]\\s*"), "").trim()
        return if (stripped.isNotBlank()) stripped else material.trim()
    }

    /**
     * Cleans task scope by removing generic study-methodology and procedural filler
     * (e.g. "在严格限时闭卷环境下...", "专项演算: 攻坚", "含公式严格推导与步骤规范"),
     * strictly focusing on WHAT needs to be completed:
     * Chapters, sections, page numbers, problem numbers, and core topics.
     */
    fun cleanScopeForChecklist(rawScope: String): String {
        var s = rawScope.trim()

        // 1. Remove methodology prefixes
        s = s.replace(
            Regex("^(晨间核心突破|黄金数理攻坚|专业题型实战|专项演算|精炼内化|客观题专项排查|深度攻坚|基础扫盲|全科考后深度错题复盘归纳|考后深度错题复盘归纳)[:：]\\s*"),
            ""
        )
        s = s.replace(Regex("^攻坚\\s*"), "")
        s = s.replace(Regex("^(研读|精读)\\s+"), "")

        // 2. Remove procedural parentheticals and cognitive density tags
        s = s.replace(Regex("【.*?认知系数.*?】"), "")
        s = s.replace(Regex("\\(含.*?草稿推演.*?\\)"), "")
        s = s.replace(Regex("\\(含.*?时空复杂度.*?\\)"), "")
        s = s.replace(Regex("\\(含.*?步骤规范.*?\\)"), "")
        s = s.replace(Regex("\\(含.*?手写算法.*?\\)"), "")
        s = s.replace(Regex("并录入错题本.*"), "")

        // 3. Remove environmental / procedural instructions
        s = s.replace(Regex("严格实战考场限时环境下"), "")
        s = s.replace(Regex("在严格.*?环境下"), "")
        s = s.replace(Regex("严格限时演练"), "")
        s = s.replace(Regex("与各科最后知识盲区紧急修补"), "与错题总结")
        s = s.replace(Regex("逐题剖析错题成因与最终知识漏洞紧急修补"), "错题复盘与考点攻坚")
        s = s.replace(Regex("逐题剖析错题成因与各科最后知识盲区紧急修补"), "错题深度复盘与弱项攻坚")

        // 4. Clean up whitespace
        s = s.replace(Regex("\\s{2,}"), " ").trim()
        return if (s.isNotBlank()) s else rawScope.trim()
    }

    /**
     * Splits text into multiple lines that fit within maxWidth without truncation.
     */
    fun splitTextToFitWidth(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isEmpty()) return emptyList()
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        var curLine = StringBuilder()

        for (ch in text) {
            val candidate = curLine.toString() + ch
            if (paint.measureText(candidate) <= maxWidth) {
                curLine.append(ch)
            } else {
                if (curLine.isNotEmpty()) {
                    lines.add(curLine.toString())
                    curLine = StringBuilder()
                }
                curLine.append(ch)
            }
        }
        if (curLine.isNotEmpty()) {
            lines.add(curLine.toString())
        }
        return lines
    }

    /**
     * Maps taskType string to a colored badge style.
     */
    fun getBadgeStyle(taskType: String): TaskBadgeStyle {
        return when (taskType.lowercase()) {
            "reading" -> TaskBadgeStyle("精读", Color.parseColor("#DBEAFE"), Color.parseColor("#1D4ED8"))
            "practice" -> TaskBadgeStyle("刷题", Color.parseColor("#DCFCE7"), Color.parseColor("#15803D"))
            "lecture" -> TaskBadgeStyle("网课", Color.parseColor("#F3E8FF"), Color.parseColor("#7E22CE"))
            "review" -> TaskBadgeStyle("复习", Color.parseColor("#FEF3C7"), Color.parseColor("#B45309"))
            else -> TaskBadgeStyle("任务", Color.parseColor("#F1F5F9"), Color.parseColor("#475569"))
        }
    }

    /**
     * Measures all missions and their tasks to ensure 100% of text fits without truncation.
     */
    private fun measureDailySchedule(
        dailySchedule: List<DailyMission>,
        taskColUsableWidth: Float,
        textPaint: Paint
    ): List<MeasuredDay> {
        val cbSize = 13f
        val badgeWidth = 34f
        val prefixWidth = cbSize + 6f + badgeWidth + 6f
        val textUsableWidth = taskColUsableWidth - prefixWidth

        return dailySchedule.map { mission ->
            val measuredTasks = mission.tasks.map { task ->
                val badge = getBadgeStyle(task.taskType)
                val cleanMat = cleanMaterialTitle(task.material)
                val cleanScope = cleanScopeForChecklist(task.scope)
                val fullText = "《$cleanMat》$cleanScope (${task.estimatedMinutes}m)"

                val lines = splitTextToFitWidth(fullText, textUsableWidth, textPaint)
                val itemHeight = maxOf(17f, lines.size * 15f)

                MeasuredTask(
                    task = task,
                    badgeStyle = badge,
                    fullContentText = fullText,
                    lines = lines,
                    itemHeight = itemHeight
                )
            }

            val rowHeight = if (measuredTasks.isEmpty()) {
                42f
            } else {
                val totalTasksHeight = measuredTasks.sumOf { it.itemHeight.toDouble() }.toFloat()
                val gapsHeight = maxOf(0, measuredTasks.size - 1) * 6f
                maxOf(42f, 16f + totalTasksHeight + gapsHeight)
            }

            MeasuredDay(
                mission = mission,
                tasks = measuredTasks,
                rowHeight = rowHeight
            )
        }
    }

    /**
     * Calculates the total height needed to render the entire contract matrix with all tasks complete.
     */
    fun calculateTotalRequiredHeight(contract: StudySyncContract): Int {
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
        }
        val taskColWidth = 860f
        val taskColUsableWidth = taskColWidth - 24f

        val measured = measureDailySchedule(contract.dailySchedule, taskColUsableWidth, textPaint)
        val rowsTotalHeight = measured.sumOf { it.rowHeight.toDouble() }.toFloat()

        val topMargin = 48f
        val headerAreaHeight = 70f
        val dividerHeight = 12f
        val milestonesAreaHeight = 88f
        val tableHeaderHeight = 32f
        val footerAreaHeight = 80f
        val bottomMargin = 48f

        val computed = topMargin + headerAreaHeight + dividerHeight + milestonesAreaHeight +
                tableHeaderHeight + rowsTotalHeight + footerAreaHeight + bottomMargin

        return maxOf(A4_HEIGHT, computed.toInt())
    }

    /**
     * Draws the A4 Matrix to a Canvas with adaptive height so all tasks are completely shown.
     */
    fun drawA4MatrixToCanvas(
        canvas: Canvas,
        contract: StudySyncContract,
        canvasHeight: Float = A4_HEIGHT.toFloat()
    ) {
        canvas.drawColor(Color.WHITE)

        val bgPaint = Paint().apply { isAntiAlias = true }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val textPaint = Paint().apply { isAntiAlias = true }

        val startX = 50f
        val startY = 48f
        val contentWidth = 1140f
        val endX = startX + contentWidth

        // 1. Double Archival Outer Border
        strokePaint.color = Color.parseColor("#94A3B8")
        strokePaint.strokeWidth = 2f
        canvas.drawRect(startX - 10f, startY - 10f, endX + 10f, canvasHeight - 36f, strokePaint)

        strokePaint.color = Color.parseColor("#E2E8F0")
        strokePaint.strokeWidth = 1f
        canvas.drawRect(startX - 6f, startY - 6f, endX + 6f, canvasHeight - 40f, strokePaint)

        var curY = startY + 12f

        // 2. Header Banner
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 23f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("STUDYSYNC AI · 考研全周期备考习惯追踪打卡矩阵", startX, curY + 20f, textPaint)

        // Subtitle badge
        val profileLabel = if (contract.profileId.lowercase().contains("a")) "画像A (主备考人)" else "画像B (研友协作)"
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#475569")
        val subInfo = "报考科目: ${contract.planOverview.title}  |  总周期: ${contract.dailySchedule.size}天全科攻坚  |  分区: $profileLabel"
        canvas.drawText(subInfo, startX, curY + 44f, textPaint)

        // Right side metadata pill
        val metaBoxWidth = 260f
        val metaBoxHeight = 44f
        val metaBoxLeft = endX - metaBoxWidth
        val metaBoxTop = curY
        bgPaint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(metaBoxLeft, metaBoxTop, endX, metaBoxTop + metaBoxHeight), 6f, 6f, bgPaint)
        strokePaint.color = Color.parseColor("#0F172A")
        strokePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(RectF(metaBoxLeft, metaBoxTop, endX, metaBoxTop + metaBoxHeight), 6f, 6f, strokePaint)

        textPaint.textSize = 11.5f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("考前截止: ${contract.planOverview.targetDate}", metaBoxLeft + 12f, metaBoxTop + 18f, textPaint)
        textPaint.textSize = 10.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("总课时: ${contract.planOverview.totalEstimatedHours}h  完成率: ${contract.planOverview.completionRate}", metaBoxLeft + 12f, metaBoxTop + 36f, textPaint)

        curY += 58f

        // Thin divider
        strokePaint.color = Color.parseColor("#CBD5E1")
        strokePaint.strokeWidth = 1.5f
        canvas.drawLine(startX, curY, endX, curY, strokePaint)
        curY += 12f

        // 3. Milestones Section (4 cards in one row)
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#1E293B")
        canvas.drawText("阶段诊断与考核里程碑 (Milestones):", startX, curY + 12f, textPaint)
        curY += 20f

        val milestoneSpacing = 10f
        val milestoneWidth = (contentWidth - 3 * milestoneSpacing) / 4f
        val milestoneHeight = 54f

        val milestones = contract.milestones.take(4)
        milestones.forEachIndexed { index, m ->
            val mLeft = startX + index * (milestoneWidth + milestoneSpacing)
            val mRight = mLeft + milestoneWidth

            bgPaint.color = if (m.completed) Color.parseColor("#ECFDF5") else Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(mLeft, curY, mRight, curY + milestoneHeight), 4f, 4f, bgPaint)
            strokePaint.color = if (m.completed) Color.parseColor("#10B981") else Color.parseColor("#CBD5E1")
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(RectF(mLeft, curY, mRight, curY + milestoneHeight), 4f, 4f, strokePaint)

            val cbSize = 14f
            val cbLeft = mLeft + 8f
            val cbTop = curY + 8f
            strokePaint.color = if (m.completed) Color.parseColor("#059669") else Color.parseColor("#64748B")
            strokePaint.strokeWidth = 1.5f
            canvas.drawRect(cbLeft, cbTop, cbLeft + cbSize, cbTop + cbSize, strokePaint)
            if (m.completed) {
                bgPaint.color = Color.parseColor("#059669")
                canvas.drawRect(cbLeft + 2f, cbTop + 2f, cbLeft + cbSize - 2f, cbTop + cbSize - 2f, bgPaint)
            }

            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(m.stage, cbLeft + cbSize + 6f, curY + 20f, textPaint)

            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textPaint.color = Color.parseColor("#059669")
            canvas.drawText(m.targetDate, mLeft + 8f, curY + 35f, textPaint)

            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.parseColor("#475569")
            canvas.drawText(m.assessmentTopic, mLeft + 8f, curY + 48f, textPaint)
        }

        curY += milestoneHeight + 14f

        // 4. Daily Habit Matrix Table Setup
        // Column widths: [Day: 65, Date: 85, Daily Tasks: 860, Status: 130] = 1140
        val colWidths = floatArrayOf(65f, 85f, 860f, 130f)
        val colHeaders = arrayOf("天数", "日期", "当日备考攻坚任务清单 (具体完成内容及逐项打卡)", "打卡状态")
        val tableHeaderHeight = 32f

        val tableStartY = curY

        // Table Header
        bgPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(startX, curY, endX, curY + tableHeaderHeight, bgPaint)
        strokePaint.color = Color.parseColor("#64748B")
        strokePaint.strokeWidth = 1.5f
        canvas.drawRect(startX, curY, endX, curY + tableHeaderHeight, strokePaint)

        var curColX = startX
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0F172A")

        for (i in colWidths.indices) {
            val w = colWidths[i]
            val headerText = colHeaders[i]
            val textWidth = textPaint.measureText(headerText)
            val posX = if (i == 2) curColX + 12f else curColX + (w - textWidth) / 2f
            canvas.drawText(headerText, posX, curY + 21f, textPaint)

            if (i > 0) {
                strokePaint.color = Color.parseColor("#94A3B8")
                strokePaint.strokeWidth = 1f
                canvas.drawLine(curColX, curY, curColX, curY + tableHeaderHeight, strokePaint)
            }
            curColX += w
        }

        curY += tableHeaderHeight

        // Measure all days dynamically so nothing is ever clipped
        val taskColUsableWidth = colWidths[2] - 24f
        val taskTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
        }
        val measuredDays = measureDailySchedule(contract.dailySchedule, taskColUsableWidth, taskTextPaint)

        // Draw each Day row
        measuredDays.forEachIndexed { index, measuredDay ->
            val mission = measuredDay.mission
            val rowY = curY
            val rowH = measuredDay.rowHeight
            val isEven = index % 2 == 0

            // Row background
            bgPaint.color = if (isEven) Color.WHITE else Color.parseColor("#F8FAFC")
            canvas.drawRect(startX, rowY, endX, rowY + rowH, bgPaint)

            // Row bottom line
            strokePaint.color = Color.parseColor("#CBD5E1")
            strokePaint.strokeWidth = 0.8f
            canvas.drawLine(startX, rowY + rowH, endX, rowY + rowH, strokePaint)

            val tasks = mission.tasks
            val doneCount = tasks.count { it.completed }
            val totalCount = tasks.size
            val allDone = totalCount > 0 && doneCount == totalCount

            var colX = startX
            for (colIdx in colWidths.indices) {
                val colW = colWidths[colIdx]

                // Column vertical divider
                if (colIdx > 0) {
                    strokePaint.color = Color.parseColor("#E2E8F0")
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawLine(colX, rowY, colX, rowY + rowH, strokePaint)
                }

                when (colIdx) {
                    0 -> { // Day (e.g. D1)
                        textPaint.textSize = 11.5f
                        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        textPaint.color = Color.parseColor("#1E293B")
                        val dayStr = "D${mission.day}"
                        val tw = textPaint.measureText(dayStr)
                        canvas.drawText(dayStr, colX + (colW - tw) / 2f, rowY + (rowH / 2f) + 4.5f, textPaint)
                    }
                    1 -> { // Date (e.g. 09-15)
                        textPaint.textSize = 10f
                        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        textPaint.color = Color.parseColor("#475569")
                        val dateStr = if (mission.date.length >= 5) mission.date.takeLast(5) else mission.date
                        val tw = textPaint.measureText(dateStr)
                        canvas.drawText(dateStr, colX + (colW - tw) / 2f, rowY + (rowH / 2f) + 4f, textPaint)
                    }
                    2 -> { // All Tasks with Checkbox, Badge, What-to-complete Content & Time
                        if (measuredDay.tasks.isEmpty()) {
                            textPaint.textSize = 10f
                            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            textPaint.color = Color.parseColor("#64748B")
                            canvas.drawText(mission.coreFocus, colX + 12f, rowY + (rowH / 2f) + 4f, textPaint)
                        } else {
                            var taskY = rowY + 9f
                            val cbSize = 13f
                            val badgeW = 34f
                            val badgeH = 15f

                            measuredDay.tasks.forEach { mTask ->
                                val task = mTask.task
                                val boxLeft = colX + 12f
                                val boxTop = taskY + 1f

                                // Checkbox
                                if (task.completed) {
                                    bgPaint.color = Color.parseColor("#059669")
                                    canvas.drawRoundRect(RectF(boxLeft, boxTop, boxLeft + cbSize, boxTop + cbSize), 2f, 2f, bgPaint)
                                    strokePaint.color = Color.parseColor("#047857")
                                    strokePaint.strokeWidth = 1f
                                    canvas.drawRoundRect(RectF(boxLeft, boxTop, boxLeft + cbSize, boxTop + cbSize), 2f, 2f, strokePaint)

                                    strokePaint.color = Color.WHITE
                                    strokePaint.strokeWidth = 1.6f
                                    canvas.drawLine(boxLeft + 2.5f, boxTop + 6.5f, boxLeft + 5.5f, boxTop + 10f, strokePaint)
                                    canvas.drawLine(boxLeft + 5.5f, boxTop + 10f, boxLeft + 10.5f, boxTop + 3.5f, strokePaint)
                                } else {
                                    strokePaint.color = Color.parseColor("#64748B")
                                    strokePaint.strokeWidth = 1.2f
                                    canvas.drawRoundRect(RectF(boxLeft, boxTop, boxLeft + cbSize, boxTop + cbSize), 2f, 2f, strokePaint)
                                }

                                // Type Badge
                                val badgeLeft = boxLeft + cbSize + 6f
                                val badgeTop = taskY
                                bgPaint.color = mTask.badgeStyle.bgColor
                                canvas.drawRoundRect(RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH), 3f, 3f, bgPaint)

                                textPaint.textSize = 8.5f
                                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textPaint.color = mTask.badgeStyle.textColor
                                val btw = textPaint.measureText(mTask.badgeStyle.label)
                                canvas.drawText(mTask.badgeStyle.label, badgeLeft + (badgeW - btw) / 2f, badgeTop + 11f, textPaint)

                                // Task Content Lines
                                val textStartX = badgeLeft + badgeW + 6f
                                textPaint.textSize = 10f
                                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                                textPaint.color = if (task.completed) Color.parseColor("#059669") else Color.parseColor("#0F172A")

                                mTask.lines.forEachIndexed { lIdx, lineText ->
                                    val lineBaseline = taskY + 11.5f + (lIdx * 15f)
                                    canvas.drawText(lineText, textStartX, lineBaseline, textPaint)
                                    if (task.completed) {
                                        val lw = textPaint.measureText(lineText)
                                        strokePaint.color = Color.parseColor("#059669")
                                        strokePaint.strokeWidth = 1f
                                        canvas.drawLine(textStartX, lineBaseline - 3.5f, textStartX + lw, lineBaseline - 3.5f, strokePaint)
                                    }
                                }

                                taskY += mTask.itemHeight + 6f
                            }
                        }
                    }
                    3 -> { // Status & Check-off summary
                        if (allDone) {
                            val badgeW = 90f
                            val badgeH = 22f
                            val bx = colX + (colW - badgeW) / 2f
                            val by = rowY + (rowH - badgeH) / 2f
                            bgPaint.color = Color.parseColor("#DCFCE7")
                            canvas.drawRoundRect(RectF(bx, by, bx + badgeW, by + badgeH), 4f, 4f, bgPaint)
                            strokePaint.color = Color.parseColor("#16A34A")
                            strokePaint.strokeWidth = 1f
                            canvas.drawRoundRect(RectF(bx, by, bx + badgeW, by + badgeH), 4f, 4f, strokePaint)

                            textPaint.textSize = 10f
                            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textPaint.color = Color.parseColor("#15803D")
                            val tw = textPaint.measureText("全部打卡 ✓")
                            canvas.drawText("全部打卡 ✓", colX + (colW - tw) / 2f, by + 15f, textPaint)
                        } else if (doneCount > 0) {
                            textPaint.textSize = 10f
                            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                            textPaint.color = Color.parseColor("#2563EB")
                            val progStr = "$doneCount / $totalCount 完成"
                            val tw = textPaint.measureText(progStr)
                            canvas.drawText(progStr, colX + (colW - tw) / 2f, rowY + (rowH / 2f) + 4f, textPaint)
                        } else {
                            textPaint.textSize = 9.5f
                            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                            textPaint.color = Color.parseColor("#94A3B8")
                            val waitStr = "0 / $totalCount 待打卡"
                            val tw = textPaint.measureText(waitStr)
                            canvas.drawText(waitStr, colX + (colW - tw) / 2f, rowY + (rowH / 2f) + 4f, textPaint)
                        }
                    }
                }
                colX += colW
            }

            curY += rowH
        }

        // Table Outer Frame
        strokePaint.color = Color.parseColor("#475569")
        strokePaint.strokeWidth = 1.5f
        canvas.drawRect(startX, tableStartY, endX, curY, strokePaint)

        curY += 20f

        // 5. Signatures and Archival Footer
        val footerY = curY
        textPaint.textSize = 11.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#334155")

        canvas.drawText("考生手写核签: __________________________", startX + 10f, footerY + 16f, textPaint)
        canvas.drawText("导师 / 研友复核: __________________________", startX + 500f, footerY + 16f, textPaint)

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#64748B")
        val printDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
        val footerNotes = "“每日精进，功不唐捐。” · 导出时间: $printDate · StudySync AI 考研备考档案"
        canvas.drawText(footerNotes, startX + 10f, footerY + 40f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#94A3B8")
        val specText = "高清全量打卡表 (${A4_WIDTH} × ${canvasHeight.toInt()} px)"
        val stw = textPaint.measureText(specText)
        canvas.drawText(specText, endX - stw - 10f, footerY + 40f, textPaint)
    }

    /**
     * Splits measured days into pages greedily so that every day and every task row
     * fits completely within standard A4 height (1754 px) without any clipping or truncation.
     */
    fun paginateDaysForA4(
        contract: StudySyncContract,
        measuredDays: List<MeasuredDay>
    ): List<List<DailyMission>> {
        if (measuredDays.isEmpty()) return emptyList()

        val pages = mutableListOf<List<DailyMission>>()
        var currentPage = mutableListOf<DailyMission>()
        var currentDaysHeight = 0f

        for (measuredDay in measuredDays) {
            val isFirstPage = pages.isEmpty()
            val availableHeight = if (isFirstPage) {
                A4_HEIGHT - (TOP_MARGIN + HEADER_AREA_HEIGHT + DIVIDER_HEIGHT + MILESTONES_AREA_HEIGHT + TABLE_HEADER_HEIGHT + FOOTER_AREA_HEIGHT + BOTTOM_MARGIN)
            } else {
                A4_HEIGHT - (TOP_MARGIN + CONTINUATION_HEADER_HEIGHT + DIVIDER_HEIGHT + TABLE_HEADER_HEIGHT + FOOTER_AREA_HEIGHT + BOTTOM_MARGIN)
            }

            if (currentPage.isNotEmpty() && (currentDaysHeight + measuredDay.rowHeight > availableHeight)) {
                pages.add(currentPage.toList())
                currentPage = mutableListOf(measuredDay.mission)
                currentDaysHeight = measuredDay.rowHeight
            } else {
                currentPage.add(measuredDay.mission)
                currentDaysHeight += measuredDay.rowHeight
            }
        }

        if (currentPage.isNotEmpty()) {
            pages.add(currentPage.toList())
        }

        return pages
    }

    private const val TOP_MARGIN = 48f
    private const val HEADER_AREA_HEIGHT = 70f
    private const val CONTINUATION_HEADER_HEIGHT = 38f
    private const val DIVIDER_HEIGHT = 12f
    private const val MILESTONES_AREA_HEIGHT = 88f
    private const val TABLE_HEADER_HEIGHT = 32f
    private const val FOOTER_AREA_HEIGHT = 80f
    private const val BOTTOM_MARGIN = 48f

    /**
     * Generates high-resolution Bitmaps split into exact A4 pages (1240 × 1754 px).
     * Guarantees every day is intact and never sliced mid-row.
     */
    fun renderA4Pages(contract: StudySyncContract): List<Bitmap> {
        val taskColUsableWidth = 860f - 24f
        val taskTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
        }
        val measuredDays = measureDailySchedule(contract.dailySchedule, taskColUsableWidth, taskTextPaint)
        val pagesOfMissions = paginateDaysForA4(contract, measuredDays)

        if (pagesOfMissions.isEmpty()) {
            val singleBmp = Bitmap.createBitmap(A4_WIDTH, A4_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(singleBmp)
            drawA4MatrixToCanvas(canvas, contract, A4_HEIGHT.toFloat())
            return listOf(singleBmp)
        }

        val totalPages = pagesOfMissions.size
        return pagesOfMissions.mapIndexed { pageIdx, pageMissions ->
            val bmp = Bitmap.createBitmap(A4_WIDTH, A4_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val pageContract = contract.copy(dailySchedule = pageMissions)
            drawA4MatrixToCanvas(canvas, pageContract, A4_HEIGHT.toFloat())

            if (totalPages > 1) {
                val pText = "第 ${pageIdx + 1} / $totalPages 页"
                val pPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 11f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    color = Color.parseColor("#475569")
                }
                val ptw = pPaint.measureText(pText)
                canvas.drawText(pText, 1190f - ptw, 1754f - 52f, pPaint)
            }

            bmp
        }
    }

    /**
     * Generates a high-resolution Bitmap with adaptive canvas height so that
     * EVERY DAY and EVERY TASK is rendered completely without any truncation.
     */
    fun renderMatrixBitmap(contract: StudySyncContract): Bitmap {
        val pages = renderA4Pages(contract)
        if (pages.size == 1) {
            return pages.first()
        }
        // If multi-page, stitch vertically page-by-page so each is an exact A4 page with clean boundary
        val stitched = Bitmap.createBitmap(A4_WIDTH, A4_HEIGHT * pages.size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stitched)
        pages.forEachIndexed { index, pageBmp ->
            canvas.drawBitmap(pageBmp, 0f, (index * A4_HEIGHT).toFloat(), null)
        }
        return stitched
    }

    /**
     * Exports the matrix as a standard multi-page or single-page PDF document.
     * Uses greedy task-boundary respecting pagination to strictly eliminate any mid-row truncation.
     */
    fun exportToPdf(context: Context, contract: StudySyncContract): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "StudySync_A4_Matrix_${contract.profileId}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(exportDir, fileName)

        val document = PdfDocument()

        // Measure missions and partition into pages dynamically
        val taskColUsableWidth = 860f - 24f
        val taskTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
        }
        val measuredDays = measureDailySchedule(contract.dailySchedule, taskColUsableWidth, taskTextPaint)
        val pagesOfMissions = paginateDaysForA4(contract, measuredDays)

        if (pagesOfMissions.size <= 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            drawA4MatrixToCanvas(page.canvas, contract, A4_HEIGHT.toFloat())
            document.finishPage(page)
        } else {
            val totalPages = pagesOfMissions.size
            pagesOfMissions.forEachIndexed { pageIdx, chunkDays ->
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIdx + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Slice contract for this page
                val pageContract = contract.copy(dailySchedule = chunkDays)
                drawA4MatrixToCanvas(canvas, pageContract, A4_HEIGHT.toFloat())

                // Draw Page indicator in corner
                val pText = "第 ${pageIdx + 1} / $totalPages 页"
                val pPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 11f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    color = Color.parseColor("#475569")
                }
                val ptw = pPaint.measureText(pText)
                canvas.drawText(pText, 1190f - ptw, 1754f - 52f, pPaint)

                document.finishPage(page)
            }
        }

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Exports the matrix as a complete, un-truncated high-resolution PNG image file.
     */
    fun exportToPng(context: Context, contract: StudySyncContract): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "StudySync_A4_Matrix_${contract.profileId}_${System.currentTimeMillis()}.png"
        val imageFile = File(exportDir, fileName)

        val bitmap = renderMatrixBitmap(contract)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return imageFile
    }

    /**
     * Shares any file via Android system Intent Chooser.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "调起分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct system print using Android PrintManager.
     */
    fun printPdf(context: Context, pdfFile: File, jobName: String = "StudySync A4 打卡表") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "当前设备不支持系统打印服务", Toast.LENGTH_SHORT).show()
                return
            }

            printManager.print(
                jobName,
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder("StudySync_A4_Matrix.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        try {
                            FileInputStream(pdfFile).use { input ->
                                FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                },
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
        } catch (e: Exception) {
            Toast.makeText(context, "调起打印失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
