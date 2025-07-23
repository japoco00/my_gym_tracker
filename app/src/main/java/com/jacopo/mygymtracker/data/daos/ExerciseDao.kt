package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.jacopo.mygymtracker.data.model.Exercise
import com.jacopo.mygymtracker.data.model.ExerciseWithSets

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: List<Exercise>) : List<Long>

    @Delete
    suspend fun delete(exercise: List<Exercise>)

    @Transaction
    @Query("SELECT * FROM Exercise")
    suspend fun getAll(): List<ExerciseWithSets>
}