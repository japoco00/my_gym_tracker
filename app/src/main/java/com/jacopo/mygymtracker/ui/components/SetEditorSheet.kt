package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.formatWeight
import com.jacopo.mygymtracker.domain.plates.platesPerSide
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** Which set is open in a [SetEditorSheet], by the UI keys of its exercise and of itself. */
data class EditingSet(val exerciseKey: String, val setKey: String)

/**
 * Edits one set with big stepper cards. Values are saved as they change; the buttons
 * slide the sheet away first and then act, so it never vanishes mid-animation.
 *
 * @param previous the same set last time; a chip copies its values in.
 * @param isCompleted whether a live workout's set is ticked; the main button toggles it.
 * Null for sets that can't be ticked (a plan, a finished workout): the main button just closes.
 * @param plates the plates owned, in [weightUnit], to show what to load per side; null for non-barbell exercises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEditorSheet(
    exerciseName: String,
    number: Int,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    previous: LoggedSet? = null,
    isCompleted: Boolean? = null,
    onCompletedChange: (Boolean) -> Unit = {},
    plates: List<Double>? = null,
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
            trackingType = trackingType,
            weightUnit = weightUnit,
            reps = reps,
            weightKg = weightKg,
            durationSeconds = durationSeconds,
            previous = previous,
            isCompleted = isCompleted,
            plates = plates,
            onRepsChange = onRepsChange,
            onWeightChange = onWeightChange,
            onDurationChange = onDurationChange,
            onPrimary = {
                hideThen {
                    if (isCompleted != null) onCompletedChange(!isCompleted)
                    onDismiss()
                }
            },
            onDelete = { hideThen(onDelete) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetEditorContent(
    exerciseName: String,
    number: Int,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
    previous: LoggedSet?,
    isCompleted: Boolean?,
    plates: List<Double>?,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
    onPrimary: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.set_editor_title, number),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (previous != null) {
            UseLastTimeChip(
                last = formatSet(trackingType, weightUnit, previous.reps, previous.weightKg, previous.durationSeconds),
                current = formatSet(trackingType, weightUnit, reps, weightKg, durationSeconds),
                onClick = {
                    if (trackingType == TrackingType.DURATION) {
                        onDurationChange(previous.durationSeconds)
                    } else {
                        onWeightChange(previous.weightKg)
                        onRepsChange(previous.reps)
                    }
                },
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            when (trackingType) {
                TrackingType.DURATION -> DurationField(
                    seconds = durationSeconds,
                    onSecondsChange = onDurationChange,
                    label = stringResource(R.string.set_time),
                    shape = ListItemDefaults.segmentedShapes(index = 0, count = 1).shape,
                )
                TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
                    val weightLabel = if (trackingType == TrackingType.REPS) R.string.set_added_weight else R.string.set_weight
                    WeightField(
                        weightKg = weightKg,
                        unit = weightUnit,
                        onWeightKgChange = onWeightChange,
                        label = stringResource(weightLabel, weightUnit.label()),
                        shape = ListItemDefaults.segmentedShapes(index = 0, count = 2).shape,
                    )
                    RepsField(
                        reps = reps,
                        onRepsChange = onRepsChange,
                        label = stringResource(R.string.set_reps),
                        shape = ListItemDefaults.segmentedShapes(index = 1, count = 2).shape,
                    )
                }
            }
        }
        if (plates != null && trackingType == TrackingType.WEIGHT_REPS && weightKg > 0) {
            PlatesPerSide(weightKg = weightKg, unit = weightUnit, plates = plates)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            FilledTonalIconButton(
                onClick = onDelete,
                shapes = IconButtonDefaults.shapes(IconButtonDefaults.mediumRoundShape, IconButtonDefaults.mediumPressedShape),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.set_delete))
            }
            PrimaryButton(isCompleted = isCompleted, onClick = onPrimary, modifier = Modifier.weight(1f))
        }
    }
}

/** "Per side: 25 · 10 · 2.5", plus what the owned [plates] can't make up, if anything. */
@Composable
private fun PlatesPerSide(weightKg: Double, unit: WeightUnit, plates: List<Double>) {
    val breakdown = platesPerSide(unit.fromKg(weightKg), plates)
    val loaded = breakdown.plates.joinToString(" · ") { formatWeight(it) }
        .ifEmpty { stringResource(R.string.plates_none) }
    Text(
        text = if (breakdown.remainder == 0.0) {
            stringResource(R.string.plates_per_side, loaded)
        } else {
            stringResource(R.string.plates_per_side_short, loaded, formatWeight(breakdown.remainder), unit.label())
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
}

/** Copies last time's values in; disabled, with a tick, once they already match. */
@Composable
private fun UseLastTimeChip(last: String, current: String, onClick: () -> Unit) {
    val isSame = last == current
    AssistChip(
        onClick = onClick,
        enabled = !isSame,
        label = {
            Text(text = stringResource(if (isSame) R.string.set_editor_same_as_last else R.string.set_editor_use_last, last))
        },
        leadingIcon = {
            val iconModifier = Modifier.size(AssistChipDefaults.IconSize)
            if (isSame) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = iconModifier)
            } else {
                Icon(painter = painterResource(R.drawable.ic_history), contentDescription = null, modifier = iconModifier)
            }
        },
        // Disabled means "already applied", not "unavailable": keep it readable.
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** "Complete set" / "Mark as not done" in a live workout, "Done" where sets can't be ticked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrimaryButton(isCompleted: Boolean?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val height = ButtonDefaults.MediumContainerHeight
    val buttonModifier = modifier.height(height)
    if (isCompleted == true) {
        FilledTonalButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier = buttonModifier,
        ) {
            Text(text = stringResource(R.string.workout_uncomplete_set), style = ButtonDefaults.textStyleFor(height))
        }
    } else {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier = buttonModifier,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(height)))
            Text(
                text = stringResource(if (isCompleted == null) R.string.action_done else R.string.workout_complete_set),
                style = ButtonDefaults.textStyleFor(height),
            )
        }
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
                trackingType = TrackingType.WEIGHT_REPS,
                weightUnit = WeightUnit.KG,
                reps = 8,
                weightKg = 80.0,
                durationSeconds = 0,
                previous = LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true),
                isCompleted = false,
                plates = WeightUnit.KG.defaultPlates,
                onRepsChange = {},
                onWeightChange = {},
                onDurationChange = {},
                onPrimary = {},
                onDelete = {},
            )
        }
    }
}
