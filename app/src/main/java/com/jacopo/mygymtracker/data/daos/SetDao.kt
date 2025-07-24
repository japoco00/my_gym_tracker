package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.jacopo.mygymtracker.data.model.ExerciseSet
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Insert
    suspend fun insert(set: List<ExerciseSet>): List<Long>

    @Delete
    suspend fun delete(set: List<ExerciseSet>)

    @Transaction
    @Query("SELECT * FROM ExerciseSet")
    fun getAll(): Flow<List<ExerciseSet>>
}