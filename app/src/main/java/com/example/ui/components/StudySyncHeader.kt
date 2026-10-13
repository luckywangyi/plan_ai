package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiModelConfig
import com.example.model.AiProvider
import com.example.model.PlanOverview
import com.example.model.StudySyncContract
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle

@Composable
fun StudySyncHeader(
    activeProfileId: String,
    contract: StudySyncContract?,
    aiConfig: AiModelConfig = AiModelConfig(),
    onOpenAiConfigDialog: () -> Unit = {},
    onSwitchProfile: (String) -> Unit,
    onOpenIngestDialog: () -> Unit,
    onOpenImportDialog: () -> Unit = {},
    onRecalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App Identity & Memory Partition Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AcademicBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "StudySync AI",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "StudySync AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "自适应备考规划与智能辅导引擎",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                // Memory Isolation Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "独立隔离",
                            tint = if (activeProfileId == "Profile_A") AcademicBlue else AcademicPurple,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeProfileId == "Profile_A") "独立隔离: 画像 A" else "独立隔离: 画像 B",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Model Configuration Active Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (aiConfig.provider) {
                    AiProvider.GEMINI -> AcademicBlueSubtle.copy(alpha = 0.6f)
                    AiProvider.DEEPSEEK -> AcademicPurpleSubtle.copy(alpha = 0.6f)
                    AiProvider.OPENAI_COMPATIBLE -> MaterialTheme.colorScheme.surfaceVariant
                    AiProvider.OFFLINE_ACADEMIC -> Color(0xFFDCFCE7).copy(alpha = 0.7f)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAiConfigDialog)
                    .testTag("button_open_ai_config")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val engineColor = when (aiConfig.provider) {
                            AiProvider.DEEPSEEK -> AcademicPurple
                            AiProvider.OFFLINE_ACADEMIC -> Color(0xFF15803D)
                            else -> AcademicBlue
                        }
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "AI 引擎配置",
                            tint = engineColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "规划引擎: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = aiConfig.activeDisplayName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = engineColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = "切换/API导入",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Switcher (Notion Segmented Pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(3.dp)
            ) {
                ProfileTabButton(
                    title = "画像 A (本人)",
                    subtitle = "备考考生 A",
                    isSelected = activeProfileId == "Profile_A",
                    activeColor = AcademicBlue,
                    testTag = "tab_profile_a",
                    onClick = { onSwitchProfile("Profile_A") },
                    modifier = Modifier.weight(1f)
                )
                ProfileTabButton(
                    title = "画像 B (学友)",
                    subtitle = "结对学友",
                    isSelected = activeProfileId == "Profile_B",
                    activeColor = AcademicPurple,
                    testTag = "tab_profile_b",
                    onClick = { onSwitchProfile("Profile_B") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Plan Overview Metrics if plan is active
            if (contract != null) {
                val overview = contract.planOverview
                val progressFloat = (overview.completionRate.replace("%", "").toFloatOrNull() ?: 0f) / 100f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = overview.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "考前硬性截止: ${overview.targetDate} • 预算 ${overview.totalEstimatedHours}小时",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    PaceStatusBadge(status = overview.currentPaceStatus)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Completion Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (activeProfileId == "Profile_A") AcademicBlue else AcademicPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = overview.completionRate,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Comparative Quick Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "双向进度对比",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "A: ${contract.dashboardComparison.profileA_Progress}  vs  B: ${contract.dashboardComparison.profileB_Progress} (${contract.dashboardComparison.velocityDiff})",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenImportDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_import_json"),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, AcademicGreen.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "导入规划JSON",
                        modifier = Modifier.size(14.dp),
                        tint = AcademicGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导入JSON", fontSize = 11.sp, color = AcademicGreen, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onOpenIngestDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_ingest_plan"),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "录入资料",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("录入大纲", fontSize = 11.sp)
                }

                Button(
                    onClick = onRecalibrate,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("button_recalibrate"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeProfileId == "Profile_A") AcademicBlue else AcademicPurple
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "自适应重校",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("自适应重校", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileTabButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "tab_bg"
    )

    Surface(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) activeColor else Color.Gray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
