package com.jacopo.mygymtracker.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.components.ChipDropdown
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * Edits the name, category and tracking type of [exercise], or of a new exercise when it is `null`.
 * Changing the type of a used exercise keeps its logged sets; only how they are shown changes.
 *
 * @param rejectedName name the repository refused as a duplicate; flagged until the user changes it.
 * @param defaultCategory category preselected for a new exercise.
 */
@Composable
fun ExerciseEditorDialog(
    exercise: Exercise?,
    rejectedName: String?,
    defaultCategory: Category,
    onSave: (name: String, category: Category, trackingType: TrackingType) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var category by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.category ?: defaultCategory) }
    var trackingType by rememberSaveable(exercise?.id) {
        mutableStateOf(exercise?.trackingType ?: TrackingType.WEIGHT_REPS)
    }
    val isNameTaken = rejectedName != null && name.trim().equals(rejectedName, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (exercise == null) R.string.exercise_dialog_title_new else R.string.exercise_dialog_title_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name_hint)) },
                    singleLine = true,
                    isError = isNameTaken,
                    supportingText = if (isNameTaken) {
                        { Text(stringResource(R.string.exercise_name_taken)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                ChipDropdown(
                    selected = category,
                    options = Category.entries,
                    label = { it.label() },
                    onSelect = { category = it },
                )
                ChipDropdown(
                    selected = trackingType,
                    options = TrackingType.entries,
                    label = { it.label() },
                    onSelect = { trackingType = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, category, trackingType) },
                enabled = name.isNotBlank() && !isNameTaken,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Preview
@Composable
private fun ExerciseEditorDialogPreview() {
    AppTheme {
        ExerciseEditorDialog(
            exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
            rejectedName = "Bench Press",
            defaultCategory = Category.OTHER,
            onSave = { _, _, _ -> },
            onDismiss = {},
        )
    }
}
