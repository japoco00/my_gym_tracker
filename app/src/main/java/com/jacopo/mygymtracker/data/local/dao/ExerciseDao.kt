package com.jacopo.mygymtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jacopo.mygymtracker.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE exerciseId = :exerciseId")
    suspend fun delete(exerciseId: Int)

    /** `true` if any routine or logged workout references the exercise. */
    @Query(
        """
        SELECT EXISTS(SELECT 1 FROM routine_exercises WHERE exerciseId = :exerciseId)
            OR EXISTS(SELECT 1 FROM workout_exercises WHERE exerciseId = :exerciseId)
        """,
    )
    suspend fun isInUse(exerciseId: Int): Boolean

    /** Case-insensitive lookup, thanks to the `NOCASE` collation of the name column. */
    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    fun observeById(exerciseId: Int): Flow<ExerciseEntity?>
}