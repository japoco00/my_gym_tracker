package com.jacopo.mygymtracker.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.data.repositories.SetRepository
import com.jacopo.mygymtracker.model.Routine
import com.jacopo.mygymtracker.model.toEntry
import com.jacopo.mygymtracker.model.toModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesListVM(
    private val routineRepository: RoutineRepository, private val setRepository: SetRepository
) : ViewModel() {

    init {
        Log.d("RoutinesListVM", "RoutinesListVM created")
    }

    var routines: StateFlow<List<Routine>> =
        routineRepository.getAllRoutinesWithExercises().map { routineList ->
            routineList.map { routineWithExercisesEntry ->
                val routineId = routineWithExercisesEntry.routineEntry.routineId
                val exercises = routineWithExercisesEntry.exercises.map { exerciseEntry ->
                    val sets = setRepository.getAllSetsByExerciseIdAndRoutineId(
                        exerciseEntry.exerciseId, routineId
                    ).map { it.toModel() }
                    exerciseEntry.toModel(sets)
                }
                Routine(
                    id = routineId,
                    name = routineWithExercisesEntry.routineEntry.name,
                    exercises = exercises,
                    note = routineWithExercisesEntry.routineEntry.note
                )
            }
        }.stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList()
            )

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            routineRepository.delete(routine.toEntry())
        }
    }
}