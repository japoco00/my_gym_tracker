package com.jacopo.mygymtracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.data.repository.BackupRepository
import com.jacopo.mygymtracker.data.repository.ImportResult
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.ThemeMode
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    /** `null` until DataStore has been read, so defaults never flash on screen. */
    val settings: StateFlow<UserSettings?> = settingsRepository.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _backupMessage = MutableStateFlow<Int?>(null)

    /** String resource saying how the last export or import went, until the screen calls [onBackupMessageShown]. */
    val backupMessage: StateFlow<Int?> = _backupMessage.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = if (backupRepository.exportTo(uri)) {
                R.string.backup_exported
            } else {
                R.string.backup_export_failed
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = if (backupRepository.exportCsvTo(uri)) {
                R.string.csv_exported
            } else {
                R.string.csv_export_failed
            }
        }
    }

    /** Replaces everything with the backup at [uri]; the screen has already asked for confirmation. */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = when (backupRepository.importFrom(uri)) {
                ImportResult.IMPORTED -> R.string.backup_imported
                ImportResult.INVALID_FILE -> R.string.backup_invalid_file
                ImportResult.WORKOUT_IN_PROGRESS -> R.string.backup_workout_in_progress
            }
        }
    }

    fun onBackupMessageShown() {
        _backupMessage.value = null
    }

    fun setName(name: String) {
        viewModelScope.launch { settingsRepository.setName(name) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    fun changeRestSeconds(deltaSeconds: Int) {
        val current = settings.value ?: return
        viewModelScope.launch { settingsRepository.setRestSeconds(current.restSeconds + deltaSeconds) }
    }

    /** Adds or removes [plate] among the plates owned in the current unit. */
    fun togglePlate(plate: Double) {
        val current = settings.value ?: return
        viewModelScope.launch { settingsRepository.togglePlate(current.weightUnit, plate) }
    }

    fun setAutoStartRest(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoStartRest(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = appContainer().settingsRepository,
                    backupRepository = appContainer().backupRepository,
                )
            }
        }
    }
}
