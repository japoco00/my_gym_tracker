package com.jacopo.mygymtracker.ui.routines

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.ChipDropdown
import com.jacopo.mygymtracker.ui.components.ColumnHeader
import com.jacopo.mygymtracker.ui.components.DurationField
import com.jacopo.mygymtracker.ui.components.RepsField
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.components.WeightField
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * Card for one exercise of the routine. Catalog exercises show their name read-only
 * (renaming happens in the catalog); new exercises can be named, categorised and typed here.
 * The set columns follow the exercise's [TrackingType].
 */
@Composable
fun RoutineExerciseCard(
    exercise: RoutineExerciseUiState,
    weightUnit: WeightUnit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onTrackingTypeChange: (TrackingType) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onRepsChange: (setKey: String, Int) -> Unit,
    onWeightChange: (setKey: String, Double) -> Unit,
    onDurationChange: (setKey: String, seconds: Int) -> Unit,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    ChipDropdown(
                        selected = exercise.category,
                        options = Category.entries,
                        label = { it.label() },
                        onSelect = onCategoryChange,
                    )
                    ChipDropdown(
                        selected = exercise.trackingType,
                        options = TrackingType.entries,
                        label = { it.label() },
                        onSelect = onTrackingTypeChange,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                when (exercise.trackingType) {
                    TrackingType.DURATION ->
                        ColumnHeader(text = stringResource(R.string.set_time), modifier = Modifier.weight(1f))
                    TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
                        val weightHeader = if (exercise.trackingType == TrackingType.REPS) {
                            R.string.set_added_weight
                        } else {
                            R.string.set_weight
                        }
                        ColumnHeader(text = stringResource(R.string.set_reps), modifier = Modifier.weight(1f))
                        ColumnHeader(
                            text = stringResource(weightHeader, weightUnit.label()),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
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
                            when (exercise.trackingType) {
                                TrackingType.DURATION -> DurationField(
                                    seconds = set.durationSeconds,
                                    onSecondsChange = { seconds -> onDurationChange(set.key, seconds) },
                                    showStepper = true,
                                    modifier = Modifier.weight(1f),
                                )
                                TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
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
            onTrackingTypeChange = {},
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
            onDurationChange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewRoutineExerciseCardPreview() {
    AppTheme {
        RoutineExerciseCard(
            exercise = RoutineExerciseUiState(
                trackingType = TrackingType.DURATION,
                sets = listOf(PlannedSetUiState(durationSeconds = 90)),
            ),
            weightUnit = WeightUnit.LB,
            onNameChange = {},
            onCategoryChange = {},
            onTrackingTypeChange = {},
            onAddSet = {},
            onDeleteSet = {},
            onRepsChange = { _, _ -> },
            onWeightChange = { _, _ -> },
            onDurationChange = { _, _ -> },
        )
    }
}
