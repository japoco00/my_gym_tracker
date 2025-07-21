package com.jacopo.mygymtracker.model

data class Routine(
    val name: String,
    val exercises: List<Exercise> = emptyList(),
)
