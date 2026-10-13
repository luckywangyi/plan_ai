package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MilestoneItem
import com.example.ui.theme.AcademicAmber
import com.example.ui.theme.AcademicAmberSubtle
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSubtle
import com.example.ui.theme.AcademicGreen
import com.example.ui.theme.AcademicGreenSubtle
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.AcademicPurpleSubtle
import com.example.ui.theme.AcademicRose
import com.example.ui.theme.AcademicRoseSubtle

@Composable
fun NotionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    calloutIcon: String? = null,
    badgeText: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null || calloutIcon != null || badgeText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (calloutIcon != null) {
                            Text(
                                text = calloutIcon,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun TaskTypeBadge(
    taskType: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (taskType.lowercase()) {
        "reading" -> Quadruple(AcademicBlueSubtle, AcademicBlue, "教材研读", Icons.AutoMirrored.Filled.MenuBook)
        "practice" -> Quadruple(AcademicAmberSubtle, AcademicAmber, "专项刷题", Icons.Default.Psychology)
        "lecture" -> Quadruple(AcademicPurpleSubtle, AcademicPurple, "网课精讲", Icons.Default.PlayCircle)
        "review" -> Quadruple(AcademicGreenSubtle, AcademicGreen, "间隔复习", Icons.Default.Autorenew)
        else -> Quadruple(AcademicBlueSubtle, AcademicBlue, taskType, Icons.AutoMirrored.Filled.MenuBook)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun PaceStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isAhead = status == "Ahead" || status == "进度超前"
    val isLagging = status == "Lagging" || status == "进度滞后"

    val (bgColor, textColor, displayLabel, icon) = when {
        isAhead -> Quadruple(AcademicGreenSubtle, AcademicGreen, "进度超前", Icons.Default.Speed)
        isLagging -> Quadruple(AcademicRoseSubtle, AcademicRose, "进度滞后", Icons.Default.Speed)
        else -> Quadruple(AcademicBlueSubtle, AcademicBlue, "按期推进", Icons.Default.CheckCircle)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = displayLabel,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Composable
fun MilestoneStepTrack(
    milestones: List<MilestoneItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        milestones.forEachIndexed { index, milestone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (milestone.completed) AcademicGreen
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (milestone.completed) AcademicGreen else MaterialTheme.colorScheme.outline,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (milestone.completed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "${(index + 1) * 25}%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = milestone.stage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (milestone.completed) AcademicGreen else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = milestone.targetDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = milestone.assessmentTopic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            if (index < milestones.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .width(2.dp)
                        .height(14.dp)
                        .background(
                            if (milestone.completed && milestones[index + 1].completed) AcademicGreen
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
