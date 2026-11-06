package com.jacopo.mygymtracker.domain.model

import java.time.LocalDate

/** The body weight on [date], in kilograms. One entry per day. */
data class BodyWeight(
    val date: LocalDate,
    val weightKg: Double,
)
