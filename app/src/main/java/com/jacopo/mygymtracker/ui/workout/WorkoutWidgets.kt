package com.jacopo.mygymtracker.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.jacopo.mygymtracker.R

/**
 * A scale that jumps to [from] and springs back to 1 whenever [value] changes and [pops] agrees.
 * Never on first composition, so opening the screen or scrolling a row back in doesn't animate.
 * Read it inside `graphicsLayer { }` so each frame redraws without recomposing.
 */
@Composable
internal fun <T> popScale(
    value: T,
    from: Float,
    pops: (old: T, new: T) -> Boolean = { _, _ -> true },
): State<Float> {
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        val old = previous
        previous = value
        if (old != value && pops(old, value)) {
            scale.snapTo(from)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
            )
        }
    }
    return scale.asState()
}

/** A ⋮ button whose menu holds one destructive [action], e.g. "Remove exercise". */
@Composable
internal fun DestructiveOverflowMenu(action: String, onAction: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val error = MaterialTheme.colorScheme.error
            DropdownMenuItem(
                text = { Text(action) },
                leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(textColor = error, leadingIconColor = error),
                onClick = {
                    expanded = false
                    onAction()
                },
            )
        }
    }
}
