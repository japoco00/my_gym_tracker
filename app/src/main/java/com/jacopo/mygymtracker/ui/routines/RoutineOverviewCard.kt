package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.ExerciseSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.ui.theme.AppTheme

@Composable
fun RoutineOverviewCard(
    routine: Routine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!routine.note.isNullOrBlank()) {
                Text(
                    text = routine.note,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            routine.exercises.forEach { exercise ->
                val summary = if (exercise.sets.isEmpty()) {
                    "No sets"
                } else {
                    "${exercise.sets.size} x ${exercise.sets.joinToString(", ") { it.reps.toString() }}"
                }
                Text(
                    text = "${exercise.name}: $summary",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineOverviewCardPreview() {
    AppTheme {
        RoutineOverviewCard(
            routine = Routine(
                id = 1,
                name = "Push Day",
                note = "Focus on controlled tempo",
                exercises = listOf(
                    Exercise(
                        id = 1,
                        name = "Bench Press",
                        category = Category.CHEST,
                        sets = listOf(
                            ExerciseSet(reps = 8, weight = 80),
                            ExerciseSet(reps = 8, weight = 80),
                        ),
                    ),
                ),
            ),
            onClick = {},
        )
    }
}
