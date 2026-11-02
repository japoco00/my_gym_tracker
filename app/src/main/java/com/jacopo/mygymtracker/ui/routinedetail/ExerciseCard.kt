package com.jacopo.mygymtracker.ui.routinedetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme

@Composable
fun ExerciseCard(
    exercise: ExerciseUiState,
    onNameChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (setUuid: String) -> Unit,
    onRepsChange: (setUuid: String, Int) -> Unit,
    onWeightChange: (setUuid: String, Int) -> Unit,
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
            TextField(
                value = exercise.name,
                onValueChange = onNameChange,
                placeholder = { Text(text = "Exercise name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SuggestionChip(
                onClick = {},
                label = { Text(text = exercise.category.name) },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Kg",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
            exercise.sets.forEach { set ->
                key(set.uuid) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    SwipeToDeleteContainer(
                        onDelete = { onDeleteSet(set.uuid) },
                    ) {
                        SetRow(
                            set = set,
                            onRepsChange = { reps -> onRepsChange(set.uuid, reps) },
                            onWeightChange = { weight -> onWeightChange(set.uuid, weight) },
                        )
                    }
                }
            }
            IconButton(onClick = onAddSet) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add set",
                )
            }
        }
    }
}

@Composable
private fun SetRow(
    set: SetUiState,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        StepperNumberField(
            value = set.reps,
            step = 1,
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1f),
        )
        StepperNumberField(
            value = set.weight,
            step = 5,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StepperNumberField(
    value: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        Button(
            shape = MaterialTheme.shapes.small,
            onClick = { onValueChange((value - step).coerceAtLeast(0)) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .wrapContentSize()
                .padding(2.dp),
        ) {
            Text(text = "-$step")
        }
        TextField(
            value = value.toString(),
            onValueChange = { raw ->
                val parsed = raw.toIntOrNull() ?: 0
                onValueChange(parsed.coerceAtLeast(0))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Button(
            shape = MaterialTheme.shapes.small,
            onClick = { onValueChange(value + step) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .wrapContentSize()
                .padding(2.dp),
        ) {
            Text(text = "+$step")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseCardPreview() {
    AppTheme {
        ExerciseCard(
            exercise = ExerciseUiState(
                id = 1,
                name = "Bench Press",
                category = Category.CHEST,
                sets = listOf(SetUiState(reps = 8, weight = 80)),
            ),
            onNameChange = {},
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
        )
    }
}
