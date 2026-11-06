package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.bodyweight.BodyWeightScreen
import com.jacopo.mygymtracker.ui.bodyweight.BodyWeightViewModel
import kotlinx.serialization.Serializable

@Serializable
data object BodyWeightRoute

fun NavGraphBuilder.bodyWeightDestination(
    onNavigateBack: () -> Unit,
) {
    composable<BodyWeightRoute> {
        val viewModel: BodyWeightViewModel = viewModel(factory = BodyWeightViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        BodyWeightScreen(
            uiState = uiState,
            onBack = onNavigateBack,
            onLogWeight = { viewModel.openEditor() },
            onEntryClick = viewModel::openEditor,
            onDeleteEntry = viewModel::delete,
            onSave = viewModel::save,
            onDismissEditor = viewModel::closeEditor,
        )
    }
}

fun NavController.navigateToBodyWeight() {
    navigate(route = BodyWeightRoute)
}

/** Pops the body weight screen only, so a double tap on Back can't also pop the screen below. */
fun NavController.closeBodyWeight() {
    popBackStack<BodyWeightRoute>(inclusive = true)
}
