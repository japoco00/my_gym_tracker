package com.jacopo.mygymtracker.ui.bodyweight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.WeightField
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Logs a body weight, or edits [entry] when it isn't `null`.
 * A new entry starts on [today] with [initialWeightKg], e.g. the latest logged weight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightSheet(
    entry: BodyWeight?,
    initialWeightKg: Double,
    today: LocalDate,
    weightUnit: WeightUnit,
    onSave: (date: LocalDate, weightKg: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var date by rememberSaveable { mutableStateOf(entry?.date ?: today) }
    var weightKg by rememberSaveable { mutableDoubleStateOf(entry?.weightKg ?: initialWeightKg) }
    var isPickingDate by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
        BodyWeightEditorContent(
            isNew = entry == null,
            date = date,
            weightKg = weightKg,
            weightUnit = weightUnit,
            onPickDate = { isPickingDate = true },
            onWeightKgChange = { weightKg = it },
            onCancel = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
            onSave = { scope.launch { sheetState.hide() }.invokeOnCompletion { onSave(date, weightKg) } },
        )
        // Inside the sheet, so the dialog's window always stacks above the sheet's, also after recreation.
        if (isPickingDate) {
            BodyWeightDatePicker(
                date = date,
                today = today,
                onConfirm = {
                    date = it
                    isPickingDate = false
                },
                onDismiss = { isPickingDate = false },
            )
        }
    }
}

@Composable
private fun BodyWeightEditorContent(
    isNew: Boolean,
    date: LocalDate,
    weightKg: Double,
    weightUnit: WeightUnit,
    onPickDate: () -> Unit,
    onWeightKgChange: (Double) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(if (isNew) R.string.body_weight_log else R.string.body_weight_edit),
            style = MaterialTheme.typography.titleLarge,
        )
        AssistChip(
            onClick = onPickDate,
            label = { Text(text = formatDate(date)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        WeightField(
            weightKg = weightKg,
            unit = weightUnit,
            onWeightKgChange = onWeightKgChange,
            label = stringResource(R.string.set_weight, weightUnit.label()),
            shape = ListItemDefaults.segmentedShapes(index = 0, count = 1).shape,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = onSave, enabled = weightKg > 0, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

/** A date picker that starts on [date] and only allows days up to [today]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyWeightDatePicker(
    date: LocalDate,
    today: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // The picker works in UTC milliseconds at the start of the day.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(today) { UpToToday(today) },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class UpToToday(private val today: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

    override fun isSelectableYear(year: Int): Boolean = year <= today.year
}

@Preview(showBackground = true)
@Composable
private fun BodyWeightEditorContentPreview() {
    AppTheme {
        Surface {
            BodyWeightEditorContent(
                isNew = true,
                date = LocalDate.of(2025, 7, 14),
                weightKg = 78.4,
                weightUnit = WeightUnit.KG,
                onPickDate = {},
                onWeightKgChange = {},
                onCancel = {},
                onSave = {},
            )
        }
    }
}
