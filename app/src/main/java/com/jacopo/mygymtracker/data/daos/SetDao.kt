package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.jacopo.mygymtracker.data.model.ExerciseSet

@Dao
interface SetDao {
    @Insert
    fun insert(set: List<ExerciseSet>) : List<Long>

    @Delete
    fun delete(set: List<ExerciseSet>)

    @Transaction
    @Query("SELECT * FROM ExerciseSet")
    fun getAll(): List<ExerciseSet>
}