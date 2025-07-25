package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.jacopo.mygymtracker.data.model.Routine
import com.jacopo.mygymtracker.data.model.RoutineWithExercisesWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insert(routine: Routine): Long

    @Delete
    suspend fun delete(routine: Routine)

    @Transaction
    @Query(
        "SELECT * FROM Routine WHERE id = :id"
    )
    fun getRoutineById(id: Int): Flow<RoutineWithExercisesWithSets?>

    @Transaction
    @Query("SELECT * FROM Routine")
    fun getAllRoutines(): Flow<List<RoutineWithExercisesWithSets>>
}