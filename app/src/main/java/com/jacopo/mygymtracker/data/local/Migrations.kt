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
