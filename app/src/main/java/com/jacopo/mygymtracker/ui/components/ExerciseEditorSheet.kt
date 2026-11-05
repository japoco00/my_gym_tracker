package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Edits the name, category, tracking type and barbell flag of [exercise], or of a new exercise when it is `null`.
 * Changing the type of a used exercise keeps its logged sets; only how they are shown changes.
 * Only weight & reps exercises can be barbell ones; [onSave] gets `false` for the others.
 *
 * @param initialName the name to start from, e.g. one the repository just refused.
 * @param isNameTaken flags a name as a duplicate, which disables Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorSheet(
    exercise: Exercise?,
    onSave: (name: String, category: Category, trackingType: TrackingType, isBarbell: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = exercise?.name.orEmpty(),
    defaultCategory: Category = Category.OTHER,
    isNameTaken: (String) -> Boolean = { false },
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var category by rememberSaveable { mutableStateOf(exercise?.category ?: defaultCategory) }
    var trackingType by rememberSaveable { mutableStateOf(exercise?.trackingType ?: TrackingType.WEIGHT_REPS) }
    var isBarbell by rememberSaveable { mutableStateOf(exercise?.isBarbell ?: false) }

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
        ExerciseEditorContent(
            isNew = exercise == null,
            name = name,
            category = category,
            trackingType = trackingType,
            isBarbell = isBarbell,
            isNameTaken = isNameTaken(name),
            onNameChange = { name = it },
            onCategoryChange = { category = it },
            onTrackingTypeChange = { trackingType = it },
            onBarbellChange = { isBarbell = it },
            onCancel = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
            onSave = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onSave(name, category, trackingType, isBarbell && trackingType == TrackingType.WEIGHT_REPS)
                }
            },
        )
    }
}

@Composable
private fun ExerciseEditorContent(
    isNew: Boolean,
    name: String,
    category: Category,
    trackingType: TrackingType,
    isBarbell: Boolean,
    isNameTaken: Boolean,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onTrackingTypeChange: (TrackingType) -> Unit,
    onBarbellChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(if (isNew) R.string.exercise_dialog_title_new else R.string.exercise_dialog_title_edit),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.exercise_name_hint)) },
            singleLine = true,
            isError = isNameTaken,
            supportingText = if (isNameTaken) {
                { Text(stringResource(R.string.exercise_name_taken)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        ChipGroup(
            title = stringResource(R.string.exercise_category),
            options = Category.entries,
            selected = category,
            label = { it.label() },
            onSelect = onCategoryChange,
        )
        ChipGroup(
            title = stringResource(R.string.exercise_tracking_type),
            options = TrackingType.entries,
            selected = trackingType,
            label = { it.label() },
            onSelect = onTrackingTypeChange,
        )
        if (trackingType == TrackingType.WEIGHT_REPS) {
            BarbellSwitch(checked = isBarbell, onCheckedChange = onBarbellChange)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = onSave, enabled = name.isNotBlank() && !isNameTaken, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

/** A titled set of chips where exactly one is selected. */
@Composable
private fun <T> ChipGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

/** The whole row toggles, so the target is bigger than the switch and reads as one control. */
@Composable
private fun BarbellSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.exercise_barbell), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.exercise_barbell_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseEditorContentPreview() {
    AppTheme {
        Surface {
            ExerciseEditorContent(
                isNew = true,
                name = "Bench Press",
                category = Category.CHEST,
                trackingType = TrackingType.WEIGHT_REPS,
                isBarbell = true,
                isNameTaken = true,
                onNameChange = {},
                onCategoryChange = {},
                onTrackingTypeChange = {},
                onBarbellChange = {},
                onCancel = {},
                onSave = {},
            )
        }
    }
}
