package com.jacopo.mygymtracker.data.repository

import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Access to the reusable exercise catalog. */
class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
) {
    /** Emits the whole catalog, sorted by name. Catalog exercises carry no sets. */
    fun observeCatalog(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { exercises -> exercises.map { it.toDomain() } }
}
