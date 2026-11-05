package com.jacopo.mygymtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App-wide state for [MainActivity]: the settings that decide the theme. */
class MainViewModel(settingsRepository: SettingsRepository) : ViewModel() {

    /** `null` until DataStore has been read. Eager, because the activity polls it before anything collects it. */
    val settings: StateFlow<UserSettings?> = settingsRepository.observeSettings()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(appContainer().settingsRepository) }
        }
    }
}
