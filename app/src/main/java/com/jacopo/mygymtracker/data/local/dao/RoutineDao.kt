package com.jacopo.mygymtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jacopo.mygymtracker.data.local.entity.PlannedSetEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE routineId = :routineId")
    suspend fun deleteById(routineId: Int)

    @Insert
    suspend fun insertRoutineExercise(routineExercise: RoutineExerciseEntity): Long

    @Insert
    suspend fun insertPlannedSets(sets: List<PlannedSetEntity>)

    /** Removes every exercise link of a routine. Planned sets cascade. */
    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Int)

    @Transaction
    @Query("SELECT * FROM routines ORDER BY routineId")
    fun observeAllWithExercises(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE routineId = :routineId")
    suspend fun getWithExercises(routineId: Int): RoutineWithExercises?
}