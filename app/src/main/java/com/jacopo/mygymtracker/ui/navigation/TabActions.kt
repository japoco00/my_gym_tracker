package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.jacopo.mygymtracker.ui.components.DockAction

/** Each tab's primary action, filled by its destination and shown by the shell's dock. */
val LocalTabActions = staticCompositionLocalOf<MutableMap<TopLevelDestination, DockAction>> { mutableMapOf() }

/**
 * Hands [tab]'s primary action to the dock. Never removed, so the dock keeps its "+" while a
 * full-screen destination covers the tab.
 */
@Composable
fun TabActionEffect(tab: TopLevelDestination, label: String, onClick: () -> Unit) {
    val actions = LocalTabActions.current
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(actions, tab, label) {
        actions[tab] = DockAction(label) { currentOnClick() }
    }
}
