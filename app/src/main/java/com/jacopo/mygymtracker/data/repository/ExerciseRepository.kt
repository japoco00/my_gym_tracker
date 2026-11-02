package com.jacopo.mygymtracker.data.repository

import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Access to the reusable exercise catalog. */
class ExerciseRepository(
    private val database: AppDatabase,
    private val exerciseDao: ExerciseDao,
) {
    /** Emits the whole catalog, sorted by name. */
    fun observeCatalog(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { exercises -> exercises.map { it.toDomain() } }

    /**
     * Adds [exercise] if it has no id, otherwise renames/recategorises it everywhere it is used.
     * Returns `false`, changing nothing, if another exercise already has the same name (ignoring case).
     */
    suspend fun saveExercise(exercise: Exercise): Boolean = database.withTransaction {
        val trimmed = exercise.copy(name = exercise.name.trim())
        val sameName = exerciseDao.findByName(trimmed.name)
        if (sameName != null && sameName.exerciseId != trimmed.id) return@withTransaction false
        if (trimmed.id == null) exerciseDao.insert(trimmed.toEntity()) else exerciseDao.update(trimmed.toEntity())
        true
    }

    /** `true` if a routine or a logged workout uses the exercise, so it cannot be deleted. */
    suspend fun isInUse(exerciseId: Int): Boolean = exerciseDao.isInUse(exerciseId)

    /** Deletes an unused exercise. Returns `false`, keeping it, if a routine or workout uses it. */
    suspend fun deleteExercise(exerciseId: Int): Boolean = database.withTransaction {
        if (exerciseDao.isInUse(exerciseId)) return@withTransaction false
        exerciseDao.delete(exerciseId)
        true
    }
}

/**
 * Returns the catalog id of [exercise]. An unsaved exercise reuses the catalog entry with the
 * same name (ignoring case), or is added to the catalog. Saved exercises are never modified here.
 * Call inside a transaction.
 */
internal suspend fun ExerciseDao.resolveCatalogId(exercise: Exercise): Int =
    exercise.id
        ?: findByName(exercise.name)?.exerciseId
        ?: insert(exercise.toEntity()).toInt()