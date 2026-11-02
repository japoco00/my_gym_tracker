package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.settings.SettingsScreen
import com.jacopo.mygymtracker.ui.settings.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsDestination() {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val settings by viewModel.settings.collectAsStateWithLifecycle()

        SettingsScreen(
            settings = settings,
            onWeightUnitChange = viewModel::setWeightUnit,
            onRestSecondsChange = viewModel::changeRestSeconds,
        )
    }
}
