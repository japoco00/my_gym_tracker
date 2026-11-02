package com.jacopo.mygymtracker.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    uiState: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onCategoryFilterChange: (Category?) -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onCreateExercise: () -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    onSaveExercise: (name: String, category: Category) -> Unit,
    onConfirmDelete: (Exercise) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExerciseDialogHost(
        dialog = uiState.dialog,
        defaultCategory = uiState.categoryFilter ?: Category.OTHER,
        onSaveExercise = onSaveExercise,
        onConfirmDelete = onConfirmDelete,
        onDismiss = onDismissDialog,
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.exercises_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateExercise,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.exercises_create)) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(query = uiState.query, onQueryChange = onQueryChange)
            CategoryFilterRow(selected = uiState.categoryFilter, onSelect = onCategoryFilterChange)

            if (uiState.visibleExercises.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.exercises_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = uiState.visibleExercises,
                        key = { exercise -> checkNotNull(exercise.id) },
                    ) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = { Text(exercise.category.label()) },
                            trailingContent = {
                                IconButton(onClick = { onDeleteExercise(exercise) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                    )
                                }
                            },
                            modifier = Modifier
                                .animateItem()
                                .clickable { onExerciseClick(exercise) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.exercises_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** One chip per category; tapping the selected chip clears the filter. */
@Composable
private fun CategoryFilterRow(
    selected: Category?,
    onSelect: (Category?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(Category.entries) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category.takeUnless { it == selected }) },
                label = { Text(category.label()) },
            )
        }
    }
}

@Composable
private fun ExerciseDialogHost(
    dialog: ExerciseDialog?,
    defaultCategory: Category,
    onSaveExercise: (name: String, category: Category) -> Unit,
    onConfirmDelete: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        is ExerciseDialog.Edit -> ExerciseEditorDialog(
            exercise = dialog.exercise,
            rejectedName = dialog.rejectedName,
            defaultCategory = defaultCategory,
            onSave = onSaveExercise,
            onDismiss = onDismiss,
        )
        is ExerciseDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.exercise_delete_title, dialog.exercise.name)) },
            text = { Text(stringResource(R.string.exercise_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(dialog.exercise) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )
        is ExerciseDialog.InUse -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.exercise_in_use_title, dialog.exercise.name)) },
            text = { Text(stringResource(R.string.exercise_in_use_message)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExercisesScreenPreview() {
    AppTheme {
        ExercisesScreen(
            uiState = ExercisesUiState(
                catalog = listOf(
                    Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                    Exercise(id = 2, name = "Squat", category = Category.LEGS),
                    Exercise(id = 3, name = "Deadlift", category = Category.BACK),
                ),
                isLoading = false,
            ),
            onQueryChange = {},
            onCategoryFilterChange = {},
            onExerciseClick = {},
            onCreateExercise = {},
            onDeleteExercise = {},
            onSaveExercise = { _, _ -> },
            onConfirmDelete = {},
            onDismissDialog = {},
        )
    }
}
