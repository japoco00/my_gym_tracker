package com.jacopo.mygymtracker.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jacopo.mygymtracker.model.Category
import com.jacopo.mygymtracker.model.Exercise
import com.jacopo.mygymtracker.ui.AddExercisesUIState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AddExercisesModal(
    uiState: AddExercisesUIState = AddExercisesUIState(),
    onSave: (List<Exercise>) -> Unit = {},
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    if (showBottomSheet) {
        ModalBottomSheet(
            sheetState = bottomSheetState, onDismissRequest = { showBottomSheet = false }) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (! uiState.isSearching.value) {
                            Text(
                                "Add Exercises",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            )
                            IconButton(
                                onClick = { uiState.isSearching.value = true },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Clear Selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            TextField(
                                value = uiState.searchQuery.value,
                                onValueChange = {
                                    uiState.searchQuery.value = it
                                    uiState.queryResult.clear()
                                    uiState.queryResult.addAll(uiState.allExercises.filter { exercise ->
                                        exercise.name.contains(it, ignoreCase = true)
                                    })
                                },
                                placeholder = { Text("Search Exercises") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .weight(1f),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            uiState.searchQuery.value = ""
                                            uiState.isSearching.value = false
                                        }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                        )
                                    }
                                })
                        }
                    }
                    LazyColumn {
                        items(
                            if (uiState.searchQuery.value.isBlank()) uiState.allExercises
                            else uiState.queryResult
                        ) { exercise ->
                            var selected by remember {
                                mutableStateOf(
                                    uiState.selectedExercises.contains(
                                        exercise
                                    )
                                )
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RectangleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (selected) {
                                                uiState.selectedExercises.remove(exercise)
                                            } else {
                                                uiState.selectedExercises.add(exercise)
                                            }
                                            selected = ! selected
                                        }) {
                                        Icon(
                                            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                            contentDescription = if (selected) "Remove Exercise" else "Add Exercise",
                                        )
                                    }
                                    Text(
                                        exercise.name,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .weight(1f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    SuggestionChip(
                                        onClick = {/*Nothing to do here*/ }, label = {
                                            Text(
                                                exercise.category.name,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }, modifier = Modifier
                                            .wrapContentSize()
                                            .padding(4.dp, 0.dp)
                                    )

                                }
                            }
                        }
                    }
                }
                LargeFloatingActionButton (
                    onClick = {
                        scope.launch {
                            onSave(uiState.selectedExercises.toList())
                            bottomSheetState.hide()
                        }.invokeOnCompletion {
                            if (! bottomSheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }, modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .zIndex(0.2f)
                ) { Icon(Icons.Default.Done, contentDescription = "Done") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExercisesScreenPreview() {
    val exercises = listOf(
        Exercise(
            name = "Bench Press",
            id = 1,
            note = "",
            category = Category.STRENGTH,
            sets = SnapshotStateList(),
        ), Exercise(
            name = "Squat",
            id = 2,
            note = "",
            category = Category.CAT1,
            sets = SnapshotStateList(),
        ), Exercise(
            name = "Deadlift",
            id = 3,
            note = "",
            category = Category.CAT2,
            sets = SnapshotStateList(),
        ), Exercise(
            name = "Overhead Press",
            id = 4,
            note = "",
            category = Category.CHEST,
            sets = SnapshotStateList(),
        )
    )
    val uiState = AddExercisesUIState(
        allExercises = SnapshotStateList<Exercise>().apply { addAll(exercises) },
        selectedExercises = SnapshotStateList()
    )
    AddExercisesModal(uiState)
}