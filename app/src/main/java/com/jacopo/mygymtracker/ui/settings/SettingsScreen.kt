package com.jacopo.mygymtracker.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.UserSettings.Companion.REST_STEP_SECONDS
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.formatWeight
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.SectionLabel
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings?,
    @StringRes backupMessage: Int?,
    onWeightUnitChange: (WeightUnit) -> Unit,
    onRestSecondsChange: (deltaSeconds: Int) -> Unit,
    onPlateToggle: (Double) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onBackupMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    BackupMessageSnackbar(message = backupMessage, snackbarHostState = snackbarHostState, onShown = onBackupMessageShown)
    if (confirmImport) {
        ConfirmImportDialog(
            onConfirm = {
                confirmImport = false
                onImportBackup()
            },
            onDismiss = { confirmImport = false },
        )
    }

    Scaffold(
        topBar = {
            // No pills, so the expanded header only needs room for the title.
            AppHeader(
                title = stringResource(R.string.settings_title),
                scrollBehavior = scrollBehavior,
                expandedHeight = 136.dp,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (settings == null) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionLabel(text = stringResource(R.string.settings_units))
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                supportingContent = { Text(text = stringResource(R.string.settings_weight_unit_description)) },
                trailingContent = { WeightUnitToggles(selected = settings.weightUnit, onSelect = onWeightUnitChange) },
            ) {
                Text(text = stringResource(R.string.settings_weight_unit))
            }

            SectionLabel(text = stringResource(R.string.settings_workout), modifier = Modifier.padding(top = 16.dp))
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                supportingContent = { Text(text = stringResource(R.string.settings_rest_time_description)) },
            ) {
                Text(text = stringResource(R.string.settings_rest_time))
            }
            SegmentedListItem(shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2)) {
                RestStepper(restSeconds = settings.restSeconds, onChange = onRestSecondsChange)
            }

            SectionLabel(text = stringResource(R.string.settings_plates), modifier = Modifier.padding(top = 16.dp))
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                supportingContent = { Text(text = stringResource(R.string.settings_plates_description)) },
            ) {
                Text(text = stringResource(R.string.settings_plates_owned, settings.weightUnit.label()))
            }
            SegmentedListItem(shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2)) {
                PlateChips(
                    options = settings.weightUnit.plateOptions,
                    owned = settings.currentPlates,
                    onToggle = onPlateToggle,
                )
            }

            SectionLabel(text = stringResource(R.string.settings_backup), modifier = Modifier.padding(top = 16.dp))
            SegmentedListItem(
                onClick = onExportBackup,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                supportingContent = { Text(text = stringResource(R.string.settings_backup_export_description)) },
            ) {
                Text(text = stringResource(R.string.settings_backup_export))
            }
            SegmentedListItem(
                onClick = { confirmImport = true },
                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                supportingContent = { Text(text = stringResource(R.string.settings_backup_import_description)) },
            ) {
                Text(text = stringResource(R.string.settings_backup_import))
            }
        }
    }
}

/** Import wipes everything, so it asks before the file picker opens. */
@Composable
private fun ConfirmImportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.backup_import_confirm_title)) },
        text = { Text(text = stringResource(R.string.backup_import_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.backup_import_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Shows [message], then calls [onShown] so the same message can show again next time.
 * A different message arriving meanwhile replaces the current snackbar.
 */
@Composable
private fun BackupMessageSnackbar(
    @StringRes message: Int?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val text = message?.let { stringResource(it) }
    val currentOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        text ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        currentOnShown()
    }
}

/** One chip per plate size, selected when owned. */
@Composable
private fun PlateChips(options: List<Double>, owned: List<Double>, onToggle: (Double) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { plate ->
            FilterChip(
                selected = plate in owned,
                onClick = { onToggle(plate) },
                label = { Text(text = formatWeight(plate)) },
            )
        }
    }
}

/** kg | lb as a connected button group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeightUnitToggles(selected: WeightUnit, onSelect: (WeightUnit) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier,
    ) {
        WeightUnit.entries.forEachIndexed { index, unit ->
            ToggleButton(
                checked = unit == selected,
                onCheckedChange = { onSelect(unit) },
                shapes = if (index == 0) {
                    ButtonGroupDefaults.connectedLeadingButtonShapes()
                } else {
                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                },
                modifier = Modifier.semantics { role = Role.RadioButton },
            ) {
                Text(text = unit.label())
            }
        }
    }
}

/** The rest duration between −/+ [REST_STEP_SECONDS] buttons; never below one step, as the repository enforces. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestStepper(restSeconds: Int, onChange: (deltaSeconds: Int) -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        StepButton(
            text = stringResource(R.string.rest_decrease, REST_STEP_SECONDS),
            onClick = { onChange(-REST_STEP_SECONDS) },
            enabled = restSeconds > REST_STEP_SECONDS,
        )
        Text(
            text = formatDuration(restSeconds.toLong()),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepButton(
            text = stringResource(R.string.rest_increase, REST_STEP_SECONDS),
            onClick = { onChange(REST_STEP_SECONDS) },
        )
    }
}

/** A medium tonal button that squares its corners while pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val height = ButtonDefaults.MediumContainerHeight
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(height),
        enabled = enabled,
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier = Modifier.height(height),
    ) {
        Text(text = text, style = ButtonDefaults.textStyleFor(height))
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            settings = UserSettings(),
            backupMessage = null,
            onWeightUnitChange = {},
            onRestSecondsChange = {},
            onPlateToggle = {},
            onExportBackup = {},
            onImportBackup = {},
            onBackupMessageShown = {},
        )
    }
}