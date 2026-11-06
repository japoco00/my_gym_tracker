package com.jacopo.mygymtracker.domain.model

/** The role of a set. Stored by name: never rename an entry. */
enum class SetType {
    NORMAL,

    /** A lighter ramp-up set: left out of volume, records and progress charts. */
    WARM_UP,

    /** A set right after another one with less weight. */
    DROP,

    /** A set taken to failure. */
    FAILURE,
}
