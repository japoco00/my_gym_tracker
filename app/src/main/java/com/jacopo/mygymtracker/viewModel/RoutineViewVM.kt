package com.jacopo.mygymtracker.viewModel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.model.Category
import com.jacopo.mygymtracker.model.Exercise
import com.jacopo.mygymtracker.model.ExerciseSet
import com.jacopo.mygymtracker.model.Routine
import com.jacopo.mygymtracker.ui.ExerciseUIState
import com.jacopo.mygymtracker.ui.RoutineUIState
import com.jacopo.mygymtracker.ui.SetUIState
import kotlinx.coroutines.launch

class RoutineViewVM(
    private val routineRepository: RoutineRepository,
) : ViewModel() {
    private val _routineState = mutableStateOf<RoutineUIState?>(null)
    var routineState: State<RoutineUIState?> = _routineState

    fun loadRoutine(routineId: Int?) {
        Log.d("RoutineViewVM", "loadRoutine called with id: $routineId")

        if (routineId == null) {
            Log.d("RoutineViewVM", "Creating new routine state")
            _routineState.value = RoutineUIState(
                name = mutableStateOf("New routine"),
                note = mutableStateOf(null),
                exercises = mutableStateListOf()
            )
            return
        }
        viewModelScope.launch {
            routineRepository.getRoutineById(routineId)
                .collect { routine ->
                    _routineState.value = routineToUIState(routine!!)
                }
        }
    }

    fun saveRoutine() {
        if (_routineState.value == null) {
            return
        }
        val routine = routineUIStateToRoutine(_routineState.value!!)
        viewModelScope.launch {
            Log.d("RoutineViewVM", "Saving routine: ${routine.name}")
            routineRepository.updateFull(routine)
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
                    sets = mutableStateOf(exercise.sets.map { set ->
                        SetUIState(
                            id = set.id,
                            reps = mutableIntStateOf(set.reps),
                            weight = mutableIntStateOf(set.weight)
                        )
                    })
                )
            }.toMutableStateList()
        )
    }

    fun removeExercise(exercise: ExerciseUIState) {
        if (_routineState.value != null) {
            _routineState.value!!.exercises.remove(exercise)
        }
    }

    fun addNewSet(exercise: ExerciseUIState) {
        val newSet = SetUIState(
            reps = mutableIntStateOf(0),
            weight = mutableIntStateOf(0)
        )
        exercise.sets.value += newSet
    }

    private fun routineUIStateToRoutine(routineUIState: RoutineUIState): Routine {
        return Routine(
            id = routineUIState.id,
            name = routineUIState.name.value,
            note = routineUIState.note.value,
            exercises = routineUIState.exercises.map { exercise ->
                Exercise(
                    id = exercise.id,
                    name = exercise.name.value,
                    note = exercise.note.value,
                    category = exercise.category.value,
                    sets = exercise.sets.value.map { set ->
                        ExerciseSet(
                            id = set.id,
                            reps = set.reps.intValue,
                            weight = set.weight.intValue
                        )
                    }.toMutableList()
                )
            }.toMutableList()
        )
    }

    fun addEmptyExercise() {
        val newExercise = ExerciseUIState(
            name = mutableStateOf(""),
            note = mutableStateOf(null),
            category = mutableStateOf(Category.CAT1),
        )
        if (_routineState.value != null) {
            _routineState.value!!.exercises.add(newExercise)
        }
    }
}


