package com.jacopo.mygymtracker.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.settings.SettingsScreen
import com.jacopo.mygymtracker.ui.settings.SettingsViewModel
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data object SettingsRoute

private const val BACKUP_MIME_TYPE = "application/json"

fun NavGraphBuilder.settingsDestination() {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
        // The system file pickers: the app needs no storage permission, only the one file the user picks.
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) {
            it?.let(viewModel::exportBackup)
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let(viewModel::importBackup)
        }

        SettingsScreen(
            settings = settings,
            backupMessage = backupMessage,
            onWeightUnitChange = viewModel::setWeightUnit,
            onRestSecondsChange = viewModel::changeRestSeconds,
            onPlateToggle = viewModel::togglePlate,
            onAutoStartRestChange = viewModel::setAutoStartRest,
            onKeepScreenOnChange = viewModel::setKeepScreenOn,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
            onExportBackup = { exportLauncher.launch("my-gym-tracker-${LocalDate.now()}.json") },
            // Some providers report .json files as a generic binary.
            onImportBackup = { importLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "application/octet-stream")) },
            onBackupMessageShown = viewModel::onBackupMessageShown,
        )
    }
}
