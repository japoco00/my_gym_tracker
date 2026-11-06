package com.jacopo.mygymtracker.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.domain.model.BodyWeight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BodyWeightRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var bodyWeights: BodyWeightRepository

    private val monday = LocalDate.of(2025, 7, 14)
    private val tuesday = monday.plusDays(1)

    @Before
    fun setUp() {
        database = createTestDatabase()
        bodyWeights = BodyWeightRepository(database, database.bodyWeightDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun save_keepsOneEntryPerDayOldestFirst() = runTest {
        bodyWeights.save(BodyWeight(tuesday, 79.0))
        bodyWeights.save(BodyWeight(monday, 80.0))
        bodyWeights.save(BodyWeight(monday, 79.5))

        assertEquals(listOf(BodyWeight(monday, 79.5), BodyWeight(tuesday, 79.0)), bodyWeights.observeAll().first())
    }

    @Test
    fun save_movesAnEditedEntryToItsNewDate() = runTest {
        bodyWeights.save(BodyWeight(monday, 80.0))

        bodyWeights.save(BodyWeight(tuesday, 80.0), replacing = monday)

        assertEquals(listOf(BodyWeight(tuesday, 80.0)), bodyWeights.observeAll().first())
    }

    @Test
    fun delete_removesOnlyThatDay() = runTest {
        bodyWeights.save(BodyWeight(monday, 80.0))
        bodyWeights.save(BodyWeight(tuesday, 79.0))

        bodyWeights.delete(monday)

        assertEquals(listOf(BodyWeight(tuesday, 79.0)), bodyWeights.observeAll().first())
    }
}
