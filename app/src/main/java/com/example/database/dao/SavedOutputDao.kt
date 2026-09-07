package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entity.SavedOutputEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedOutputDao {
    @Query("SELECT * FROM saved_outputs ORDER BY savedAt DESC")
    fun getAllSavedOutputs(): Flow<List<SavedOutputEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedOutput(output: SavedOutputEntity)

    @Query("DELETE FROM saved_outputs WHERE fileId = :fileId")
    suspend fun deleteSavedOutput(fileId: String)
}
