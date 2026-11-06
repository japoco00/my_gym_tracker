package com.jacopo.mygymtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jacopo.mygymtracker.data.local.entity.BodyWeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    /** Oldest first. */
    @Query("SELECT * FROM body_weights ORDER BY dateEpochDay")
    fun observeAll(): Flow<List<BodyWeightEntity>>

    /** Adds the day's entry, or replaces it if that day already has one. */
    @Upsert
    suspend fun upsert(entry: BodyWeightEntity)

    @Query("DELETE FROM body_weights WHERE dateEpochDay = :dateEpochDay")
    suspend fun delete(dateEpochDay: Long)

    @Query("SELECT * FROM body_weights ORDER BY dateEpochDay")
    suspend fun getAll(): List<BodyWeightEntity>

    @Query("DELETE FROM body_weights")
    suspend fun deleteAll()
}
