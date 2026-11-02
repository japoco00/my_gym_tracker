package com.jacopo.mygymtracker.domain.model

/** What a set of an exercise records. Stored by name: never rename an entry. */
enum class TrackingType {
    /** Reps with an external load, e.g. Bench Press. */
    WEIGHT_REPS,

    /** Bodyweight reps; the weight is optional added load, e.g. a weighted Pull-Up. */
    REPS,

    /** A held or timed effort, cardio included, e.g. Plank or Running. */
    DURATION,
}
