package com.jacopo.mygymtracker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationsTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate4To5_keepsDataAndAssignsTrackingTypes() {
        helper.createDatabase(DB_NAME, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO exercises (exerciseId, name, category) VALUES
                (1, 'Bench Press', 'CHEST'), (2, 'pull-up', 'BACK'), (3, 'Plank', 'CORE')
                """,
            )
            db.execSQL("INSERT INTO workout_sessions (sessionId, routineName, startedAtEpochMs) VALUES (1, 'Push', 0)")
            db.execSQL("INSERT INTO workout_exercises (workoutExerciseId, sessionId, exerciseId, position) VALUES (1, 1, 1, 0)")
            db.execSQL(
                """
                INSERT INTO logged_sets (workoutExerciseId, position, reps, weightKg, isCompleted)
                VALUES (1, 0, 5, 100.0, 1)
                """,
            )
        }

        // Also validates the migrated schema against the v5 entities.
        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.query("SELECT name, trackingType FROM exercises ORDER BY exerciseId").use { cursor ->
            val types = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
            assertEquals(
                listOf("Bench Press" to "WEIGHT_REPS", "pull-up" to "REPS", "Plank" to "DURATION"),
                types,
            )
        }
        db.query("SELECT reps, weightKg, durationSeconds FROM logged_sets").use { cursor ->
            cursor.moveToFirst()
            assertEquals(5, cursor.getInt(0))
            assertEquals(100.0, cursor.getDouble(1), 0.0)
            assertEquals(0, cursor.getInt(2))
        }
    }

    @Test
    fun migrate5To6_addsDefaultRestAndFlagsBarbellLifts() {
        helper.createDatabase(DB_NAME, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO exercises (exerciseId, name, category) VALUES
                (1, 'bench press', 'CHEST'), (2, 'Leg Press', 'LEGS')
                """,
            )
            db.execSQL("INSERT INTO routines (routineId, name) VALUES (1, 'Push')")
            db.execSQL("INSERT INTO routine_exercises (routineId, exerciseId, position) VALUES (1, 1, 0)")
            db.execSQL("INSERT INTO workout_sessions (sessionId, routineName, startedAtEpochMs) VALUES (1, 'Push', 0)")
            db.execSQL("INSERT INTO workout_exercises (sessionId, exerciseId, position) VALUES (1, 1, 0)")
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, MIGRATION_5_6)

        db.query("SELECT name, isBarbell FROM exercises ORDER BY exerciseId").use { cursor ->
            val flags = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getInt(1))
            }
            assertEquals(listOf("bench press" to 1, "Leg Press" to 0), flags)
        }
        for (table in listOf("routine_exercises", "workout_exercises")) {
            db.query("SELECT restSeconds FROM $table").use { cursor ->
                cursor.moveToFirst()
                assertTrue("$table rest should default to the settings", cursor.isNull(0))
            }
        }
    }

    @Test
    fun migrate6To7_marksExistingSetsNormalAndAddsBodyWeights() {
        helper.createDatabase(DB_NAME, 6).use { db ->
            db.execSQL("INSERT INTO exercises (exerciseId, name, category, trackingType) VALUES (1, 'Squat', 'LEGS', 'WEIGHT_REPS')")
            db.execSQL("INSERT INTO routines (routineId, name) VALUES (1, 'Legs')")
            db.execSQL("INSERT INTO routine_exercises (routineExerciseId, routineId, exerciseId, position) VALUES (1, 1, 1, 0)")
            db.execSQL("INSERT INTO planned_sets (routineExerciseId, position, reps, weightKg) VALUES (1, 0, 5, 100.0)")
            db.execSQL("INSERT INTO workout_sessions (sessionId, routineName, startedAtEpochMs) VALUES (1, 'Legs', 0)")
            db.execSQL("INSERT INTO workout_exercises (workoutExerciseId, sessionId, exerciseId, position) VALUES (1, 1, 1, 0)")
            db.execSQL(
                "INSERT INTO logged_sets (workoutExerciseId, position, reps, weightKg, isCompleted) VALUES (1, 0, 5, 100.0, 1)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

        db.query("SELECT type FROM planned_sets").use { cursor ->
            cursor.moveToFirst()
            assertEquals("NORMAL", cursor.getString(0))
        }
        db.query("SELECT type, rpe FROM logged_sets").use { cursor ->
            cursor.moveToFirst()
            assertEquals("NORMAL", cursor.getString(0))
            assertTrue("RPE should be unset", cursor.isNull(1))
        }
        db.execSQL("INSERT INTO body_weights (dateEpochDay, weightKg) VALUES (19000, 80.5)")
        db.query("SELECT weightKg FROM body_weights").use { cursor ->
            cursor.moveToFirst()
            assertEquals(80.5, cursor.getDouble(0), 0.0)
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
