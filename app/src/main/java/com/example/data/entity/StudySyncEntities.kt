package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val profileId: String, // "Profile_A" or "Profile_B"
    val displayName: String,
    val roleTitle: String,
    val targetExam: String,
    val avatarColorHex: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contracts")
data class ContractEntity(
    @PrimaryKey val profileId: String,
    val jsonPayload: String,
    val title: String,
    val targetDate: String,
    val completionRate: String,
    val currentPaceStatus: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val dayNumber: Int,
    val taskId: String,
    val taskTitle: String,
    val material: String,
    val taskType: String,
    val expectedMinutes: Int,
    val actualMinutes: Int,
    val errorRatePercent: Int,
    val difficultyFeedback: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
