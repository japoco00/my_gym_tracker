package com.jacopo.mygymtracker.ui.routinedetail

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExercisesModal(
    catalogExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (List<Exercise>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val selectedExercises = remember { mutableStateListOf<Exercise>() }

    val displayedExercises = remember(catalogExercises, searchQuery) {
        catalogExercises.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        AddExercisesSheetContent(
            exercises = displayedExercises,
            selectedExercises = selectedExercises,
            searchQuery = searchQuery,
            isSearching = isSearching,
            onStartSearch = { isSearching = true },
            onClearSearch = {
                searchQuery = ""
                isSearching = false
            },
            onSearchQueryChange = { searchQuery = it },
            onToggleExercise = { exercise ->
                if (exercise in selectedExercises) {
                    selectedExercises.remove(exercise)
                } else {
                    selectedExercises.add(exercise)
                }
            },
            onConfirm = {
                val chosen = selectedExercises.toList()
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onConfirm(chosen)
                }
            },
        )
    }
}

@Composable
private fun AddExercisesSheetContent(
    exercises: List<Exercise>,
    selectedExercises: List<Exercise>,
    searchQuery: String,
    isSearching: Boolean,
    onStartSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleExercise: (Exercise) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                if (!isSearching) {
                    Text(
                        text = "Add Exercises",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onStartSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search exercises",
                        )
                    }
                } else {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(text = "Search exercises") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (exercises.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = "No exercises in catalog yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = exercises,
                        key = { it.id ?: it.name },
                    ) { exercise ->
                        val isSelected = exercise in selectedExercises
                        Card(
                            shape = RectangleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                IconButton(onClick = { onToggleExercise(exercise) }) {
                                    Icon(
                                        imageVector = if (isSelected) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.AddCircle
                                        },
                                        contentDescription = if (isSelected) {
                                            "Deselect exercise"
                                        } else {
                                            "Select exercise"
                                        },
                                    )
                                }
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = exercise.category.name,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    modifier = Modifier.wrapContentSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        LargeFloatingActionButton(
            onClick = onConfirm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(imageVector = Icons.Default.Done, contentDescription = "Confirm selection")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddExercisesSheetContentPreview() {
    AppTheme {
        AddExercisesSheetContent(
            exercises = listOf(
                Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                Exercise(id = 2, name = "Squat", category = Category.LEGS),
                Exercise(id = 3, name = "Deadlift", category = Category.BACK),
            ),
            selectedExercises = listOf(
                Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
            ),
            searchQuery = "",
            isSearching = false,
            onStartSearch = {},
            onClearSearch = {},
            onSearchQueryChange = {},
            onToggleExercise = {},
            onConfirm = {},
        )
    }
}
