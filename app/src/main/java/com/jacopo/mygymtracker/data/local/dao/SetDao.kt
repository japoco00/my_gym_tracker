package com.jacopo.mygymtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jacopo.mygymtracker.data.local.entity.SetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<SetEntity>)

    @Query("DELETE FROM sets WHERE routineId = :routineId")
    suspend fun deleteForRoutine(routineId: Int)

    @Query("SELECT * FROM sets WHERE routineId = :routineId ORDER BY setId")
    suspend fun getForRoutine(routineId: Int): List<SetEntity>

    @Query("SELECT * FROM sets ORDER BY setId")
    fun observeAll(): Flow<List<SetEntity>>
}
