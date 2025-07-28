package com.jacopo.mygymtracker.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.model.Routine
import com.jacopo.mygymtracker.ui.components.RoutineOverviewCard
import com.jacopo.mygymtracker.ui.components.SwipableElement
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RoutinesListScreen(
    routinesFlow: StateFlow<List<Routine>>,
    onNavigateToRoutineDetails: (Int) -> Unit = { /* No-op */ },
    onDeleteRoutine: (Routine) -> Unit = { /* No-op */ },
) {
    val routines by routinesFlow.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(routines, key = { it.id !! }) { routine ->
                val content: @Composable () -> Unit =
                    {
                        RoutineOverviewCard(
                            routine,
                            onClick = { onNavigateToRoutineDetails(routine.id !!) },
                        )
                    }

                SwipableElement<Routine>(
                    item = routine,
                    onDismiss = { onDeleteRoutine(it) },
                    content = content,
                    modifier = Modifier.animateItem()
                )
            }
        }
        Button(
            onClick = { onNavigateToRoutineDetails(- 1) },
            modifier = Modifier.padding(0.dp, 16.dp)
        )
        {
            Text(text = "Create a Routine")
        }
    }
}

