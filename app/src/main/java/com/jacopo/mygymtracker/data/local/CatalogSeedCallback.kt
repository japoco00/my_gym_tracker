package com.jacopo.mygymtracker.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jacopo.mygymtracker.domain.model.Category

/**
 * Fills the exercise catalog with common exercises.
 * Runs on first creation and after a destructive migration (which does not call `onCreate`).
 */
internal object CatalogSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) = seed(db)

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) = seed(db)

    private fun seed(db: SupportSQLiteDatabase) {
        SEED_EXERCISES.forEach { (name, category) ->
            db.execSQL(
                "INSERT OR IGNORE INTO exercises (name, category) VALUES (?, ?)",
                arrayOf(name, category.name),
            )
        }
    }

    private val SEED_EXERCISES: List<Pair<String, Category>> = listOf(
        "Bench Press" to Category.CHEST,
        "Incline Dumbbell Press" to Category.CHEST,
        "Chest Fly" to Category.CHEST,
        "Push-Up" to Category.CHEST,
        "Deadlift" to Category.BACK,
        "Pull-Up" to Category.BACK,
        "Lat Pulldown" to Category.BACK,
        "Barbell Row" to Category.BACK,
        "Seated Cable Row" to Category.BACK,
        "Overhead Press" to Category.SHOULDERS,
        "Dumbbell Shoulder Press" to Category.SHOULDERS,
        "Lateral Raise" to Category.SHOULDERS,
        "Face Pull" to Category.SHOULDERS,
        "Barbell Curl" to Category.BICEPS,
        "Dumbbell Curl" to Category.BICEPS,
        "Hammer Curl" to Category.BICEPS,
        "Triceps Pushdown" to Category.TRICEPS,
        "Skull Crusher" to Category.TRICEPS,
        "Overhead Triceps Extension" to Category.TRICEPS,
        "Dip" to Category.TRICEPS,
        "Squat" to Category.LEGS,
        "Leg Press" to Category.LEGS,
        "Romanian Deadlift" to Category.LEGS,
        "Leg Extension" to Category.LEGS,
        "Leg Curl" to Category.LEGS,
        "Walking Lunge" to Category.LEGS,
        "Calf Raise" to Category.LEGS,
        "Hip Thrust" to Category.GLUTES,
        "Bulgarian Split Squat" to Category.GLUTES,
        "Plank" to Category.CORE,
        "Hanging Leg Raise" to Category.CORE,
        "Cable Crunch" to Category.CORE,
        "Kettlebell Swing" to Category.FULL_BODY,
        "Burpee" to Category.FULL_BODY,
        "Rowing Machine" to Category.CARDIO,
        "Running" to Category.CARDIO,
    )
}