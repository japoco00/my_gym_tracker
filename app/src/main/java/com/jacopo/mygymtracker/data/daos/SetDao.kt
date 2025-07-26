package com.jacopo.mygymtracker.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jacopo.mygymtracker.data.model.SetEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(set: SetEntry): Long

    @Update
    suspend fun update(set: SetEntry)

    @Delete
    suspend fun delete(set: List<SetEntry>)

    @Transaction
    @Query("SELECT * FROM SetEntry")
    fun getAll(): Flow<List<SetEntry>>
}