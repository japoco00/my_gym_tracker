package com.jacopo.mygymtracker.viewModel

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacopo.mygymtracker.data.repositories.ExerciseRepository
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.data.repositories.SetRepository
import com.jacopo.mygymtracker.model.Category
import com.jacopo.mygymtracker.model.Exercise
import com.jacopo.mygymtracker.model.Routine
import kotlinx.coroutines.launch

class RoutineViewVM(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val setRepository: SetRepository
) : ViewModel() {

    var routineState by mutableStateOf<RoutineUIState?>(null)
        private set

    fun loadRoutine(routineId: Int) {
        // Load the routine and its exercises from the repository
        viewModelScope.launch {
            val routine = routineRepository.getRoutineById(routineId)
            routineState = routineToUIState(routine)
        }
    }

    private fun routineToUIState(routine: Routine): RoutineUIState {
        return RoutineUIState(
            id = routine.id,
            name = mutableStateOf(routine.name),
            note = mutableStateOf(routine.note),
            exercises = routine.exercises.map { exercise ->
                ExerciseUIState(
                    id = exercise.id,
                    name = mutableStateOf(exercise.name),
                    note = mutableStateOf(exercise.note),
                    category = mutableStateOf(exercise.category),
                    sets = exercise.sets.map { set ->
                        SetUIState(
                            id = set.id,
                            reps = mutableIntStateOf(set.reps),
                            weight = mutableIntStateOf(set.weight)
                        )
                    }.toMutableStateList()
                )
            }.toMutableStateList()
        )
    }

}

data class RoutineUIState(
    val id: Int? = null,
    val name: MutableState<String>,
    val note: MutableState<String?> = mutableStateOf(null),
    val exercises: SnapshotStateList<ExerciseUIState>,
)

data class ExerciseUIState(
    val id: Int? = null,
    val name: MutableState<String>,
    val note: MutableState<String?> = mutableStateOf(null),
    val category: MutableState<Category>,
    val sets: SnapshotStateList<SetUIState>
)

data class SetUIState(
    val id: Int? = null,
    val reps: MutableIntState = mutableIntStateOf(0),
    val weight: MutableIntState = mutableIntStateOf(0)
)
