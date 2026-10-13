package com.example.data

import com.example.data.dao.StudySyncDao
import com.example.data.entity.ContractEntity
import com.example.data.entity.ExecutionLogEntity
import com.example.data.entity.ProfileEntity
import com.example.engine.AcademicPlanningEngine
import com.example.model.ExamPresets
import com.example.model.StudyRawInputs
import com.example.model.StudySyncContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudySyncRepository(
    private val dao: StudySyncDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val allProfiles: Flow<List<ProfileEntity>> = dao.getAllProfiles()

    init {
        scope.launch {
            seedDefaultProfilesAndPlansIfEmpty()
        }
    }

    suspend fun seedDefaultProfilesAndPlansIfEmpty() = withContext(Dispatchers.IO) {
        val existingA = dao.getProfileById("Profile_A")
        if (existingA == null) {
            val profA = ProfileEntity(
                profileId = "Profile_A",
                displayName = "Profile A (You)",
                roleTitle = "Primary Candidate",
                targetExam = "GRE General (Quant + Verbal 330+)",
                avatarColorHex = "#0284C7"
            )
            dao.insertProfile(profA)

            // Seed initial contract for Profile A
            val presetA = ExamPresets.presets[0] // GRE
            val initialContractA = AcademicPlanningEngine.generateRigorousPlan("Profile_A", presetA.rawInputs)
            saveContractDirect(initialContractA)
        }

        val existingB = dao.getProfileById("Profile_B")
        if (existingB == null) {
            val profB = ProfileEntity(
                profileId = "Profile_B",
                displayName = "Profile B (Peer)",
                roleTitle = "Study Partner",
                targetExam = "GATE CS & IT Entrance Exam",
                avatarColorHex = "#7C3AED"
            )
            dao.insertProfile(profB)

            // Seed initial contract for Profile B
            val presetB = ExamPresets.presets[3] // GATE CS
            val initialContractB = AcademicPlanningEngine.generateRigorousPlan("Profile_B", presetB.rawInputs)
            // Mark a few tasks completed in Peer profile to demonstrate real comparative metrics right away!
            val updatedSchedule = initialContractB.dailySchedule.mapIndexed { index, mission ->
                if (index == 0) {
                    mission.copy(
                        tasks = mission.tasks.map { it.copy(completed = true, actualMinutes = it.estimatedMinutes) }
                    )
                } else if (index == 1) {
                    mission.copy(
                        tasks = mission.tasks.mapIndexed { tIdx, task ->
                            if (tIdx == 0) task.copy(completed = true, actualMinutes = task.estimatedMinutes) else task
                        }
                    )
                } else mission
            }
            val contractBWithProgress = initialContractB.copy(dailySchedule = updatedSchedule)
            val recalibratedB = AcademicPlanningEngine.recalibratePlan(contractBWithProgress, emptyList())
            saveContractDirect(recalibratedB)
        }

        // Synchronize comparison metrics across both contracts
        syncComparisonBetweenProfiles()
    }

    fun getContractFlow(profileId: String): Flow<StudySyncContract?> {
        return dao.getContractFlow(profileId).map { entity ->
            entity?.let { StudySyncJsonParser.fromJson(it.jsonPayload) }
        }
    }

    fun getExecutionLogsFlow(profileId: String): Flow<List<ExecutionLogEntity>> {
        return dao.getExecutionLogsFlow(profileId)
    }

    suspend fun saveContract(contract: StudySyncContract) = withContext(Dispatchers.IO) {
        saveContractDirect(contract)
        syncComparisonBetweenProfiles()
    }

    private suspend fun saveContractDirect(contract: StudySyncContract) {
        val json = StudySyncJsonParser.toJson(contract)
        val entity = ContractEntity(
            profileId = contract.profileId,
            jsonPayload = json,
            title = contract.planOverview.title,
            targetDate = contract.planOverview.targetDate,
            completionRate = contract.planOverview.completionRate,
            currentPaceStatus = contract.planOverview.currentPaceStatus,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertContract(entity)
    }

    suspend fun recordTaskExecution(
        profileId: String,
        dayNumber: Int,
        taskId: String,
        actualMinutes: Int,
        errorRatePercent: Int,
        difficultyFeedback: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val currentContract = dao.getContractDirect(profileId)?.let {
            StudySyncJsonParser.fromJson(it.jsonPayload)
        } ?: return@withContext

        var taskTitle = "Study Task"
        var material = ""
        var taskType = "practice"
        var expectedMinutes = 45

        // Update task completion and telemetry in the schedule
        val updatedSchedule = currentContract.dailySchedule.map { mission ->
            if (mission.day == dayNumber) {
                val updatedTasks = mission.tasks.map { task ->
                    if (task.id == taskId || (task.id.isEmpty() && taskId == "${mission.day}-${task.material}")) {
                        taskTitle = task.scope
                        material = task.material
                        taskType = task.taskType
                        expectedMinutes = task.estimatedMinutes
                        task.copy(
                            completed = true,
                            actualMinutes = actualMinutes,
                            errorRatePercent = errorRatePercent,
                            difficultyFeedback = difficultyFeedback
                        )
                    } else task
                }
                mission.copy(tasks = updatedTasks)
            } else mission
        }

        // Insert execution log
        val log = ExecutionLogEntity(
            profileId = profileId,
            dayNumber = dayNumber,
            taskId = taskId,
            taskTitle = taskTitle,
            material = material,
            taskType = taskType,
            expectedMinutes = expectedMinutes,
            actualMinutes = actualMinutes,
            errorRatePercent = errorRatePercent,
            difficultyFeedback = difficultyFeedback,
            notes = notes
        )
        dao.insertExecutionLog(log)

        // Dynamically recalibrate plan
        val allLogs = dao.getExecutionLogsDirect(profileId)
        val intermediateContract = currentContract.copy(dailySchedule = updatedSchedule)
        val recalibrated = AcademicPlanningEngine.recalibratePlan(intermediateContract, allLogs)

        saveContract(recalibrated)
    }

    suspend fun toggleTaskCompletion(
        profileId: String,
        dayNumber: Int,
        taskId: String
    ) = withContext(Dispatchers.IO) {
        val currentContract = dao.getContractDirect(profileId)?.let {
            StudySyncJsonParser.fromJson(it.jsonPayload)
        } ?: return@withContext

        val updatedSchedule = currentContract.dailySchedule.map { mission ->
            if (mission.day == dayNumber) {
                val updatedTasks = mission.tasks.mapIndexed { idx, task ->
                    val fallbackId = "d${mission.day}_t$idx"
                    if (task.id == taskId || (task.id.isBlank() && fallbackId == taskId)) {
                        val newCompleted = !task.completed
                        val actual = if (newCompleted && task.actualMinutes == 0) task.estimatedMinutes else task.actualMinutes
                        task.copy(id = if (task.id.isBlank()) fallbackId else task.id, completed = newCompleted, actualMinutes = actual)
                    } else task
                }
                mission.copy(tasks = updatedTasks)
            } else mission
        }

        val allLogs = dao.getExecutionLogsDirect(profileId)
        val intermediate = currentContract.copy(dailySchedule = updatedSchedule)
        val recalibrated = AcademicPlanningEngine.recalibratePlan(intermediate, allLogs)
        saveContract(recalibrated)
    }

    suspend fun syncComparisonBetweenProfiles() = withContext(Dispatchers.IO) {
        val entityA = dao.getContractDirect("Profile_A")
        val entityB = dao.getContractDirect("Profile_B")

        val contractA = entityA?.let { StudySyncJsonParser.fromJson(it.jsonPayload) }
        val contractB = entityB?.let { StudySyncJsonParser.fromJson(it.jsonPayload) }

        if (contractA != null && contractB != null) {
            val comparison = AcademicPlanningEngine.computeComparison(contractA, contractB)
            if (contractA.dashboardComparison != comparison) {
                saveContractDirect(contractA.copy(dashboardComparison = comparison))
            }
            if (contractB.dashboardComparison != comparison) {
                saveContractDirect(contractB.copy(dashboardComparison = comparison))
            }
        }
    }
}
