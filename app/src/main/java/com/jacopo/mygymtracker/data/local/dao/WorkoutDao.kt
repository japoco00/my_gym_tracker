package com.jacopo.mygymtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jacopo.mygymtracker.data.local.entity.ExerciseHistoryRow
import com.jacopo.mygymtracker.data.local.entity.LoggedSetEntity
import com.jacopo.mygymtracker.data.local.entity.WorkoutExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.WorkoutSessionEntity
import com.jacopo.mygymtracker.data.local.entity.WorkoutSessionWithExercises
import com.jacopo.mygymtracker.data.local.entity.WorkoutSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE sessionId = :sessionId AND finishedAtEpochMs IS NULL)")
    suspend fun isActive(sessionId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE sessionId = :sessionId AND finishedAtEpochMs IS NOT NULL)")
    suspend fun isFinished(sessionId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE finishedAtEpochMs IS NULL)")
    suspend fun hasActive(): Boolean

    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long

    @Insert
    suspend fun insertLoggedSets(sets: List<LoggedSetEntity>)

    /** Removes every exercise of a session. Logged sets cascade. */
    @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun deleteWorkoutExercises(sessionId: Int)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMs IS NULL LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getWithExercises(sessionId: Int): WorkoutSessionWithExercises?

    @Query(
        """
        SELECT s.sessionId, s.routineName, s.startedAtEpochMs, s.finishedAtEpochMs,
               COUNT(ls.loggedSetId) AS setCount,
               COALESCE(SUM(ls.reps * ls.weightKg), 0) AS volumeKg
        FROM workout_sessions s
        LEFT JOIN workout_exercises we ON we.sessionId = s.sessionId
        LEFT JOIN logged_sets ls ON ls.workoutExerciseId = we.workoutExerciseId AND ls.isCompleted = 1
        WHERE s.finishedAtEpochMs IS NOT NULL
        GROUP BY s.sessionId
        ORDER BY s.startedAtEpochMs DESC
        """,
    )
    fun observeSummaries(): Flow<List<WorkoutSummaryRow>>

    /** The sets of [exerciseId] in its most recent finished session (first occurrence only). */
    @Query(
        """
        SELECT * FROM logged_sets
        WHERE workoutExerciseId = (
            SELECT we.workoutExerciseId
            FROM workout_exercises we
            JOIN workout_sessions s ON s.sessionId = we.sessionId
            WHERE we.exerciseId = :exerciseId AND s.finishedAtEpochMs IS NOT NULL
            ORDER BY s.startedAtEpochMs DESC, we.position
            LIMIT 1
        )
        ORDER BY position
        """,
    )
    suspend fun getLastSets(exerciseId: Int): List<LoggedSetEntity>

    /** Every completed set of [exerciseId] in finished sessions, oldest session first. */
    @Query(
        """
        SELECT s.sessionId, s.startedAtEpochMs, ls.reps, ls.weightKg, ls.durationSeconds
        FROM logged_sets ls
        JOIN workout_exercises we ON we.workoutExerciseId = ls.workoutExerciseId
        JOIN workout_sessions s ON s.sessionId = we.sessionId
        WHERE we.exerciseId = :exerciseId AND s.finishedAtEpochMs IS NOT NULL AND ls.isCompleted = 1
        ORDER BY s.startedAtEpochMs, we.position, ls.position
        """,
    )
    fun observeExerciseHistory(exerciseId: Int): Flow<List<ExerciseHistoryRow>>
}