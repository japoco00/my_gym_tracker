package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesListScreen(
    uiState: RoutinesListUiState,
    onCreateRoutine: () -> Unit,
    onRoutineClick: (Int) -> Unit,
    onDeleteRoutine: (Routine) -> Unit,
    onUndoDelete: (routineId: Int) -> Unit,
    onConfirmDelete: (routineId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UndoDeleteSnackbar(
        pendingDelete = uiState.pendingDelete,
        snackbarHostState = snackbarHostState,
        onUndo = onUndoDelete,
        onConfirm = onConfirmDelete,
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.routines_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRoutine,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.routines_create)) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            return@Scaffold
        } else if (uiState.routines.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    text = stringResource(R.string.routines_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(
                    items = uiState.routines,
                    key = { routine -> checkNotNull(routine.id) },
                ) { routine ->
                    SwipeToDeleteContainer(
                        onDelete = { onDeleteRoutine(routine) },
                        modifier = Modifier.animateItem(),
                    ) {
                        RoutineOverviewCard(
                            routine = routine,
                            onClick = { routine.id?.let(onRoutineClick) },
                        )
                    }
                }
            }
        }
    }
}

/** Shows the undo snackbar for [pendingDelete]; a newer delete replaces the current snackbar. */
@Composable
private fun UndoDeleteSnackbar(
    pendingDelete: Routine?,
    snackbarHostState: SnackbarHostState,
    onUndo: (routineId: Int) -> Unit,
    onConfirm: (routineId: Int) -> Unit,
) {
    val message = stringResource(R.string.routine_deleted, pendingDelete?.name.orEmpty())
    val undoLabel = stringResource(R.string.action_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(pendingDelete?.id) {
        val routineId = pendingDelete?.id ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo(routineId) else currentOnConfirm(routineId)
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutinesListScreenPreview() {
    AppTheme {
        RoutinesListScreen(
            uiState = RoutinesListUiState(
                routines = listOf(
                    Routine(id = 1, name = "Upper Body", note = "Monday"),
                    Routine(id = 2, name = "Lower Body", note = "Thursday"),
                ),
                isLoading = false,
            ),
            onCreateRoutine = {},
            onRoutineClick = {},
            onDeleteRoutine = {},
            onUndoDelete = {},
            onConfirmDelete = {},
        )
    }
}
