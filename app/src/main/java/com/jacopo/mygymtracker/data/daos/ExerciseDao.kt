package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.jacopo.mygymtracker.data.model.ExerciseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(exerciseEntry: ExerciseEntry): Long

    @Update
    suspend fun update(exerciseEntry: ExerciseEntry)

    @Delete
    suspend fun delete(exerciseEntry: List<ExerciseEntry>)

    @Query("SELECT * FROM ExerciseEntry")
    fun getAll(): Flow<List<ExerciseEntry>>
}