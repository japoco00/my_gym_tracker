package com.jacopo.mygymtracker.model

data class Exercise(
    val name: String,
    val category: Category,
    var sets: MutableList<Set> = mutableListOf(
        Set(numberOfReps = 0, weight = 0),
        Set(numberOfReps = 0, weight = 0),
        Set(numberOfReps = 0, weight = 0)
    )
)

data class Set(
    var numberOfReps: Int,
    var weight: Int
)


enum class Category {
    CAT1, CAT2, CAT3
}
