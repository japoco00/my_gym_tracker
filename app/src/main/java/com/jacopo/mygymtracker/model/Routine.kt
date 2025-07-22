package com.jacopo.mygymtracker.model

data class Routine(
    var id: Int? = null,
    val name: String,
    val exercises: List<Exercise> = emptyList(),
    var note: String? = null
)
