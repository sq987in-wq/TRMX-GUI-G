package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.database.entity.RecentJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentJobDao {
    @Query("SELECT * FROM recent_jobs ORDER BY updatedAt DESC LIMIT 50")
    fun getAllRecentJobs(): Flow<List<RecentJobEntity>>

    @Query("SELECT * FROM recent_jobs WHERE jobId = :jobId LIMIT 1")
    suspend fun getJobById(jobId: String): RecentJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(job: RecentJobEntity)

    @Update
    suspend fun update(job: RecentJobEntity)

    @Query("DELETE FROM recent_jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM recent_jobs")
    suspend fun clearAllJobs()
}
