package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.database.entity.CustomToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomToolDao {
    @Query("SELECT * FROM custom_tools ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllTools(): Flow<List<CustomToolEntity>>

    @Query("SELECT * FROM custom_tools WHERE enabled = 1 ORDER BY sortOrder ASC, createdAt ASC")
    fun getEnabledTools(): Flow<List<CustomToolEntity>>

    @Query("SELECT * FROM custom_tools WHERE id = :id LIMIT 1")
    suspend fun getToolById(id: String): CustomToolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: CustomToolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<CustomToolEntity>)

    @Update
    suspend fun updateTool(tool: CustomToolEntity)

    @Query("DELETE FROM custom_tools WHERE id = :id")
    suspend fun deleteToolById(id: String)

    @Query("UPDATE custom_tools SET enabled = :enabled WHERE id = :id")
    suspend fun setToolEnabled(id: String, enabled: Boolean)

    @Query("UPDATE custom_tools SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int)
}
