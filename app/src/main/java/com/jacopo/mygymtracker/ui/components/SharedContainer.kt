package com.jacopo.mygymtracker.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/** The nav host's [SharedTransitionScope]. Null in previews, which turns [sharedContainer] off. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The current destination's enter/exit scope. Only destinations that share bounds provide it. */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Container transform: an element grows into the root of the screen it opens and shrinks back on pop.
 * Give both ends the same [key]. Their bounds morph while the contents crossfade, so the corners blend from
 * one end's [shape] into the other's. Does nothing when either local is missing.
 *
 * @param shape clips this end while it is in flight; pass the element's resting shape.
 */
@Composable
fun Modifier.sharedContainer(key: Any, shape: Shape = RectangleShape): Modifier {
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(transitionScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibilityScope,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}
