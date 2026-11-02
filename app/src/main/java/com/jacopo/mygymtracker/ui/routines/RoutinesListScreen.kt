package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme

@Composable
fun RoutinesListScreen(
    uiState: RoutinesListUiState,
    onCreateRoutine: () -> Unit,
    onRoutineClick: (Int) -> Unit,
    onDeleteRoutine: (Routine) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
    ) {
        if (uiState.routines.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "No routines yet. Create your first one!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
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

        Button(
            onClick = onCreateRoutine,
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            Text(text = "Create a Routine")
        }
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
            ),
            onCreateRoutine = {},
            onRoutineClick = {},
            onDeleteRoutine = {},
        )
    }
}
