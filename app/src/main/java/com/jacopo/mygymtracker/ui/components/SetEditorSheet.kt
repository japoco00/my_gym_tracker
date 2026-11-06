package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.formatWeight
import com.jacopo.mygymtracker.domain.plates.platesPerSide
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.format.setName
import com.jacopo.mygymtracker.ui.theme.AppTheme

/** Which set is open in a [SetEditorSheet], by the UI keys of its exercise and of itself. */
data class EditingSet(val exerciseKey: String, val setKey: String)

/**
 * Edits one set with big stepper cards. Values are saved as they change; the buttons
 * slide the sheet away first and then act, so it never vanishes mid-animation.
 *
 * @param number the set's number among the normal sets, null for the other [type]s.
 * @param previous the same set last time; a chip copies its values in.
 * @param isCompleted whether a live workout's set is ticked; the main button toggles it.
 * Null for sets that can't be ticked (a plan, a finished workout): the main button just closes.
 * @param plates the plates owned, in [weightUnit], to show what to load per side; null for non-barbell exercises.
 * @param onRpeChange edits the set's [rpe]; null hides the field, for sets that aren't logged (a plan).
 */
@Composable
fun SetEditorSheet(
    exerciseName: String,
    number: Int?,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
    type: SetType,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
    onTypeChange: (SetType) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    previous: LoggedSet? = null,
    isCompleted: Boolean? = null,
    onCompletedChange: (Boolean) -> Unit = {},
    plates: List<Double>? = null,
    rpe: Double? = null,
    onRpeChange: ((Double?) -> Unit)? = null,
) {
    AppBottomSheet(onDismissRequest = onDismiss) { hideThen ->
        SetEditorContent(
            exerciseName = exerciseName,
            number = number,
            trackingType = trackingType,
            weightUnit = weightUnit,
            reps = reps,
            weightKg = weightKg,
            durationSeconds = durationSeconds,
            type = type,
            previous = previous,
            isCompleted = isCompleted,
            plates = plates,
            rpe = rpe,
            onRepsChange = onRepsChange,
            onWeightChange = onWeightChange,
            onDurationChange = onDurationChange,
            onTypeChange = onTypeChange,
            onRpeChange = onRpeChange,
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
    number: Int?,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
    type: SetType,
    previous: LoggedSet?,
    isCompleted: Boolean?,
    plates: List<Double>?,
    rpe: Double?,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
    onTypeChange: (SetType) -> Unit,
    onRpeChange: ((Double?) -> Unit)?,
    onPrimary: () -> Unit,
    onDelete: () -> Unit,
) {
    // Up to three stepper cards plus the type buttons: scroll rather than clip on short screens.
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = setName(number, type),
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
        val fieldCount = (if (trackingType == TrackingType.DURATION) 1 else 2) + (if (onRpeChange != null) 1 else 0)
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            when (trackingType) {
                TrackingType.DURATION -> DurationField(
                    seconds = durationSeconds,
                    onSecondsChange = onDurationChange,
                    label = stringResource(R.string.set_time),
                    shape = ListItemDefaults.segmentedShapes(index = 0, count = fieldCount).shape,
                )
                TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
                    val weightLabel = if (trackingType == TrackingType.REPS) R.string.set_added_weight else R.string.set_weight
                    WeightField(
                        weightKg = weightKg,
                        unit = weightUnit,
                        onWeightKgChange = onWeightChange,
                        label = stringResource(weightLabel, weightUnit.label()),
                        shape = ListItemDefaults.segmentedShapes(index = 0, count = fieldCount).shape,
                    )
                    RepsField(
                        reps = reps,
                        onRepsChange = onRepsChange,
                        label = stringResource(R.string.set_reps),
                        shape = ListItemDefaults.segmentedShapes(index = 1, count = fieldCount).shape,
                    )
                }
            }
            if (onRpeChange != null) {
                RpeField(
                    rpe = rpe,
                    onRpeChange = onRpeChange,
                    label = stringResource(R.string.set_rpe),
                    shape = ListItemDefaults.segmentedShapes(index = fieldCount - 1, count = fieldCount).shape,
                )
            }
        }
        if (plates != null && trackingType == TrackingType.WEIGHT_REPS && weightKg > 0) {
            PlatesPerSide(weightKg = weightKg, unit = weightUnit, plates = plates)
        }
        SetTypeToggles(selected = type, onSelect = onTypeChange, modifier = Modifier.padding(top = 16.dp))
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

/** The set types as a connected button group, with exactly one selected. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTypeToggles(selected: SetType, onSelect: (SetType) -> Unit, modifier: Modifier = Modifier) {
    val types = SetType.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier.fillMaxWidth(),
    ) {
        types.forEachIndexed { index, type ->
            ToggleButton(
                checked = type == selected,
                onCheckedChange = { onSelect(type) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    types.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                // Four labels share the width: the default padding would cut "Warm-up" short.
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(text = type.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
                type = SetType.NORMAL,
                previous = LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true),
                isCompleted = false,
                plates = WeightUnit.KG.defaultPlates,
                rpe = 8.5,
                onRepsChange = {},
                onWeightChange = {},
                onDurationChange = {},
                onTypeChange = {},
                onRpeChange = {},
                onPrimary = {},
                onDelete = {},
            )
        }
    }
}
