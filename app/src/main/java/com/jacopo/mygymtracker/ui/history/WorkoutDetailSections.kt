package com.jacopo.mygymtracker.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.normalSetNumbers
import com.jacopo.mygymtracker.ui.components.PrTag
import com.jacopo.mygymtracker.ui.components.SetMarker
import com.jacopo.mygymtracker.ui.components.SetRow
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.setMarker
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * One exercise of a finished workout, read-only: its numbered sets, the personal-record one
 * starred. [onClick] on the name opens the exercise's progress.
 */
@Composable
fun LoggedExerciseSection(
    exercise: WorkoutExercise,
    prSetIndex: Int?,
    weightUnit: WeightUnit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 20.dp, bottom = 4.dp)) {
        ExerciseTitle(name = exercise.exercise.name, setCount = exercise.sets.size, onClick = onClick)
        val numbers = exercise.sets.map { it.type }.normalSetNumbers()
        exercise.sets.forEachIndexed { index, set ->
            SetRow(
                value = formatSet(exercise.exercise.trackingType, weightUnit, set.reps, set.weightKg, set.durationSeconds),
                leading = { SetMarker(label = setMarker(numbers[index], set.type)) },
            ) {
                if (index == prSetIndex) PrTag()
            }
        }
    }
}

/** One exercise of a finished workout being edited: tap a set to edit it, swipe it to delete it. */
@Composable
fun EditableExerciseSection(
    exercise: EditableExerciseUiState,
    weightUnit: WeightUnit,
    onAddSet: () -> Unit,
    onOpenSet: (setKey: String) -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 20.dp, bottom = 4.dp)
            .animateContentSize(),
    ) {
        ExerciseTitle(name = exercise.exercise.name, setCount = exercise.sets.size, onClick = null)
        val numbers = exercise.sets.map { it.type }.normalSetNumbers()
        exercise.sets.forEachIndexed { index, set ->
            key(set.key) {
                SwipeToDeleteContainer(onDelete = { onDeleteSet(set.key) }) {
                    SetRow(
                        value = formatSet(exercise.exercise.trackingType, weightUnit, set.reps, set.weightKg, set.durationSeconds),
                        leading = { SetMarker(label = setMarker(numbers[index], set.type)) },
                        onClick = { onOpenSet(set.key) },
                    )
                }
            }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.padding(start = 52.dp)) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.action_add_set))
        }
    }
}

@Composable
private fun ExerciseTitle(name: String, setCount: Int, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(start = 20.dp, end = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = pluralStringResource(R.plurals.history_set_count, setCount, setCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoggedExerciseSectionPreview() {
    AppTheme {
        LoggedExerciseSection(
            exercise = WorkoutExercise(
                exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                sets = listOf(
                    LoggedSet(reps = 8, weightKg = 80.0, isCompleted = true),
                    LoggedSet(reps = 6, weightKg = 85.0, isCompleted = true),
                ),
            ),
            prSetIndex = 1,
            weightUnit = WeightUnit.KG,
            onClick = {},
        )
    }
}
