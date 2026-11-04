package com.jacopo.mygymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.graphics.shapes.Morph
import com.jacopo.mygymtracker.R

/** Share of the row width to swipe past for the delete to "arm", i.e. happen on release. */
private const val DELETE_THRESHOLD = 0.3f

/** Wraps [content] so that swiping it from end to start triggers [onDelete]. */
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    // Plain remember, not saveable: an item restored by "Undo" must come back un-swiped.
    val dismissState = remember {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = { totalDistance -> totalDistance * DELETE_THRESHOLD },
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        // Called once the swipe has settled, so the dismiss animation has already played.
        onDismiss = { currentOnDelete() },
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                DeleteBackground(
                    swipeOffset = dismissState::requireOffset,
                    armed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart,
                )
            }
        },
        content = content,
    )
}

/**
 * Fades in with the swipe. Past the threshold the delete "arms": the bin's backdrop morphs
 * from a faint circle into a solid cookie, the bin wiggles and the phone ticks once.
 *
 * @param swipeOffset the row's offset in px, negative while swiping; read only when drawing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeleteBackground(swipeOffset: () -> Float, armed: Boolean) {
    val arm by animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "deleteArm",
    )
    val wiggle = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(armed) {
        if (!armed) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        wiggle.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 300
                -14f at 75
                12f at 150
            },
        )
    }
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val reveal = (-swipeOffset() / size.width / DELETE_THRESHOLD).coerceIn(0f, 1f)
                drawRect(colors.errorContainer, alpha = reveal)
                // The spring overshoots: the shape uses the clamped value, the turn the raw one.
                val t = arm.coerceIn(0f, 1f)
                val side = lerp(28.dp.toPx(), 44.dp.toPx(), reveal)
                val shapeCenter = Offset(size.width - BinCenterFromEnd.toPx(), center.y)
                translate(shapeCenter.x - side / 2, shapeCenter.y - side / 2) {
                    rotate(40f * arm, pivot = Offset(side / 2, side / 2)) {
                        scale(side, pivot = Offset.Zero) {
                            // Normalized to a 1 x 1 square, hence the scale.
                            drawPath(morph.toPath(t), colors.error, alpha = lerp(0.12f, 1f, t) * reveal)
                        }
                    }
                }
            },
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.action_delete),
            tint = lerp(colors.onErrorContainer, colors.onError, arm.coerceIn(0f, 1f)),
            modifier = Modifier
                .padding(end = BinCenterFromEnd - 12.dp) // 12dp: half the icon
                .graphicsLayer { rotationZ = wiggle.value },
        )
    }
}

private val BinCenterFromEnd = 40.dp
