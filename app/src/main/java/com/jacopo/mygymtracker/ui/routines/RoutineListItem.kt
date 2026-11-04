package com.jacopo.mygymtracker.ui.routines

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.ui.theme.AppTheme

/** Shared-bounds key linking a routine's row to its editor, so the row grows into the screen. */
fun routineContainerKey(routineId: Int): Any = "routine-$routineId"

/**
 * One row of the routines group.
 *
 * @param shapes from [ListItemDefaults.segmentedShapes], so only the group's outer corners are fully rounded.
 * @param isWorkoutActive a workout from this routine is in progress: the play button is filled and resumes it.
 */
@Composable
fun RoutineListItem(
    routine: Routine,
    shapes: ListItemShapes,
    isWorkoutActive: Boolean,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exerciseCount = routine.exercises.size
    val setCount = routine.exercises.sumOf { it.sets.size }
    val overline = listOfNotNull(
        pluralStringResource(R.plurals.routine_exercise_count, exerciseCount, exerciseCount),
        pluralStringResource(R.plurals.history_set_count, setCount, setCount),
        routine.note?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    val exerciseNames = if (routine.exercises.isEmpty()) {
        stringResource(R.string.routine_no_exercises)
    } else {
        routine.exercises.joinToString(" · ") { it.exercise.name }
    }
    val startLabel = stringResource(
        if (isWorkoutActive) R.string.routine_resume else R.string.routine_start,
        routine.name,
    )

    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        overlineContent = { Text(text = overline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(text = exerciseNames, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            FilledTonalIconButton(
                onClick = onStartWorkout,
                shapes = IconButtonDefaults.shapes(),
                enabled = isWorkoutActive || routine.exercises.isNotEmpty(),
                colors = if (isWorkoutActive) {
                    IconButtonDefaults.filledIconButtonColors()
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors()
                },
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = startLabel)
            }
        },
    ) {
        Text(
            text = routine.name,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineListItemPreview() {
    AppTheme {
        RoutineListItem(
            routine = Routine(
                id = 1,
                name = "Push Day",
                note = "Monday",
                exercises = listOf(
                    RoutineExercise(
                        exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                        sets = listOf(PlannedSet(reps = 8, weightKg = 80.0), PlannedSet(reps = 8, weightKg = 80.0)),
                    ),
                    RoutineExercise(
                        exercise = Exercise(id = 2, name = "Overhead Press", category = Category.SHOULDERS),
                        sets = listOf(PlannedSet(reps = 10, weightKg = 40.0)),
                    ),
                ),
            ),
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            isWorkoutActive = false,
            onClick = {},
            onStartWorkout = {},
        )
    }
}
