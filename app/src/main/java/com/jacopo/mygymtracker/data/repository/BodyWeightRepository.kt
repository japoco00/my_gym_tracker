package com.jacopo.mygymtracker.data.repository

import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.local.dao.BodyWeightDao
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.domain.model.BodyWeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** The body weight log: one entry per day. */
class BodyWeightRepository(
    private val database: AppDatabase,
    private val bodyWeightDao: BodyWeightDao,
) {
    /** Emits every entry, oldest first. */
    fun observeAll(): Flow<List<BodyWeight>> =
        bodyWeightDao.observeAll().map { entries -> entries.map { it.toDomain() } }

    /**
     * Saves [entry], replacing the one already on its date if any.
     * Pass the edited entry's old date as [replacing] so a date change moves it instead of copying it.
     */
    suspend fun save(entry: BodyWeight, replacing: LocalDate? = null) = database.withTransaction {
        if (replacing != null && replacing != entry.date) bodyWeightDao.delete(replacing.toEpochDay())
        bodyWeightDao.upsert(entry.toEntity())
    }

    suspend fun delete(date: LocalDate) = bodyWeightDao.delete(date.toEpochDay())
}
