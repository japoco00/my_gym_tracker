package com.jacopo.mygymtracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.DurationField
import com.jacopo.mygymtracker.ui.components.RepsField
import com.jacopo.mygymtracker.ui.components.WeightField
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Edits one set with big stepper fields. Values are saved as they change; the buttons
 * slide the sheet away first and then act, so it never vanishes mid-animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEditorSheet(
    exerciseName: String,
    number: Int,
    set: WorkoutSetUiState,
    previous: LoggedSet?,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
        SetEditorContent(
            exerciseName = exerciseName,
            number = number,
            set = set,
            previous = previous,
            trackingType = trackingType,
            weightUnit = weightUnit,
            onRepsChange = onRepsChange,
            onWeightChange = onWeightChange,
            onDurationChange = onDurationChange,
            onToggleCompleted = {
                hideThen {
                    onCompletedChange(!set.isCompleted)
                    onDismiss()
                }
            },
            onDelete = { hideThen(onDelete) },
        )
    }
}

@Composable
private fun SetEditorContent(
    exerciseName: String,
    number: Int,
    set: WorkoutSetUiState,
    previous: LoggedSet?,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.workout_set_editor_title, exerciseName, number),
            style = MaterialTheme.typography.titleLarge,
        )
        if (previous != null) {
            Text(
                text = stringResource(
                    R.string.workout_set_editor_last_time,
                    formatSet(trackingType, weightUnit, previous.reps, previous.weightKg, previous.durationSeconds),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        when (trackingType) {
            TrackingType.DURATION -> LabeledField(stringResource(R.string.set_time)) {
                DurationField(
                    seconds = set.durationSeconds,
                    onSecondsChange = onDurationChange,
                    showStepper = true,
                )
            }
            TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
                val weightLabel = if (trackingType == TrackingType.REPS) R.string.set_added_weight else R.string.set_weight
                LabeledField(stringResource(weightLabel, weightUnit.label())) {
                    WeightField(
                        weightKg = set.weightKg,
                        unit = weightUnit,
                        onWeightKgChange = onWeightChange,
                        showStepper = true,
                    )
                }
                LabeledField(stringResource(R.string.set_reps)) {
                    RepsField(reps = set.reps, onRepsChange = onRepsChange, showStepper = true)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.workout_delete_set))
            }
            if (set.isCompleted) {
                TextButton(onClick = onToggleCompleted) { Text(text = stringResource(R.string.workout_uncomplete_set)) }
            } else {
                Button(onClick = onToggleCompleted, shapes = ButtonDefaults.shapes()) {
                    Text(text = stringResource(R.string.workout_complete_set))
                }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        field()
    }
}

@Preview(showBackground = true)
@Composable
private fun SetEditorContentPreview() {
    AppTheme {
        Surface {
            SetEditorContent(
                exerciseName = "Bench Press",
                number = 2,
                set = WorkoutSetUiState(reps = 8, weightKg = 80.0),
                previous = LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true),
                trackingType = TrackingType.WEIGHT_REPS,
                weightUnit = WeightUnit.KG,
                onRepsChange = {},
                onWeightChange = {},
                onDurationChange = {},
                onToggleCompleted = {},
                onDelete = {},
            )
        }
    }
}
