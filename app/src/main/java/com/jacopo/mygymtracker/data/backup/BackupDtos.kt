package com.jacopo.mygymtracker.data.backup

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.ThemeMode
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.serialization.Serializable

/**
 * The backup file, stored as JSON: settings, catalog, routines, finished workouts and body weights.
 * Exercises are referenced by name; routines by an id that only means something inside the file.
 *
 * This is a file format: change these classes only together with [CURRENT_FORMAT_VERSION].
 * The import refuses newer versions, and a new field needs a default so older files still import.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val settings: BackupSettings,
    val exercises: List<BackupExercise>,
    val routines: List<BackupRoutine>,
    val workouts: List<BackupWorkout>,
    val bodyWeights: List<BackupBodyWeight> = emptyList(),
) {
    companion object {
        /**
         * 2 added the theme, dynamic colour, rest auto-start and keep-screen-on settings.
         * 3 added set types, RPE and body weights.
         */
        const val CURRENT_FORMAT_VERSION = 3
    }
}

/** The fields added after version 1 default to the [UserSettings] defaults, so a version 1 file still imports. */
@Serializable
data class BackupSettings(
    val weightUnit: WeightUnit,
    val restSeconds: Int,
    val plates: Map<WeightUnit, List<Double>>,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val autoStartRest: Boolean = true,
    val keepScreenOn: Boolean = true,
)

@Serializable
data class BackupExercise(
    val name: String,
    val category: Category,
    val trackingType: TrackingType,
    val note: String?,
    val isBarbell: Boolean,
)

/** [id] links workouts to their routine within the file; the import assigns a new one. */
@Serializable
data class BackupRoutine(
    val id: Int,
    val name: String,
    val note: String?,
    val exercises: List<BackupExerciseSets>,
)

/** A finished workout: every set in it counts as completed. */
@Serializable
data class BackupWorkout(
    val routineId: Int?,
    val routineName: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val exercises: List<BackupExerciseSets>,
)

/** A catalog exercise, by name, with its ordered sets: planned in a routine, performed in a workout. */
@Serializable
data class BackupExerciseSets(
    val exercise: String,
    val restSeconds: Int?,
    val sets: List<BackupSet>,
)

/** [rpe] is only ever set on a workout's sets. */
@Serializable
data class BackupSet(
    val reps: Int,
    val weightKg: Double,
    val durationSeconds: Int,
    val type: SetType = SetType.NORMAL,
    val rpe: Double? = null,
)

/** [dateEpochDay] is `LocalDate.toEpochDay()`. */
@Serializable
data class BackupBodyWeight(
    val dateEpochDay: Long,
    val weightKg: Double,
)
