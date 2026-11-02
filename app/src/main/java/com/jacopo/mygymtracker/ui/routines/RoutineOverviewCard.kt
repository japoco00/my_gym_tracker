package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * @param isWorkoutActive a workout from this routine is in progress: the button resumes it.
 */
@Composable
fun RoutineOverviewCard(
    routine: Routine,
    isWorkoutActive: Boolean,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit,
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
            routine.exercises.forEach { (exercise, sets) ->
                val summary = if (sets.isEmpty()) {
                    stringResource(R.string.routine_exercise_no_sets, exercise.name)
                } else {
                    stringResource(
                        R.string.routine_exercise_sets,
                        exercise.name,
                        sets.size,
                        sets.joinToString(", ") { it.reps.toString() },
                    )
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            FilledTonalButton(
                onClick = onStartWorkout,
                enabled = isWorkoutActive || routine.exercises.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isWorkoutActive) R.string.workout_resume else R.string.workout_start,
                    ),
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
                    RoutineExercise(
                        exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                        sets = listOf(
                            PlannedSet(reps = 8, weightKg = 80.0),
                            PlannedSet(reps = 8, weightKg = 80.0),
                        ),
                    ),
                ),
            ),
            isWorkoutActive = false,
            onClick = {},
            onStartWorkout = {},
        )
    }
}
