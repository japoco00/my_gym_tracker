package com.jacopo.mygymtracker.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
import com.jacopo.mygymtracker.ui.components.EmptyStateText
import com.jacopo.mygymtracker.ui.components.ExerciseEditorSheet
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.SectionLabel
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    uiState: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onCategoryFilterChange: (Category?) -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onEditExercise: (Exercise) -> Unit,
    onCreateExercise: () -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    onSaveExercise: (name: String, category: Category, trackingType: TrackingType, isBarbell: Boolean) -> Unit,
    onConfirmDelete: (Exercise) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExerciseDialogHost(
        uiState = uiState,
        onSaveExercise = onSaveExercise,
        onConfirmDelete = onConfirmDelete,
        onDismiss = onDismissDialog,
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            AppHeader(
                title = stringResource(R.string.exercises_title),
                scrollBehavior = scrollBehavior,
                pills = { if (!uiState.isLoading) CatalogPill(uiState.catalog.size) },
            ) {
                SearchField(query = uiState.query, onQueryChange = onQueryChange)
                CategoryFilterRow(
                    categories = uiState.categories,
                    selected = uiState.categoryFilter,
                    onSelect = onCategoryFilterChange,
                )
            }
        },
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                onClick = onCreateExercise,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.exercises_create)) },
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            // The bottom inset keeps the last row clear of the medium FAB.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.visibleGroups.isEmpty()) {
                item(key = "empty") {
                    EmptyStateText(
                        text = stringResource(R.string.exercises_empty),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            uiState.visibleGroups.entries.forEachIndexed { groupIndex, (category, exercises) ->
                item(key = "category-${category.name}") {
                    SectionLabel(
                        text = category.label(),
                        modifier = Modifier
                            .animateItem()
                            .padding(top = if (groupIndex == 0) 0.dp else 16.dp),
                    )
                }
                itemsIndexed(items = exercises, key = { _, exercise -> checkNotNull(exercise.id) }) { index, exercise ->
                    ExerciseListItem(
                        exercise = exercise,
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = exercises.size),
                        onClick = { onExerciseClick(exercise) },
                        onEdit = { onEditExercise(exercise) },
                        onDelete = { onDeleteExercise(exercise) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogPill(count: Int) {
    Pill(icon = painterResource(R.drawable.ic_list), text = pluralStringResource(R.plurals.exercises_catalog_count, count, count))
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    TextField(
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
        shape = CircleShape,
        // A pill without the filled field's underline, like a search bar.
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

/** One chip per category; tapping the selected chip clears the filter. */
@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
    ) {
        items(items = categories, key = { it.name }) { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category.takeUnless { isSelected }) },
                label = { Text(category.label()) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        supportingContent = { Text(exercise.trackingType.label()) },
        trailingContent = { ExerciseMenu(onEdit = onEdit, onDelete = onDelete) },
    ) {
        Text(
            text = exercise.name,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExerciseMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    DestructiveOverflowMenu(action = stringResource(R.string.action_delete), onAction = onDelete) { close ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
            onClick = {
                close()
                onEdit()
            },
        )
    }
}

@Composable
private fun ExerciseDialogHost(
    uiState: ExercisesUiState,
    onSaveExercise: (name: String, category: Category, trackingType: TrackingType, isBarbell: Boolean) -> Unit,
    onConfirmDelete: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    when (val dialog = uiState.dialog) {
        null -> Unit
        // Keyed so an editor re-opened with a refused name is a new, visible sheet.
        is ExerciseDialog.Edit -> key(dialog) {
            ExerciseEditorSheet(
                exercise = dialog.exercise,
                onSave = onSaveExercise,
                onDismiss = onDismiss,
                initialName = dialog.rejectedName ?: dialog.exercise?.name.orEmpty(),
                defaultCategory = uiState.categoryFilter ?: Category.OTHER,
                isNameTaken = uiState::isNameTaken,
            )
        }
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
                    Exercise(id = 2, name = "Incline Dumbbell Press", category = Category.CHEST),
                    Exercise(id = 3, name = "Squat", category = Category.LEGS),
                    Exercise(id = 4, name = "Plank", category = Category.CORE, trackingType = TrackingType.DURATION),
                ),
                categoryFilter = null,
                isLoading = false,
            ),
            onQueryChange = {},
            onCategoryFilterChange = {},
            onExerciseClick = {},
            onEditExercise = {},
            onCreateExercise = {},
            onDeleteExercise = {},
            onSaveExercise = { _, _, _, _ -> },
            onConfirmDelete = {},
            onDismissDialog = {},
        )
    }
}
