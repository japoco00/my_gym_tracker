package com.jacopo.mygymtracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.data.backup.BackupFile
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.data.withAllSetsCompleted
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.ThemeMode
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private val databases = mutableListOf<AppDatabase>()

    private lateinit var app: TestApp

    @Before
    fun setUp() {
        app = TestApp()
    }

    @After
    fun tearDown() = databases.forEach { it.close() }

    @Test
    fun importingAnExport_rebuildsTheSameData() = runTest {
        app.fill()
        val exported = app.backup.exportJson()

        val restored = TestApp()
        assertEquals(ImportResult.IMPORTED, restored.backup.importJson(exported))

        assertEquals(exported, restored.backup.exportJson())
        // A setting the export forgets would match in both files, so compare the settings themselves too.
        assertEquals(app.settings.observeSettings().first(), restored.settings.observeSettings().first())
    }

    @Test
    fun aVersion1File_importsWithDefaultsForLaterSettings() = runTest {
        app.fill()
        val exported = Json.parseToJsonElement(app.backup.exportJson()).jsonObject
        val v1Keys = setOf("weightUnit", "restSeconds", "plates")
        val v1Settings = JsonObject(exported.getValue("settings").jsonObject.filterKeys { it in v1Keys })
        val v1 = JsonObject(exported + mapOf("formatVersion" to JsonPrimitive(1), "settings" to v1Settings))

        val restored = TestApp()
        assertEquals(ImportResult.IMPORTED, restored.backup.importJson(v1.toString()))

        val defaults = UserSettings()
        val expected = app.settings.observeSettings().first().copy(
            themeMode = defaults.themeMode,
            dynamicColor = defaults.dynamicColor,
            autoStartRest = defaults.autoStartRest,
            keepScreenOn = defaults.keepScreenOn,
        )
        assertEquals(expected, restored.settings.observeSettings().first())
    }

    @Test
    fun import_replacesWhatWasThere() = runTest {
        app.fill()
        val exported = app.backup.exportJson()
        app.routines.saveRoutine(routineOf("Arms", newExercise("Curl") to listOf(PlannedSet(reps = 10))))

        assertEquals(ImportResult.IMPORTED, app.backup.importJson(exported))

        assertEquals(listOf("Push"), app.routines.observeRoutines().first().map { it.name })
        assertEquals(listOf("Plank", "Squat"), app.database.exerciseDao().getAll().map { it.name })
    }

    @Test
    fun invalidFiles_areRefusedAndChangeNothing() = runTest {
        app.fill()
        val exported = app.backup.exportJson()
        val backup = Json.decodeFromString<BackupFile>(exported)
        val invalid = listOf("not a backup") + listOf(
            backup.copy(formatVersion = BackupFile.CURRENT_FORMAT_VERSION + 1),
            backup.copy(exercises = backup.exercises.filter { it.name != "Squat" }),
            backup.copy(exercises = backup.exercises + backup.exercises.first().copy(name = "SQUAT")),
        ).map { Json.encodeToString(it) }

        invalid.forEach { assertEquals(ImportResult.INVALID_FILE, app.backup.importJson(it)) }

        assertEquals(exported, app.backup.exportJson())
    }

    @Test
    fun import_isRefusedWhileAWorkoutIsInProgress() = runTest {
        app.fill()
        val exported = app.backup.exportJson()
        app.workouts.startWorkout(app.routines.observeRoutines().first().single())

        assertEquals(ImportResult.WORKOUT_IN_PROGRESS, app.backup.importJson(exported))

        assertEquals("Push", app.workouts.observeActiveWorkout().first()?.routineName)
    }

    /** One app's worth of storage: a fresh database and settings. */
    private inner class TestApp {
        val database = createTestDatabase().also { databases += it }
        val settings = SettingsRepository(InMemoryPreferencesDataStore())
        val routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        val workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
        val backup = BackupRepository(
            database = database,
            exerciseDao = database.exerciseDao(),
            routineDao = database.routineDao(),
            workoutDao = database.workoutDao(),
            settingsRepository = settings,
            contentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver,
        )

        /** Every field away from its default, so a field lost on the way shows up in the export. */
        suspend fun fill() {
            settings.setWeightUnit(WeightUnit.LB)
            settings.setRestSeconds(120)
            settings.togglePlate(WeightUnit.KG, 25.0)
            settings.setThemeMode(ThemeMode.DARK)
            settings.setDynamicColor(false)
            settings.setAutoStartRest(false)
            settings.setKeepScreenOn(false)
            val squat = Exercise(name = "Squat", category = Category.LEGS, note = "High bar", isBarbell = true)
            val plank = newExercise("Plank", Category.CORE).copy(trackingType = TrackingType.DURATION)
            val routine = routineOf(
                "Push",
                squat to listOf(PlannedSet(reps = 5, weightKg = 100.0)),
                plank to listOf(PlannedSet(durationSeconds = 60)),
            ).let { it.copy(note = "Heavy day", exercises = it.exercises.map { e -> e.copy(restSeconds = 180) }) }
            routines.saveRoutine(routine)
            val sessionId = workouts.startWorkout(routines.observeRoutines().first().single(), Instant.ofEpochMilli(1_000))!!
            workouts.finishWorkout(workouts.getWorkout(sessionId)!!.withAllSetsCompleted(), Instant.ofEpochMilli(2_000))
        }
    }
}
