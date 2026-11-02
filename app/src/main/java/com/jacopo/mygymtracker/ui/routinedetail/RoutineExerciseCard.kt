package com.jacopo.mygymtracker.ui.routinedetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.CategoryDropdown
import com.jacopo.mygymtracker.ui.components.RepsField
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.components.WeightField
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * Card for one exercise of the routine. Catalog exercises show their name read-only
 * (renaming happens in the catalog); new exercises can be named and categorised here.
 */
@Composable
fun RoutineExerciseCard(
    exercise: RoutineExerciseUiState,
    weightUnit: WeightUnit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onRepsChange: (setKey: String, Int) -> Unit,
    onWeightChange: (setKey: String, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .animateContentSize(),
        ) {
            if (exercise.exerciseId != null) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
                Text(
                    text = exercise.category.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            } else {
                TextField(
                    value = exercise.name,
                    onValueChange = onNameChange,
                    placeholder = { Text(text = stringResource(R.string.exercise_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryDropdown(
                    selected = exercise.category,
                    onSelect = onCategoryChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                ColumnHeader(text = stringResource(R.string.set_reps), modifier = Modifier.weight(1f))
                ColumnHeader(
                    text = stringResource(R.string.set_weight, weightUnit.label()),
                    modifier = Modifier.weight(1f),
                )
            }
            exercise.sets.forEach { set ->
                key(set.key) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    SwipeToDeleteContainer(
                        onDelete = { onDeleteSet(set.key) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            RepsField(
                                reps = set.reps,
                                onRepsChange = { reps -> onRepsChange(set.key, reps) },
                                showStepper = true,
                                modifier = Modifier.weight(1f),
                            )
                            WeightField(
                                weightKg = set.weightKg,
                                unit = weightUnit,
                                onWeightKgChange = { weight -> onWeightChange(set.key, weight) },
                                showStepper = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onAddSet) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_set),
                )
            }
        }
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
private fun RoutineExerciseCardPreview() {
    AppTheme {
        RoutineExerciseCard(
            exercise = RoutineExerciseUiState(
                exerciseId = 1,
                name = "Bench Press",
                category = Category.CHEST,
                sets = listOf(PlannedSetUiState(reps = 8, weightKg = 82.5)),
            ),
            weightUnit = WeightUnit.KG,
            onNameChange = {},
            onCategoryChange = {},
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewRoutineExerciseCardPreview() {
    AppTheme {
        RoutineExerciseCard(
            exercise = RoutineExerciseUiState(sets = listOf(PlannedSetUiState())),
            weightUnit = WeightUnit.LB,
            onNameChange = {},
            onCategoryChange = {},
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
        )
    }
}
