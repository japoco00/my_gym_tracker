package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.model.Routine

@Composable
fun RoutineOverviewCard(routine: Routine, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text( // Display the routine name
                    routine.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (routine.note != null) {
                    Text(
                        // Display the routine note if it exists
                        routine.note !!,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                routine.exercises.forEach { exercise ->
                    val repsString =
                        exercise.sets.size.toString() + " x " + exercise.sets.joinToString(", ") { set -> set.reps.toString() }
                    Text( // Display the exercise name and sets
                        "${exercise.name}: $repsString",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp, 2.dp)
                    )
                }
            }
        }
    }
}
