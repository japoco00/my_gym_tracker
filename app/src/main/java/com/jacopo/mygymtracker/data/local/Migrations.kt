package com.jacopo.mygymtracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds tracking types to exercises and a duration to sets.
 * Seeded bodyweight and timed exercises get their type; everything else stays weight x reps.
 * The names are frozen here on purpose: a migration must not change when the seed list does.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN trackingType TEXT NOT NULL DEFAULT 'WEIGHT_REPS'")
        db.execSQL("ALTER TABLE planned_sets ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE logged_sets ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE exercises SET trackingType = 'REPS'
            WHERE name IN ('Push-Up', 'Pull-Up', 'Dip', 'Burpee', 'Hanging Leg Raise')
            """,
        )
        db.execSQL(
            """
            UPDATE exercises SET trackingType = 'DURATION'
            WHERE name IN ('Plank', 'Rowing Machine', 'Running')
            """,
        )
    }
}

/**
 * Adds a rest time to routine and workout exercises (`NULL` = the settings default)
 * and a barbell flag to exercises. Seeded barbell lifts get the flag; names frozen as above.
 */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN restSeconds INTEGER")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN restSeconds INTEGER")
        db.execSQL("ALTER TABLE exercises ADD COLUMN isBarbell INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE exercises SET isBarbell = 1
            WHERE name IN (
                'Bench Press', 'Squat', 'Deadlift', 'Romanian Deadlift', 'Barbell Row',
                'Overhead Press', 'Barbell Curl', 'Hip Thrust'
            )
            """,
        )
    }
}

/**
 * Adds a set type to planned and logged sets (existing sets become normal ones),
 * an optional RPE to logged sets, and the body weight log.
 */
internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE planned_sets ADD COLUMN type TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("ALTER TABLE logged_sets ADD COLUMN type TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("ALTER TABLE logged_sets ADD COLUMN rpe REAL")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS body_weights " +
                "(dateEpochDay INTEGER NOT NULL, weightKg REAL NOT NULL, PRIMARY KEY(dateEpochDay))",
        )
    }
}
