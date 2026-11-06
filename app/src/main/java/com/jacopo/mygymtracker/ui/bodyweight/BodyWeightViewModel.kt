package com.jacopo.mygymtracker.ui.bodyweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.BodyWeightRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** The open log sheet: a new entry when [entry] is `null`, otherwise that entry being edited. */
data class BodyWeightEditor(val entry: BodyWeight?)

/**
 * @property entries oldest first.
 * @property today the last day an entry can be logged for.
 * @property editor the open log sheet, `null` when closed.
 */
data class BodyWeightUiState(
    val entries: List<BodyWeight> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val today: LocalDate = LocalDate.ofEpochDay(0),
    val editor: BodyWeightEditor? = null,
    val isLoading: Boolean = true,
) {
    val latest: BodyWeight? = entries.lastOrNull()
}

class BodyWeightViewModel(
    private val bodyWeightRepository: BodyWeightRepository,
    settingsRepository: SettingsRepository,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val editor = MutableStateFlow<BodyWeightEditor?>(null)

    val uiState: StateFlow<BodyWeightUiState> = combine(
        bodyWeightRepository.observeAll(),
        settingsRepository.observeSettings(),
        editor,
    ) { entries, settings, editor ->
        BodyWeightUiState(
            entries = entries,
            weightUnit = settings.weightUnit,
            today = LocalDate.now(clock),
            editor = editor,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BodyWeightUiState(),
    )

    /** Opens the log sheet for [entry], or for a new entry. */
    fun openEditor(entry: BodyWeight? = null) {
        editor.value = BodyWeightEditor(entry)
    }

    fun closeEditor() {
        editor.value = null
    }

    /** Saves the sheet and closes it; an edited entry moves if its date changed. */
    fun save(date: LocalDate, weightKg: Double) {
        val replacing = editor.value?.entry?.date
        editor.value = null
        viewModelScope.launch { bodyWeightRepository.save(BodyWeight(date, weightKg), replacing) }
    }

    fun delete(date: LocalDate) {
        viewModelScope.launch { bodyWeightRepository.delete(date) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                BodyWeightViewModel(
                    bodyWeightRepository = container.bodyWeightRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
