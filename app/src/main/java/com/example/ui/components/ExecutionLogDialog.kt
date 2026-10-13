package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ContractTask
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicRose

@Composable
fun ExecutionLogDialog(
    dayNumber: Int,
    task: ContractTask,
    onDismiss: () -> Unit,
    onSaveExecution: (actualMinutes: Int, errorRatePercent: Int, difficulty: String, notes: String) -> Unit
) {
    var actualMinutes by remember { mutableIntStateOf(if (task.actualMinutes > 0) task.actualMinutes else task.estimatedMinutes) }
    var errorRatePercent by remember { mutableIntStateOf(task.errorRatePercent) }
    var selectedDifficulty by remember { mutableStateOf(if (task.difficultyFeedback.isNotBlank()) task.difficultyFeedback else "适中") }
    var notes by remember { mutableStateOf("") }

    val difficulties = listOf("轻松", "适中", "吃力", "严重超负荷")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "记录任务做题执行数据",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "第 $dayNumber 天 • ${task.material}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = task.scope,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actual Time Spent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实际耗时",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "$actualMinutes 分钟 (计划: ${task.estimatedMinutes}分钟)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (actualMinutes > task.estimatedMinutes) AcademicRose else AcademicGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = actualMinutes.toFloat(),
                    onValueChange = { actualMinutes = it.toInt() },
                    valueRange = 10f..180f,
                    steps = 33,
                    modifier = Modifier.testTag("slider_actual_minutes")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error Rate (for problem sets / recall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "错题率 / 遗忘率",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "$errorRatePercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (errorRatePercent > 25) AcademicRose else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = errorRatePercent.toFloat(),
                    onValueChange = { errorRatePercent = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.testTag("slider_error_rate")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Difficulty Feedback
                Text(
                    text = "主观难度感知 (认知负荷)",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    difficulties.forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reflection Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("错因复盘 / 薄弱考点笔记 (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onSaveExecution(actualMinutes, errorRatePercent, selectedDifficulty, notes)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_save_execution"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue)
                ) {
                    Text("保存数据并触发动态重校", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
