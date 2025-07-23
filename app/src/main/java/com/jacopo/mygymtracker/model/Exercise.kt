package com.jacopo.mygymtracker.model

data class Exercise(
    val id: Int? = null,
    val note: String? = null,
    val name: String,
    val category: Category,
    var sets: MutableList<Set> = mutableListOf(
        Set(reps = 0, weight = 0),
        Set(reps = 0, weight = 0),
        Set(reps = 0, weight = 0)
    )
)

data class Set(
    val id: Int? = null,
    var reps: Int,
    var weight: Int
)


enum class Category {
    CAT1, CAT2, CAT3, STRENGTH, CHEST, LEGS
}
