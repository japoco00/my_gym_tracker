package com.jacopo.mygymtracker.ui.routines

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.normalSetNumbers
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.routine_exercise_subtitle,
                        exercise.category.label(),
                        exercise.trackingType.label(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
            TextButton(onClick = onAddSet) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.action_add_set))
            }
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
