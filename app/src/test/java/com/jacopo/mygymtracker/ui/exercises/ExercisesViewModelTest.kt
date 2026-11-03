package com.jacopo.mygymtracker.ui.exercises

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExercisesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var exercises: ExerciseRepository
    private val viewModels = ViewModelStore()

    @Before
    fun setUp() {
        database = createTestDatabase()
        exercises = ExerciseRepository(database, database.exerciseDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        database.close()
    }

    @Test
    fun saveExercise_reopensTheEditorWithARefusedName() = runTest {
        exercises.saveExercise(newExercise("Bench Press"))
        val viewModel = loadedViewModel()
        viewModel.openNewExercise()

        viewModel.saveExercise(" bench press", Category.CHEST, TrackingType.WEIGHT_REPS)

        val dialog = viewModel.uiState.first { it.dialog != null }.dialog
        assertEquals(ExerciseDialog.Edit(exercise = null, rejectedName = "bench press"), dialog)
    }

    @Test
    fun isNameTaken_ignoresCaseAndTheEditedExercise() {
        val bench = Exercise(id = 1, name = "Bench Press", category = Category.CHEST)
        val squat = Exercise(id = 2, name = "Squat", category = Category.LEGS)
        val state = ExercisesUiState(catalog = listOf(bench, squat), dialog = ExerciseDialog.Edit(bench))

        assertFalse(state.isNameTaken("bench press"))
        assertTrue(state.isNameTaken(" squat "))
        assertFalse(state.copy(dialog = null).isNameTaken("Squat"))
    }

    private suspend fun loadedViewModel(): ExercisesViewModel {
        val factory = viewModelFactory { initializer { ExercisesViewModel(exercises) } }
        val viewModel = ViewModelProvider.create(viewModels, factory)[ExercisesViewModel::class]
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }
}
