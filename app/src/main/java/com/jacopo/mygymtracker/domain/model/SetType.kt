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

/** Numbers the normal sets 1, 2, 3… in order; the other types get null, as their type marks them instead. */
fun List<SetType>.normalSetNumbers(): List<Int?> {
    var number = 0
    return map { type -> if (type == SetType.NORMAL) ++number else null }
}
