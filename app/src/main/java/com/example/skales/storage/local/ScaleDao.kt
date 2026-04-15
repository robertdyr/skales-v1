package com.example.skales.storage.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScaleDao {
    @Query("SELECT * FROM scales ORDER BY updatedAt DESC")
    fun observeScales(): Flow<List<ScaleEntity>>

    @Query("SELECT * FROM scales WHERE id = :id")
    suspend fun getScaleById(id: String): ScaleEntity?

    @Upsert
    suspend fun upsert(scale: ScaleEntity)

    @Query("DELETE FROM scales WHERE id = :id")
    suspend fun deleteById(id: String)
}
