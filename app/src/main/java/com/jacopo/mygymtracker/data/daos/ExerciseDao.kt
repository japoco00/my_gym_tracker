package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.jacopo.mygymtracker.data.model.Exercise
import com.jacopo.mygymtracker.data.model.ExerciseWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: List<Exercise>): List<Long>

    @Delete
    suspend fun delete(exercise: List<Exercise>)

    @Transaction
    @Query("SELECT * FROM Exercise")
    fun getAll(): Flow<List<ExerciseWithSets>>
}