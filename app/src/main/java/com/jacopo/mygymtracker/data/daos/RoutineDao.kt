package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jacopo.mygymtracker.data.model.RoutineEntry
import com.jacopo.mygymtracker.data.model.RoutineFullEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(routineEntry: RoutineEntry): Long

    @Update
    suspend fun update(routineEntry: RoutineEntry)
    @Delete
    suspend fun delete(routineEntry: RoutineEntry)

    @Transaction
    @Query(
        "SELECT * FROM RoutineEntry WHERE id = :id"
    )
    fun getRoutineById(id: Int): Flow<RoutineFullEntry?>

    @Transaction
    @Query("SELECT * FROM RoutineEntry")
    fun getAllRoutines(): Flow<List<RoutineFullEntry>>
}