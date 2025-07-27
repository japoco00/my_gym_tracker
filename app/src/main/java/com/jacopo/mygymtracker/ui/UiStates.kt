package com.jacopo.mygymtracker.ui

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.jacopo.mygymtracker.model.Category
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface UiState

data class RoutineUIState(
    val id: Int? = null,
    val name: MutableState<String>,
    val note: MutableState<String?> = mutableStateOf(null),
    val exercises: SnapshotStateList<ExerciseUIState>,
) : UiState

data class ExerciseUIState @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Int? = null,
    val draftId: String = Uuid.random().toString(), // Used to identify unsaved exercises
    val name: MutableState<String>,
    val note: MutableState<String?> = mutableStateOf(null),
    val category: MutableState<Category>,
    val sets: MutableState<List<SetUIState>> = mutableStateOf(emptyList()),
) : UiState

data class SetUIState @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Int? = null,
    val reps: MutableIntState = mutableIntStateOf(0),
    val weight: MutableIntState = mutableIntStateOf(0),
    val uuid: String = Uuid.random().toString()
) : UiState