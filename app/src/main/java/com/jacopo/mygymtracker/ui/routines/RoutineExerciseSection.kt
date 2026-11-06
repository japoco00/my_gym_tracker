package com.jacopo.mygymtracker.ui.routines

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.normalSetNumbers
import com.jacopo.mygymtracker.ui.components.AddSetButton
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
import com.jacopo.mygymtracker.ui.components.ExerciseTitle
import com.jacopo.mygymtracker.ui.components.RestChip
import com.jacopo.mygymtracker.ui.components.SetMarker
import com.jacopo.mygymtracker.ui.components.SetRow
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.format.setMarker
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * One exercise of the routine, laid out like the workout: a numbered line per planned set.
 * Tap a set to edit it, swipe it to delete it.
 */
@Composable
fun RoutineExerciseSection(
    exercise: RoutineExerciseUiState,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onEditRest: () -> Unit,
    onOpenSet: (setKey: String) -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 20.dp, bottom = 4.dp)
            .animateContentSize(),
    ) {
        ExerciseTitle(
            name = exercise.name,
            subtitle = stringResource(
                R.string.routine_exercise_subtitle,
                exercise.category.label(),
                exercise.trackingType.label(),
            ),
        ) {
            DestructiveOverflowMenu(action = stringResource(R.string.action_remove_exercise), onAction = onRemove)
        }
        val numbers = exercise.sets.map { it.type }.normalSetNumbers()
        exercise.sets.forEachIndexed { index, set ->
            key(set.key) {
                SwipeToDeleteContainer(onDelete = { onDeleteSet(set.key) }) {
                    SetRow(
                        value = formatSet(exercise.trackingType, weightUnit, set.reps, set.weightKg, set.durationSeconds),
                        leading = { SetMarker(label = setMarker(numbers[index], set.type)) },
                        onClick = { onOpenSet(set.key) },
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, end = 20.dp),
        ) {
            AddSetButton(onClick = onAddSet)
            Spacer(modifier = Modifier.weight(1f))
            RestChip(restSeconds = exercise.restSeconds, defaultSeconds = defaultRestSeconds, onClick = onEditRest)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineExerciseSectionPreview() {
    AppTheme {
        RoutineExerciseSection(
            exercise = RoutineExerciseUiState(
                exerciseId = 1,
                name = "Bench Press",
                category = Category.CHEST,
                sets = List(3) { PlannedSetUiState(reps = 8, weightKg = 80.0) },
            ),
            weightUnit = WeightUnit.KG,
            defaultRestSeconds = 90,
            onRemove = {},
            onAddSet = {},
            onEditRest = {},
            onOpenSet = {},
            onDeleteSet = {},
        )
    }
}
