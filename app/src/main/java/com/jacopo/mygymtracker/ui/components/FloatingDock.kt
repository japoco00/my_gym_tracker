package com.jacopo.mygymtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/** Space a tab screen leaves under its last row: the dock, its margin and a gap. */
val FloatingDockClearance = 96.dp

/** A tab's primary action, shown as the dock's "+". */
class DockAction(val label: String, val onClick: () -> Unit)

private val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private val DockElevation = 6.dp

/**
 * The tab bar as a floating pill, with the current tab's [action] attached as a "+" segment.
 * Off spec: M3 has no floating navigation bar, so this wraps a [ShortNavigationBar] with icons only.
 * The "+" fades and shrinks first, then the gap closes; the hand-timed exit keeps the bar from
 * jumping while the button is still visible.
 */
@Composable
fun FloatingDock(action: DockAction?, modifier: Modifier = Modifier, items: @Composable () -> Unit) {
    // Keeps the label while the "+" animates away.
    var shownAction by remember { mutableStateOf(action) }
    if (action != null) shownAction = action
    val hasAction = action != null
    val motion = MaterialTheme.motionScheme
    val corner by animateDpAsState(
        targetValue = if (hasAction) 8.dp else 32.dp,
        animationSpec = if (hasAction) motion.defaultSpatialSpec() else tween(300, delayMillis = 100, easing = Standard),
    )
    val barShape = RoundedCornerShape(topStart = 32.dp, topEnd = corner, bottomEnd = corner, bottomStart = 32.dp)
    Row(modifier = modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        ShortNavigationBar(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 272.dp)
                .shadow(DockElevation, barShape),
            windowInsets = WindowInsets(0),
            content = items,
        )
        // Unclipped, or the clip would cut the button's shadow.
        AnimatedVisibility(
            visible = hasAction,
            enter = expandHorizontally(motion.defaultSpatialSpec(), expandFrom = Alignment.Start, clip = false) +
                fadeIn(motion.defaultEffectsSpec()),
            exit = fadeOut(tween(100, easing = LinearEasing)) +
                scaleOut(tween(150, easing = StandardAccelerate), targetScale = 0.6f) +
                shrinkHorizontally(tween(350, delayMillis = 100, easing = Standard), shrinkTowards = Alignment.Start, clip = false),
        ) {
            FloatingActionButton(
                onClick = { action?.onClick() },
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 32.dp, bottomEnd = 32.dp, bottomStart = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(64.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = shownAction?.label)
            }
        }
    }
}
