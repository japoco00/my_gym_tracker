package com.jacopo.mygymtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseCrossRef
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: RoutineExerciseCrossRef)

    @Query("DELETE FROM routine_exercise_cross_ref WHERE routineId = :routineId")
    suspend fun deleteCrossRefsForRoutine(routineId: Int)

    @Transaction
    @Query("SELECT * FROM routines")
    fun observeRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE routineId = :routineId")
    suspend fun getRoutineWithExercises(routineId: Int): RoutineWithExercises?
}
