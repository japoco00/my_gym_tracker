package com.jacopo.mygymtracker.ui

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.jacopo.mygymtracker.model.Category
import com.jacopo.mygymtracker.model.Exercise
import com.jacopo.mygymtracker.model.ExerciseSet
import com.jacopo.mygymtracker.model.Routine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface UiState

data class RoutineUIState(
    val id: Int? = null,
    val name: MutableState<String>,
    val note: MutableState<String?> = mutableStateOf(null),
    val exercises: SnapshotStateList<ExerciseUIState>,
) : UiState

fun Routine.toUIState(): RoutineUIState {
    return RoutineUIState(
        id = id,
        name = mutableStateOf(name),
        note = mutableStateOf(note),
        exercises = exercises.map { it.toUiState() }.toMutableStateList()
    )

}

data class ExerciseUIState @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Int? = null,
    val draftId: String = Uuid.random().toString(), // Used to identify unsaved exercises
    val name: MutableState<String>,
    val note: MutableState<String?> = mutableStateOf(null),
    val category: MutableState<Category>,
    val sets: MutableState<List<SetUIState>> = mutableStateOf(emptyList()),
) : UiState

fun Exercise.toUiState(): ExerciseUIState {
    return ExerciseUIState(
        id = id,
        name = mutableStateOf(name),
        note = mutableStateOf(note),
        category = mutableStateOf(category),
        sets = mutableStateOf(sets.map { it.toUiState() })
    )
}

data class SetUIState @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Int? = null,
    val reps: MutableIntState = mutableIntStateOf(0),
    val weight: MutableIntState = mutableIntStateOf(0),
    val uuid: String = Uuid.random().toString()
) : UiState

fun ExerciseSet.toUiState(): SetUIState {
    return SetUIState(
        id = id,
        reps = mutableIntStateOf(reps),
        weight = mutableIntStateOf(weight)
    )
}

data class AddExercisesUIState(
    val allExercises: SnapshotStateList<Exercise> = SnapshotStateList(),
    val selectedExercises: SnapshotStateList<Exercise> = SnapshotStateList(),
    val searchQuery: MutableState<String> = mutableStateOf(""),
    val isSearching: MutableState<Boolean> = mutableStateOf(false),
    val queryResult: SnapshotStateList<Exercise> = SnapshotStateList(),
) : UiState
