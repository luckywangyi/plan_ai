package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.example.data.StudySyncJsonParser
import androidx.compose.material.icons.filled.ContentCopy
import com.example.engine.AcademicBookCatalog
import com.example.model.AiModelConfig
import com.example.model.ExamPreset
import com.example.model.ExamPresets
import com.example.model.MaterialCardItem
import com.example.model.StudyRawInputs
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicPurple
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngestPlanDialog(
    activeProfileId: String,
    initialRawInputs: StudyRawInputs?,
    aiConfig: AiModelConfig,
    onOpenAiConfig: () -> Unit,
    onDismiss: () -> Unit,
    onSaveDraft: ((StudyRawInputs) -> Unit)? = null,
    onResetToPresets: (() -> Unit)? = null,
    onIngestPlan: (StudyRawInputs) -> Unit,
    onAnalyzeBooks: ((StudyRawInputs) -> Unit)? = null
) {
    // Helper to calculate target remaining days from a target date string
    fun daysBetweenTodayAnd(dateStr: String): Int? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val target = sdf.parse(dateStr) ?: return null
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val targetCal = Calendar.getInstance().apply {
                time = target
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = targetCal.timeInMillis - todayCal.timeInMillis
            val days = Math.round(diff.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()
            maxOf(1, days)
        } catch (e: Exception) {
            null
        }
    }

    fun calculateTargetDate(days: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    // Initialize Material Cards from initialRawInputs
    val materialCards = remember {
        mutableStateListOf<MaterialCardItem>().apply {
            if (initialRawInputs != null && initialRawInputs.materialCards.isNotEmpty()) {
                addAll(initialRawInputs.materialCards)
            } else if (initialRawInputs != null) {
                // Migrate books, problemSets, videoLectures
                var counter = 1
                initialRawInputs.books.forEach { b ->
                    val isArchived = initialRawInputs.archivedCompletedBooks.any { b.contains(it) }
                    val status = if (isArchived) "已结项" else "一轮精读"
                    add(MaterialCardItem(id = "card_${counter++}", title = b, type = "BOOK", status = status, isIncluded = true))
                }
                initialRawInputs.problemSets.forEach { p ->
                    add(MaterialCardItem(id = "card_${counter++}", title = p, type = "EXERCISE", status = "刷题强化", isIncluded = true))
                }
                initialRawInputs.videoLectures.forEach { v ->
                    add(MaterialCardItem(id = "card_${counter++}", title = v, type = "LECTURE", status = "一轮精读", isIncluded = true))
                }
            }
        }
    }

    // If still empty (first time opening), initialize with 4 core subjects
    LaunchedEffect(Unit) {
        if (materialCards.isEmpty()) {
            val defaultPresetIds = listOf("preset_english_one", "preset_cs_408", "preset_math_one", "preset_politics")
            val presets = ExamPresets.presets.filter { it.id in defaultPresetIds }
            val merged = ExamPresets.mergePresets(presets)
            var counter = 1
            merged.books.forEach { b ->
                materialCards.add(MaterialCardItem(id = "card_${counter++}", title = b, type = "BOOK", status = "一轮精读", isIncluded = true))
            }
            merged.problemSets.forEach { p ->
                materialCards.add(MaterialCardItem(id = "card_${counter++}", title = p, type = "EXERCISE", status = "刷题强化", isIncluded = true))
            }
            merged.videoLectures.forEach { v ->
                materialCards.add(MaterialCardItem(id = "card_${counter++}", title = v, type = "LECTURE", status = "一轮精读", isIncluded = true))
            }
        }
    }

    // Input States
    var examTitle by remember {
        mutableStateOf(initialRawInputs?.examTitle?.ifBlank { "2026 全国硕士研究生统考备战计划" } ?: "2026 全国硕士研究生统考备战计划")
    }
    var targetDays by remember {
        val days = initialRawInputs?.let { daysBetweenTodayAnd(it.targetDate) } ?: 60
        mutableIntStateOf(days.coerceIn(1, 1000))
    }
    var dailyHoursAvailable by remember {
        mutableFloatStateOf(initialRawInputs?.dailyHoursAvailable ?: 7.5f)
    }
    var customNotes by remember {
        mutableStateOf(initialRawInputs?.customNotes ?: "注重真题精剖与错题复现，高难度科目分阶段突破，严格遵循艾宾浩斯复盘周期。")
    }
    var isPhaseAdditionMode by remember {
        mutableStateOf(initialRawInputs?.isPhaseAdditionMode ?: false)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var materialFilterType by remember { mutableStateOf("ALL") } // "ALL", "BOOK", "EXERCISE", "LECTURE", "INCLUDED"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<MaterialCardItem?>(null) }
    var draftSavedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(draftSavedNotice) {
        if (draftSavedNotice) {
            delay(2500)
            draftSavedNotice = false
        }
    }

    // Build raw inputs object for saving or plan generation
    fun buildCurrentInputs(): StudyRawInputs {
        val includedCards = materialCards.filter { it.isIncluded }
        val booksList = mutableListOf<String>()
        val problemsList = mutableListOf<String>()
        val lecturesList = mutableListOf<String>()
        val archivedList = mutableListOf<String>()

        includedCards.forEach { card ->
            val noteTag = if (card.note.isNotBlank()) " ${card.note}" else ""
            val statusTag = if (card.status != "未开始") " (${card.status}$noteTag)" else if (noteTag.isNotBlank()) " ($noteTag)" else ""
            val formattedTitle = "${card.title}$statusTag"

            when (card.type) {
                "BOOK" -> booksList.add(formattedTitle)
                "EXERCISE" -> problemsList.add(formattedTitle)
                "LECTURE" -> lecturesList.add(formattedTitle)
            }
            if (card.status == "已结项") {
                archivedList.add(card.title)
            }
        }

        return StudyRawInputs(
            examTitle = examTitle.ifBlank { "备战计划" },
            targetDate = calculateTargetDate(targetDays),
            dailyHoursAvailable = dailyHoursAvailable,
            books = booksList,
            problemSets = problemsList,
            videoLectures = lecturesList,
            customNotes = customNotes,
            materialCards = materialCards.toList(),
            archivedCompletedBooks = archivedList.distinct(),
            isPhaseAdditionMode = isPhaseAdditionMode
        )
    }

    fun saveDraft() {
        val inputs = buildCurrentInputs()
        onSaveDraft?.invoke(inputs)
        draftSavedNotice = true
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val copyCustomPrompt: () -> Unit = {
        saveDraft()
        val inputs = buildCurrentInputs()
        val count = materialCards.count { it.isIncluded }
        val prompt = StudySyncJsonParser.buildCustomizedAiPrompt(inputs, targetProfileId = activeProfileId)
        clipboardManager.setText(AnnotatedString(prompt))
        Toast.makeText(
            context,
            "✓ 已基于当前已勾选的 ${count} 项资料与标注生成外部 AI 提示词！直接发给 DeepSeek/ChatGPT 即可",
            Toast.LENGTH_LONG
        ).show()
    }

    Dialog(
        onDismissRequest = {
            saveDraft()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Sticky Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "备战资料库与计划配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeProfileId == "Profile_A") AcademicBlue.copy(alpha = 0.12f) else AcademicPurple.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (activeProfileId == "Profile_A") "本人画像 A" else "学友画像 B",
                                    color = if (activeProfileId == "Profile_A") AcademicBlue else AcademicPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { saveDraft() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("button_save_changes")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (draftSavedNotice) "✓ 已保存" else "保存修改",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    saveDraft()
                                    onDismiss()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Tab Navigation Row
                val includedCount = materialCards.count { it.isIncluded }
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = AcademicBlue,
                    edgePadding = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "📚 备考资料库 (${includedCount}/${materialCards.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "⏱️ 周期与时间 (${targetDays}天 · ${dailyHoursAvailable}h)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "🧠 战略与 AI 指令",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                // 3. Tab Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        0 -> MaterialCardsTab(
                            cards = materialCards,
                            filterType = materialFilterType,
                            onFilterChange = { materialFilterType = it },
                            onToggleInclude = { cardId ->
                                val index = materialCards.indexOfFirst { it.id == cardId }
                                if (index != -1) {
                                    val old = materialCards[index]
                                    materialCards[index] = old.copy(isIncluded = !old.isIncluded)
                                }
                            },
                            onSelectAll = { includeAll ->
                                val updated = materialCards.map { it.copy(isIncluded = includeAll) }
                                materialCards.clear()
                                materialCards.addAll(updated)
                            },
                            onChangeStatus = { cardId, newStatus ->
                                val index = materialCards.indexOfFirst { it.id == cardId }
                                if (index != -1) {
                                    val old = materialCards[index]
                                    materialCards[index] = old.copy(status = newStatus)
                                }
                            },
                            onChangeNote = { cardId, newNote ->
                                val index = materialCards.indexOfFirst { it.id == cardId }
                                if (index != -1) {
                                    val old = materialCards[index]
                                    materialCards[index] = old.copy(note = newNote)
                                }
                            },
                            onDeleteCard = { cardId ->
                                materialCards.removeAll { it.id == cardId }
                            },
                            onEditCard = { card ->
                                editingCard = card
                            },
                            onAddNewCard = { showAddDialog = true }
                        )

                        1 -> ScheduleParametersTab(
                            examTitle = examTitle,
                            onExamTitleChange = { examTitle = it },
                            targetDays = targetDays,
                            onTargetDaysChange = { targetDays = it },
                            dailyHoursAvailable = dailyHoursAvailable,
                            onDailyHoursChange = { dailyHoursAvailable = it },
                            isPhaseAdditionMode = isPhaseAdditionMode,
                            onTogglePhaseMode = { isPhaseAdditionMode = it },
                            targetDate = calculateTargetDate(targetDays)
                        )

                        2 -> StrategyAiTab(
                            customNotes = customNotes,
                            onCustomNotesChange = { customNotes = it },
                            aiConfig = aiConfig,
                            onOpenAiConfig = onOpenAiConfig,
                            onCopyCustomPrompt = copyCustomPrompt
                        )
                    }
                }

                // 4. Sticky Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "已选 ${includedCount} 项资料 · 周期 ${targetDays} 天 (${dailyHoursAvailable}h/天)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AnimatedVisibility(visible = draftSavedNotice) {
                                Text(
                                    text = "✓ 已存入草稿",
                                    fontSize = 11.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { saveDraft() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("button_bottom_save")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("保存", fontSize = 11.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = copyCustomPrompt,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("button_copy_prompt_ingest")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = AcademicPurple)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("复制Prompt", fontSize = 11.sp, maxLines = 1, color = AcademicPurple)
                            }

                            if (onAnalyzeBooks != null) {
                                OutlinedButton(
                                    onClick = {
                                        saveDraft()
                                        onAnalyzeBooks(buildCurrentInputs())
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .testTag("button_analyze_books")
                                ) {
                                    Text("测算负荷", fontSize = 11.sp, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = {
                                    saveDraft()
                                    onIngestPlan(buildCurrentInputs())
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(if (onAnalyzeBooks != null) 1.6f else 2.2f)
                                    .testTag("button_generate_plan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "启动 AI 规划",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Material Dialog
    if (showAddDialog) {
        AddMaterialCardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, status, note, groundTruthCatalog ->
                val newCard = MaterialCardItem(
                    id = "card_${UUID.randomUUID().toString().take(8)}",
                    title = title,
                    type = type,
                    status = status,
                    isIncluded = true,
                    note = note,
                    groundTruthCatalog = groundTruthCatalog
                )
                materialCards.add(0, newCard)
                showAddDialog = false
                saveDraft()
            }
        )
    }

    // Edit Material Dialog
    editingCard?.let { card ->
        EditMaterialCardDialog(
            card = card,
            onDismiss = { editingCard = null },
            onConfirm = { updatedCard ->
                val index = materialCards.indexOfFirst { it.id == updatedCard.id }
                if (index != -1) {
                    materialCards[index] = updatedCard
                }
                editingCard = null
                saveDraft()
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// Tab 1: Material Cards View with Filters & Inclusion Toggles
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialCardsTab(
    cards: List<MaterialCardItem>,
    filterType: String,
    onFilterChange: (String) -> Unit,
    onToggleInclude: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onChangeStatus: (String, String) -> Unit,
    onChangeNote: (String, String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onEditCard: (MaterialCardItem) -> Unit,
    onAddNewCard: () -> Unit
) {
    val booksCount = cards.count { it.type == "BOOK" }
    val exerciseCount = cards.count { it.type == "EXERCISE" }
    val lectureCount = cards.count { it.type == "LECTURE" }
    val includedCount = cards.count { it.isIncluded }

    val filteredCards = when (filterType) {
        "BOOK" -> cards.filter { it.type == "BOOK" }
        "EXERCISE" -> cards.filter { it.type == "EXERCISE" }
        "LECTURE" -> cards.filter { it.type == "LECTURE" }
        "INCLUDED" -> cards.filter { it.isIncluded }
        else -> cards
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls Row: Filters + Add Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { onFilterChange("ALL") },
                    label = { Text("全部 (${cards.size})", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = filterType == "BOOK",
                    onClick = { onFilterChange("BOOK") },
                    label = { Text("📖 教材 ($booksCount)", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = filterType == "EXERCISE",
                    onClick = { onFilterChange("EXERCISE") },
                    label = { Text("📝 习题 ($exerciseCount)", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = filterType == "LECTURE",
                    onClick = { onFilterChange("LECTURE") },
                    label = { Text("🎬 网课 ($lectureCount)", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = filterType == "INCLUDED",
                    onClick = { onFilterChange("INCLUDED") },
                    label = { Text("✓ 已纳入 ($includedCount)", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = onAddNewCard,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue.copy(alpha = 0.15f)),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("button_add_material_card")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AcademicBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("添加资料", color = AcademicBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Summary bar with Quick Select All / Unselect All
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "当前纳入: $includedCount 项资料将由 AI 科学编排到每日攻坚日程中",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    Text(
                        text = "全选",
                        fontSize = 11.sp,
                        color = AcademicBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onSelectAll(true) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "清空",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectAll(false) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Cards List
        if (filteredCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无匹配的资料卡片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onAddNewCard) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("点击添加教材、习题集或网课", fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCards, key = { it.id }) { card ->
                    MaterialCardRow(
                        card = card,
                        onToggleInclude = { onToggleInclude(card.id) },
                        onChangeStatus = { newStatus -> onChangeStatus(card.id, newStatus) },
                        onChangeNote = { newNote -> onChangeNote(card.id, newNote) },
                        onEdit = { onEditCard(card) },
                        onDelete = { onDeleteCard(card.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Single Material Card Row
// -----------------------------------------------------------------------------------------
@Composable
private fun MaterialCardRow(
    card: MaterialCardItem,
    onToggleInclude: () -> Unit,
    onChangeStatus: (String) -> Unit,
    onChangeNote: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showNotePrompt by remember { mutableStateOf(false) }

    val statusOptions = listOf("未开始", "一轮精读", "刷题强化", "冲刺查漏", "已结项")

    val statusColor = when (card.status) {
        "一轮精读" -> AcademicBlue
        "刷题强化" -> AcademicPurple
        "冲刺查漏" -> Color(0xFFEA580C)
        "已结项" -> Color(0xFF16A34A)
        else -> Color(0xFF757575)
    }

    val typeLabel = when (card.type) {
        "BOOK" -> "📖 教材"
        "EXERCISE" -> "📝 习题"
        "LECTURE" -> "🎬 网课"
        else -> "📚 资料"
    }

    val typeBg = when (card.type) {
        "BOOK" -> AcademicBlue.copy(alpha = 0.10f)
        "EXERCISE" -> AcademicPurple.copy(alpha = 0.10f)
        else -> Color(0xFF0D9488).copy(alpha = 0.10f)
    }
    val typeColor = when (card.type) {
        "BOOK" -> AcademicBlue
        "EXERCISE" -> AcademicPurple
        else -> Color(0xFF0D9488)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isIncluded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            width = if (card.isIncluded) 1.5.dp else 1.dp,
            color = if (card.isIncluded) AcademicBlue.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_material_${card.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top Row: Checkbox + Type Tag + Delete / Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onToggleInclude() }
                        .padding(end = 6.dp)
                ) {
                    Checkbox(
                        checked = card.isIncluded,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = AcademicBlue),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (card.isIncluded) "纳入当前计划" else "未纳入 (仅存库)",
                        fontSize = 12.sp,
                        fontWeight = if (card.isIncluded) FontWeight.Bold else FontWeight.Normal,
                        color = if (card.isIncluded) AcademicBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Middle: Material Title
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (card.isIncluded) FontWeight.SemiBold else FontWeight.Normal,
                color = if (card.isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Line: Status dropdown pill + Note pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Dropdown Pill
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { showStatusMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "状态: ${card.status} ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        statusOptions.forEach { st ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = st,
                                        fontWeight = if (card.status == st) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    onChangeStatus(st)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Progress note tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showNotePrompt = true }
                ) {
                    Text(
                        text = if (card.note.isNotBlank()) "进度: ${card.note} ✎" else "+ 进度备注",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (card.groundTruthCatalog.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AcademicGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AcademicGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onEdit() }
                    ) {
                        Text(
                            text = "✓ 已锁事实基准",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcademicGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }

    // Quick Note Edit Dialog
    if (showNotePrompt) {
        var tempNote by remember { mutableStateOf(card.note) }
        AlertDialog(
            onDismissRequest = { showNotePrompt = false },
            title = { Text("设置进度/章节备注", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("例如：'从第3章开始'、'20天内刷完'、'重点攻坚'", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempNote,
                        onValueChange = { tempNote = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onChangeNote(tempNote.trim())
                    showNotePrompt = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotePrompt = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// Tab 2: Target Days & Daily Hours Parameters
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleParametersTab(
    examTitle: String,
    onExamTitleChange: (String) -> Unit,
    targetDays: Int,
    onTargetDaysChange: (Int) -> Unit,
    dailyHoursAvailable: Float,
    onDailyHoursChange: (Float) -> Unit,
    isPhaseAdditionMode: Boolean,
    onTogglePhaseMode: (Boolean) -> Unit,
    targetDate: String
) {
    val presetDays = listOf(7, 14, 21, 30, 60, 90, 180)
    var daysInputText by remember(targetDays) { mutableStateOf(targetDays.toString()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Exam Title
        Text(
            text = "备战目标与计划标题",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = examTitle,
            onValueChange = onExamTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_exam_title"),
            placeholder = { Text("例如：2026 计算机408考研总攻坚计划") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Target Days
        Text(
            text = "备战总周期与倒计时 (天数)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            presetDays.forEach { d ->
                FilterChip(
                    selected = targetDays == d,
                    onClick = { onTargetDaysChange(d) },
                    label = { Text("${d}天", fontSize = 12.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = daysInputText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(4)
                    daysInputText = filtered
                    val parsed = filtered.toIntOrNull()
                    if (parsed != null && parsed >= 1) {
                        onTargetDaysChange(parsed.coerceIn(1, 1000))
                    }
                },
                label = { Text("自定义天数") },
                placeholder = { Text("例如 23") },
                singleLine = true,
                modifier = Modifier
                    .width(130.dp)
                    .testTag("input_target_days")
            )
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "目标考期: $targetDate (距今 $targetDays 天)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AcademicBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Available Hours
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "每日可用学习时长",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AcademicBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "$dailyHoursAvailable 小时 / 天 (${(dailyHoursAvailable * 60).toInt()} 分钟)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AcademicBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Slider(
            value = dailyHoursAvailable,
            onValueChange = { onDailyHoursChange((it * 2).toInt() / 2f) },
            valueRange = 1.0f..14.0f,
            steps = 25,
            modifier = Modifier.testTag("slider_daily_hours")
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "AI 智能配比建议：30% 核心教材精读 (${(dailyHoursAvailable * 18).toInt()}m) · 50% 刷题内化 (${(dailyHoursAvailable * 30).toInt()}m) · 20% 艾宾浩斯复盘 (${(dailyHoursAvailable * 12).toInt()}m)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase Addition Mode
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "阶段增量接续模式",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "开启后保留现有已打卡任务与打卡表，新勾选资料将接续在后续日程，不重新从第1天覆盖",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPhaseAdditionMode,
                    onCheckedChange = onTogglePhaseMode,
                    colors = SwitchDefaults.colors(checkedThumbColor = AcademicBlue)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Tab 3: Strategy Notes & AI Provider
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StrategyAiTab(
    customNotes: String,
    onCustomNotesChange: (String) -> Unit,
    aiConfig: AiModelConfig,
    onOpenAiConfig: () -> Unit,
    onCopyCustomPrompt: () -> Unit
) {
    val quickPhrases = listOf(
        "偏重真题刷题内化",
        "错题多次滚动重做",
        "基础薄弱需循序渐进",
        "严格落实艾宾浩斯复盘",
        "重点突破高频难点考点",
        "网课倍速速通结合讲义",
        "算法大题纸上手写代码",
        "长难句精剖与词汇主动唤醒"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "自定义战略要求与提示词",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "清空要求",
                fontSize = 11.sp,
                color = AcademicBlue,
                modifier = Modifier
                    .clickable { onCustomNotesChange("") }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = customNotes,
            onValueChange = onCustomNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("input_custom_notes"),
            placeholder = { Text("例如：英语注重真题逻辑出题陷阱剖析；408数据结构侧重算法复杂度与手写实现...") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "点击快捷添加策略关键词：",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPhrases.forEach { phrase ->
                AssistChip(
                    onClick = {
                        val updated = if (customNotes.isBlank()) phrase else "$customNotes；$phrase"
                        onCustomNotesChange(updated)
                    },
                    label = { Text("+ $phrase", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // External AI Prompt Export Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AcademicPurple.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, AcademicPurple.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AcademicPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "外部 AI 专属提示词导出 (免配置 API Key)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcademicPurple
                    )
                }

                Text(
                    text = "一键将你在【教材题库】中选取的书目、在【进度微调】中指定的章节与天数，以及上方战略要求自动拼接为完整 Prompt。发给 DeepSeek (推荐深度思考版) 或 ChatGPT 即可生成标准 JSON，随后在契约页导入！",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onCopyCustomPrompt,
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicPurple),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📋 一键复制定制 AI 提示词 (带当前书目与细节)", fontSize = 11.5.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Provider info box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AcademicBlue.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AcademicBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "内置规划引擎: ${aiConfig.activeDisplayName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcademicBlue
                        )
                        Text(
                            text = "将根据你勾选的书籍、习题、网课及周期时间，智能测算每日配比",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenAiConfig,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("配置模型", fontSize = 11.sp)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Add Material Card Dialog with AI Ground Truth Syllabus Retrieval & Manual Verification
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMaterialCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: String, status: String, note: String, groundTruthCatalog: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("BOOK") }
    var selectedStatus by remember { mutableStateOf("一轮精读") }
    var note by remember { mutableStateOf("") }
    var groundTruthCatalog by remember { mutableStateOf("") }
    var isSearchingCatalog by remember { mutableStateOf(false) }
    var autoMatchedNotice by remember { mutableStateOf<String?>(null) }

    val types = listOf(
        "BOOK" to "📖 理论教材",
        "EXERCISE" to "📝 习题集册",
        "LECTURE" to "🎬 视频网课"
    )

    val statuses = listOf("未开始", "一轮精读", "刷题强化", "冲刺查漏", "已结项")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AcademicBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("添加备考资料卡片", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("资料 / 书籍 / 课程名称", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            autoMatchedNotice = null
                        },
                        placeholder = { Text("如：《王道数据结构》《张宇1000题》") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                isSearchingCatalog = true
                                val entry = AcademicBookCatalog.resolveBook(title)
                                val gt = AcademicBookCatalog.getAutoGeneratedCatalogGroundTruth(title)
                                groundTruthCatalog = gt
                                autoMatchedNotice = "已智能匹配考研官方大纲：${entry.canonicalTitle} (${entry.totalChapters}章/讲)"
                                isSearchingCatalog = false
                            }
                        },
                        enabled = title.isNotBlank() && !isSearchingCatalog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isSearchingCatalog) "检索中" else "智能搜目录", fontSize = 11.sp)
                    }
                }

                autoMatchedNotice?.let { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(msg, fontSize = 11.sp, color = AcademicGreen, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("资料类别", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("初始复习状态", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("章节/进度备注 (选填)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("例如：从第1章起 / 预计20天刷完") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "真实目录/题量基准 Ground Truth (选填)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AcademicBlue
                    )
                    if (groundTruthCatalog.isNotBlank()) {
                        TextButton(
                            onClick = { groundTruthCatalog = "" },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("清空", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "可点击上方'智能搜目录'自动获取考研统考大纲，或一键粘贴该书实际目录/章节总数/题量范围，排期时将作为事实基准喂给AI杜绝幻觉。",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = groundTruthCatalog,
                    onValueChange = { groundTruthCatalog = it },
                    placeholder = { Text("点击上方'智能搜目录'自动提取，或粘贴如：\n第1章 极限与连续 (共20题)\n第2章 导数与微分 (共35题)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), selectedType, selectedStatus, note.trim(), groundTruthCatalog.trim())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("确定添加并纳入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// -----------------------------------------------------------------------------------------
// Edit Material Card Dialog with AI Ground Truth Syllabus Retrieval & Manual Verification
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditMaterialCardDialog(
    card: MaterialCardItem,
    onDismiss: () -> Unit,
    onConfirm: (MaterialCardItem) -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var selectedType by remember { mutableStateOf(card.type) }
    var selectedStatus by remember { mutableStateOf(card.status) }
    var note by remember { mutableStateOf(card.note) }
    var groundTruthCatalog by remember { mutableStateOf(card.groundTruthCatalog) }
    var isSearchingCatalog by remember { mutableStateOf(false) }
    var autoMatchedNotice by remember { mutableStateOf<String?>(null) }

    val types = listOf(
        "BOOK" to "📖 理论教材",
        "EXERCISE" to "📝 习题集册",
        "LECTURE" to "🎬 视频网课"
    )

    val statuses = listOf("未开始", "一轮精读", "刷题强化", "冲刺查漏", "已结项")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = AcademicBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("修改资料卡片与事实基准", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("资料名称", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            autoMatchedNotice = null
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                isSearchingCatalog = true
                                val entry = AcademicBookCatalog.resolveBook(title)
                                val gt = AcademicBookCatalog.getAutoGeneratedCatalogGroundTruth(title)
                                groundTruthCatalog = gt
                                autoMatchedNotice = "已智能匹配考研官方大纲：${entry.canonicalTitle} (${entry.totalChapters}章/讲)"
                                isSearchingCatalog = false
                            }
                        },
                        enabled = title.isNotBlank() && !isSearchingCatalog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isSearchingCatalog) "检索中" else "智能搜目录", fontSize = 11.sp)
                    }
                }

                autoMatchedNotice?.let { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(msg, fontSize = 11.sp, color = AcademicGreen, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("类别", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = { selectedType = typeKey },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("状态", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("备注 (选填)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "真实目录/题量基准 Ground Truth (选填)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AcademicBlue
                    )
                    if (groundTruthCatalog.isNotBlank()) {
                        TextButton(
                            onClick = { groundTruthCatalog = "" },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("清空", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "内置AI事实基准与外部AI双重确认：生成规划Prompt时将直接以此为真理基准，彻底消除虚构章节与题号。",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = groundTruthCatalog,
                    onValueChange = { groundTruthCatalog = it },
                    placeholder = { Text("点击上方'智能搜目录'自动提取，或在此自定义粘贴真实目录") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            card.copy(
                                title = title.trim(),
                                type = selectedType,
                                status = selectedStatus,
                                note = note.trim(),
                                groundTruthCatalog = groundTruthCatalog.trim()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
