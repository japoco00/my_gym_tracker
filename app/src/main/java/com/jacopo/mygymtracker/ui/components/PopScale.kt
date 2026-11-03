package com.jacopo.mygymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * A scale that jumps to [from] and springs back to 1 whenever [value] changes and [pops] agrees.
 * Never on first composition, so opening the screen or scrolling a row back in doesn't animate.
 * Read it inside `graphicsLayer { }` so each frame redraws without recomposing.
 */
@Composable
fun <T> popScale(
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
