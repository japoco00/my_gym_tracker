package com.jacopo.mygymtracker.ui.workout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.RepsField
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.components.WeightField
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

private val SetNumberWidth = 32.dp
private val CheckboxWidth = 48.dp

/**
 * One exercise of the workout: a row per set with last time's values as a reference.
 * Swipe a set to delete it; tick it when it's done.
 */
@Composable
fun WorkoutExerciseCard(
    exercise: WorkoutExerciseUiState,
    previousSets: List<LoggedSet>,
    weightUnit: WeightUnit,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onRepsChange: (setKey: String, Int) -> Unit,
    onWeightChange: (setKey: String, Double) -> Unit,
    onCompletedChange: (setKey: String, completed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .animateContentSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Text(
                    text = exercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.workout_remove_exercise),
                    )
                }
            }
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                ColumnHeader(stringResource(R.string.workout_set), Modifier.width(SetNumberWidth))
                ColumnHeader(stringResource(R.string.workout_previous), Modifier.weight(1.2f))
                ColumnHeader(weightUnit.label(), Modifier.weight(1f))
                ColumnHeader(stringResource(R.string.set_reps), Modifier.weight(1f))
                ColumnHeader("", Modifier.width(CheckboxWidth))
            }
            exercise.sets.forEachIndexed { index, set ->
                key(set.key) {
                    SwipeToDeleteContainer(onDelete = { onDeleteSet(set.key) }) {
                        SetRow(
                            number = index + 1,
                            set = set,
                            previous = previousSets.getOrNull(index),
                            weightUnit = weightUnit,
                            onRepsChange = { reps -> onRepsChange(set.key, reps) },
                            onWeightChange = { weight -> onWeightChange(set.key, weight) },
                            onCompletedChange = { completed -> onCompletedChange(set.key, completed) },
                        )
                    }
                }
            }
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Text(text = stringResource(R.string.action_add_set))
            }
        }
    }
}

@Composable
private fun SetRow(
    number: Int,
    set: WorkoutSetUiState,
    previous: LoggedSet?,
    weightUnit: WeightUnit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (set.isCompleted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = number.toString(),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(SetNumberWidth),
        )
        Text(
            text = previous?.let {
                stringResource(R.string.workout_previous_set, weightUnit.format(it.weightKg), it.reps)
            } ?: stringResource(R.string.workout_no_previous),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f),
        )
        WeightField(
            weightKg = set.weightKg,
            unit = weightUnit,
            onWeightKgChange = onWeightChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
        )
        RepsField(
            reps = set.reps,
            onRepsChange = onRepsChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
        )
        val doneLabel = stringResource(R.string.workout_set_done, number)
        Checkbox(
            checked = set.isCompleted,
            onCheckedChange = onCompletedChange,
            modifier = Modifier
                .width(CheckboxWidth)
                .semantics { contentDescription = doneLabel },
        )
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun WorkoutExerciseCardPreview() {
    AppTheme {
        WorkoutExerciseCard(
            exercise = WorkoutExerciseUiState(
                exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                sets = listOf(
                    WorkoutSetUiState(reps = 8, weightKg = 80.0, isCompleted = true),
                    WorkoutSetUiState(reps = 8, weightKg = 80.0),
                ),
            ),
            previousSets = listOf(LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true)),
            weightUnit = WeightUnit.KG,
            onRemove = {},
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
            onCompletedChange = { _, _ -> },
        )
    }
}
