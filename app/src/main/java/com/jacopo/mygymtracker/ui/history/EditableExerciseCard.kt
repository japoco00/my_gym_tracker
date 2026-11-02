package com.jacopo.mygymtracker.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.ColumnHeader
import com.jacopo.mygymtracker.ui.components.SetFieldHeaders
import com.jacopo.mygymtracker.ui.components.SetFields
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme

private val SetNumberWidth = 32.dp

/** One exercise of a finished workout in edit mode. Swipe a set to delete it. */
@Composable
fun EditableExerciseCard(
    exercise: EditableExerciseUiState,
    weightUnit: WeightUnit,
    onAddSet: () -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onRepsChange: (setKey: String, Int) -> Unit,
    onWeightChange: (setKey: String, Double) -> Unit,
    onDurationChange: (setKey: String, seconds: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackingType = exercise.exercise.trackingType
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .animateContentSize(),
        ) {
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                ColumnHeader(stringResource(R.string.workout_set), Modifier.width(SetNumberWidth))
                SetFieldHeaders(trackingType, weightUnit)
            }
            exercise.sets.forEachIndexed { index, set ->
                key(set.key) {
                    SwipeToDeleteContainer(onDelete = { onDeleteSet(set.key) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(SetNumberWidth),
                            )
                            SetFields(
                                trackingType = trackingType,
                                weightUnit = weightUnit,
                                reps = set.reps,
                                weightKg = set.weightKg,
                                durationSeconds = set.durationSeconds,
                                onRepsChange = { reps -> onRepsChange(set.key, reps) },
                                onWeightKgChange = { weight -> onWeightChange(set.key, weight) },
                                onDurationChange = { seconds -> onDurationChange(set.key, seconds) },
                            )
                        }
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

@Preview(showBackground = true)
@Composable
private fun EditableExerciseCardPreview() {
    AppTheme {
        EditableExerciseCard(
            exercise = EditableExerciseUiState(
                exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                sets = listOf(
                    EditableSetUiState(reps = 8, weightKg = 80.0),
                    EditableSetUiState(reps = 7, weightKg = 80.0),
                ),
            ),
            weightUnit = WeightUnit.KG,
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
            onDurationChange = { _, _ -> },
        )
    }
}
