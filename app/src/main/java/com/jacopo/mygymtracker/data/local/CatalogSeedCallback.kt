package com.jacopo.mygymtracker.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.TrackingType

/**
 * Fills the exercise catalog with common exercises.
 * Runs on first creation and after a destructive migration (which does not call `onCreate`).
 */
internal object CatalogSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) = seed(db)

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) = seed(db)

    private fun seed(db: SupportSQLiteDatabase) {
        SEED_EXERCISES.forEach { exercise ->
            db.execSQL(
                "INSERT OR IGNORE INTO exercises (name, category, trackingType) VALUES (?, ?, ?)",
                arrayOf(exercise.name, exercise.category.name, exercise.trackingType.name),
            )
        }
    }

    private class SeedExercise(
        val name: String,
        val category: Category,
        val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    )

    private val SEED_EXERCISES: List<SeedExercise> = listOf(
        SeedExercise("Bench Press", Category.CHEST),
        SeedExercise("Incline Dumbbell Press", Category.CHEST),
        SeedExercise("Chest Fly", Category.CHEST),
        SeedExercise("Push-Up", Category.CHEST, TrackingType.REPS),
        SeedExercise("Deadlift", Category.BACK),
        SeedExercise("Pull-Up", Category.BACK, TrackingType.REPS),
        SeedExercise("Lat Pulldown", Category.BACK),
        SeedExercise("Barbell Row", Category.BACK),
        SeedExercise("Seated Cable Row", Category.BACK),
        SeedExercise("Overhead Press", Category.SHOULDERS),
        SeedExercise("Dumbbell Shoulder Press", Category.SHOULDERS),
        SeedExercise("Lateral Raise", Category.SHOULDERS),
        SeedExercise("Face Pull", Category.SHOULDERS),
        SeedExercise("Barbell Curl", Category.BICEPS),
        SeedExercise("Dumbbell Curl", Category.BICEPS),
        SeedExercise("Hammer Curl", Category.BICEPS),
        SeedExercise("Triceps Pushdown", Category.TRICEPS),
        SeedExercise("Skull Crusher", Category.TRICEPS),
        SeedExercise("Overhead Triceps Extension", Category.TRICEPS),
        SeedExercise("Dip", Category.TRICEPS, TrackingType.REPS),
        SeedExercise("Squat", Category.LEGS),
        SeedExercise("Leg Press", Category.LEGS),
        SeedExercise("Romanian Deadlift", Category.LEGS),
        SeedExercise("Leg Extension", Category.LEGS),
        SeedExercise("Leg Curl", Category.LEGS),
        SeedExercise("Walking Lunge", Category.LEGS),
        SeedExercise("Calf Raise", Category.LEGS),
        SeedExercise("Hip Thrust", Category.GLUTES),
        SeedExercise("Bulgarian Split Squat", Category.GLUTES),
        SeedExercise("Plank", Category.CORE, TrackingType.DURATION),
        SeedExercise("Hanging Leg Raise", Category.CORE, TrackingType.REPS),
        SeedExercise("Cable Crunch", Category.CORE),
        SeedExercise("Kettlebell Swing", Category.FULL_BODY),
        SeedExercise("Burpee", Category.FULL_BODY, TrackingType.REPS),
        SeedExercise("Rowing Machine", Category.CARDIO, TrackingType.DURATION),
        SeedExercise("Running", Category.CARDIO, TrackingType.DURATION),
    )
}