package com.jacopo.mygymtracker.model

data class Exercise(
    val id: Int? = null,
    val note: String? = null,
    val name: String,
    val category: Category,
    var sets: MutableList<ExerciseSet> = mutableListOf(
        ExerciseSet(reps = 0, weight = 0),
        ExerciseSet(reps = 0, weight = 0),
        ExerciseSet(reps = 0, weight = 0)
    )
)

data class ExerciseSet(
    val id: Int? = null,
    var reps: Int,
    var weight: Int
)

enum class Category {
    CAT1, CAT2, CAT3, STRENGTH, CHEST, LEGS
}
