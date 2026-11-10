package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSession

/** The running workout: progress, elapsed time and the next exercise with sets left. */
@Composable
fun ActiveWorkoutCard(
    workout: WorkoutSession,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sets = workout.exercises.flatMap { it.sets }
    val completedSets = sets.count { it.isCompleted }
    val nextExercise = workout.exercises.firstOrNull { exercise -> exercise.sets.any { !it.isCompleted } }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.workout_in_progress),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = workout.routineName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.routines_active_progress,
                    pluralStringResource(R.plurals.workout_sets_progress, sets.size, completedSets, sets.size),
                    rememberElapsedTime(workout.startedAt),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearWavyProgressIndicator(
                progress = { if (sets.isEmpty()) 0f else completedSets / sets.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nextExercise?.let { stringResource(R.string.routines_next_exercise, it.exercise.name) }
                        .orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onResume, shapes = ButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.workout_resume))
                }
            }
        }
    }
}

/** Only one workout can run at a time: resume it, or throw it away and start [routine]. */
@Composable
fun StartConflictDialog(
    activeWorkout: WorkoutSession,
    routine: Routine,
    onResume: () -> Unit,
    onDiscardAndStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_in_progress)) },
        text = {
            Text(stringResource(R.string.workout_conflict_message, activeWorkout.routineName, routine.name))
        },
        confirmButton = {
            TextButton(onClick = onResume) { Text(stringResource(R.string.workout_resume)) }
        },
        dismissButton = {
            TextButton(onClick = onDiscardAndStart) {
                Text(stringResource(R.string.workout_discard_and_start))
            }
        },
    )
}
