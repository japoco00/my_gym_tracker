package com.jacopo.mygymtracker.data.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.backup.BackupFile
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.local.dao.BodyWeightDao
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.local.dao.RoutineDao
import com.jacopo.mygymtracker.data.local.dao.WorkoutDao
import com.jacopo.mygymtracker.data.mapper.toBackup
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

enum class ImportResult { IMPORTED, INVALID_FILE, WORKOUT_IN_PROGRESS }

/**
 * Saves settings, catalog, routines, finished workouts and body weights to a JSON [BackupFile], and restores one.
 * Restoring replaces everything; a workout in progress blocks it, since the backup can't hold one.
 */
class BackupRepository(
    private val database: AppDatabase,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao,
    private val bodyWeightDao: BodyWeightDao,
    private val settingsRepository: SettingsRepository,
    private val contentResolver: ContentResolver,
) {
    /** Writes the backup to [uri], a new document the user created. Returns `false` if it can't be written. */
    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val text = exportJson()
        try {
            contentResolver.openOutputStream(uri)?.writer()?.use { it.write(text) } != null
        } catch (e: IOException) {
            false
        }
    }

    /** Restores the backup at [uri], a document the user picked; see [importJson]. */
    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = try {
            contentResolver.openInputStream(uri)?.reader()?.use { it.readText() }
        } catch (e: IOException) {
            null
        }
        text?.let { importJson(it) } ?: ImportResult.INVALID_FILE
    }

    suspend fun exportJson(): String {
        val settings = settingsRepository.observeSettings().first()
        val backup = database.withTransaction {
            BackupFile(
                formatVersion = BackupFile.CURRENT_FORMAT_VERSION,
                settings = settings.toBackup(),
                exercises = exerciseDao.getAll().map { it.toDomain().toBackup() },
                routines = routineDao.getAllWithExercises().map { it.toDomain().toBackup() },
                workouts = workoutDao.getFinishedWithExercises().map { it.toDomain().toBackup() },
                bodyWeights = bodyWeightDao.getAll().map { it.toDomain().toBackup() },
            )
        }
        return json.encodeToString(backup)
    }

    /**
     * Replaces everything with the backup in [text]. The whole file is checked first,
     * and the database is swapped in one transaction, so a failure changes nothing.
     */
    suspend fun importJson(text: String): ImportResult {
        val backup = try {
            json.decodeFromString<BackupFile>(text).also { it.validate() }
        } catch (e: IllegalArgumentException) { // Includes SerializationException.
            return ImportResult.INVALID_FILE
        }
        val imported = database.withTransaction {
            if (workoutDao.hasActive()) return@withTransaction false
            workoutDao.deleteAll()
            routineDao.deleteAll()
            exerciseDao.deleteAll()
            bodyWeightDao.deleteAll()
            val exercises = backup.exercises.associate { saved ->
                val exercise = saved.toDomain()
                saved.name to exercise.copy(id = exerciseDao.insert(exercise.toEntity()).toInt())
            }
            val routineIds = backup.routines.associate { saved ->
                val routine = saved.toDomain(exercises)
                val routineId = routineDao.insert(routine.toEntity()).toInt()
                routineDao.writeExercises(routineId, routine.exercises, exerciseDao)
                saved.id to routineId
            }
            backup.workouts.forEach { saved ->
                val workout = saved.toDomain(exercises, routineIds)
                val sessionId = workoutDao.insertSession(workout.toEntity()).toInt()
                workoutDao.writeExercises(sessionId, workout.exercises, exerciseDao)
            }
            backup.bodyWeights.forEach { bodyWeightDao.upsert(it.toDomain().toEntity()) }
            true
        }
        if (!imported) return ImportResult.WORKOUT_IN_PROGRESS
        settingsRepository.replaceAll(backup.settings.toDomain())
        return ImportResult.IMPORTED
    }

    private companion object {
        // Defaults are written too, so the file states every value instead of relying on the app's defaults.
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}

/** Throws [IllegalArgumentException] for a newer format, duplicate exercise names, or unknown exercises. */
private fun BackupFile.validate() {
    require(formatVersion in 1..BackupFile.CURRENT_FORMAT_VERSION) { "Unsupported format $formatVersion" }
    val names = exercises.map { it.name }
    require(names.distinctBy { it.lowercase() }.size == names.size) { "Duplicate exercise names" }
    val referenced = (routines.flatMap { it.exercises } + workouts.flatMap { it.exercises }).map { it.exercise }
    require(names.containsAll(referenced)) { "Unknown exercise" }
}
