package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ContractEntity
import com.example.data.entity.ExecutionLogEntity
import com.example.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySyncDao {
    // Profiles
    @Query("SELECT * FROM profiles ORDER BY profileId ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // Contract Plans
    @Query("SELECT * FROM contracts WHERE profileId = :profileId LIMIT 1")
    fun getContractFlow(profileId: String): Flow<ContractEntity?>

    @Query("SELECT * FROM contracts WHERE profileId = :profileId LIMIT 1")
    suspend fun getContractDirect(profileId: String): ContractEntity?

    @Query("SELECT * FROM contracts")
    fun getAllContractsFlow(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts")
    suspend fun getAllContractsDirect(): List<ContractEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity)

    @Query("DELETE FROM contracts WHERE profileId = :profileId")
    suspend fun deleteContract(profileId: String)

    // Execution Logs
    @Query("SELECT * FROM execution_logs WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getExecutionLogsFlow(profileId: String): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getExecutionLogsDirect(profileId: String): List<ExecutionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutionLog(log: ExecutionLogEntity)

    @Query("DELETE FROM execution_logs WHERE profileId = :profileId")
    suspend fun clearLogsForProfile(profileId: String)
}
