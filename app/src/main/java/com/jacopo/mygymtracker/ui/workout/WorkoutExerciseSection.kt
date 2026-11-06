package com.jacopo.mygymtracker.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.graphics.shapes.Morph
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.normalSetNumbers
import com.jacopo.mygymtracker.ui.components.AddSetButton
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
import com.jacopo.mygymtracker.ui.components.PrTag
import com.jacopo.mygymtracker.ui.components.RestChip
import com.jacopo.mygymtracker.ui.components.SetRow
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.setMarker
import com.jacopo.mygymtracker.ui.format.setName
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.delay

/** Gap between the tick's haptic and the record's, so they read as two beats. */
private const val PR_HAPTIC_DELAY_MS = 150L

/**
 * One exercise of the workout as a checklist: a line per set with last time's values.
 * Tick a set when it's done, tap it to edit it, swipe it to delete it.
 * The next set to do is highlighted.
 *
 * @param onStartRest starts this exercise's rest by hand; null hides the button, as the rest starts on its own.
 * @param onWarmUp opens the warm-up sets for this exercise; null hides the menu item, as there's no weight to warm up for.
 */
@Composable
fun WorkoutExerciseSection(
    exercise: WorkoutExerciseUiState,
    previousSets: List<LoggedSet>,
    prSetKeys: Set<String>,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    onRemove: () -> Unit,
    onWarmUp: (() -> Unit)?,
    onAddSet: () -> Unit,
    onEditRest: () -> Unit,
    onStartRest: (() -> Unit)?,
    onOpenSet: (setKey: String) -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onCompletedChange: (setKey: String, completed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sets = exercise.sets
    val doneCount = sets.count { it.isCompleted }
    val nextIndex = sets.indexOfFirst { !it.isCompleted }
    val numbers = sets.map { it.type }.normalSetNumbers()
    Column(
        modifier = modifier
            .padding(top = 20.dp, bottom = 4.dp)
            .animateContentSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pluralStringResource(R.plurals.workout_sets_progress, sets.size, doneCount, sets.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DestructiveOverflowMenu(action = stringResource(R.string.action_remove_exercise), onAction = onRemove) { close ->
                if (onWarmUp != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.warm_up_add)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            close()
                            onWarmUp()
                        },
                    )
                }
            }
        }
        sets.forEachIndexed { index, set ->
            key(set.key) {
                SwipeToDeleteContainer(onDelete = { onDeleteSet(set.key) }) {
                    val trackingType = exercise.exercise.trackingType
                    val previous = previousSets.getOrNull(index)
                    val number = numbers[index]
                    SetRow(
                        value = formatSet(trackingType, weightUnit, set.reps, set.weightKg, set.durationSeconds, set.rpe),
                        leading = {
                            SetCheck(
                                name = setName(number, set.type),
                                // Normal sets keep the plain ring; the others show their letter until ticked.
                                letter = if (set.type != SetType.NORMAL) setMarker(number, set.type) else null,
                                checked = set.isCompleted,
                                onCheckedChange = { completed -> onCompletedChange(set.key, completed) },
                            )
                        },
                        onClick = { onOpenSet(set.key) },
                        isHighlighted = index == nextIndex,
                        isDimmed = set.isCompleted,
                    ) {
                        LivePrTag(visible = set.key in prSetKeys)
                        if (previous != null) {
                            Text(
                                text = stringResource(
                                    R.string.workout_set_last,
                                    formatSet(trackingType, weightUnit, previous.reps, previous.weightKg, previous.durationSeconds),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, end = 20.dp),
        ) {
            AddSetButton(onClick = onAddSet)
            Spacer(modifier = Modifier.weight(1f))
            RestChip(restSeconds = exercise.restSeconds, defaultSeconds = defaultRestSeconds, onClick = onEditRest)
            if (onStartRest != null) {
                IconButton(onClick = onStartRest) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = stringResource(R.string.rest_start_description))
                }
            }
        }
    }
}

/**
 * The PR tag of a set, popping in with a spinning star and a confirm haptic when ticking the set
 * makes it a record. Already shown when the screen opens, it stays still and silent.
 */
@Composable
private fun LivePrTag(visible: Boolean) {
    val haptics = LocalHapticFeedback.current
    var wasVisible by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible && !wasVisible) {
            delay(PR_HAPTIC_DELAY_MS) // lets the tick's own haptic finish first
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        wasVisible = visible
    }
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        val spin by transition.animateFloat(transitionSpec = { spring(stiffness = Spring.StiffnessLow) }) { state ->
            if (state == EnterExitState.Visible) 0f else -360f
        }
        PrTag(
            modifier = Modifier.padding(end = 8.dp),
            starModifier = Modifier.graphicsLayer { rotationZ = spin },
        )
    }
}

/**
 * A ring that morphs into a filled, turning cookie with a tick once the set is done,
 * with a haptic tick each way. A [letter], if any, sits in the ring and fades out as it fills.
 *
 * @param name the set's name, e.g. "Set 2", for accessibility.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetCheck(name: String, letter: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "setCheck",
    )
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val fillColor = MaterialTheme.colorScheme.primary
    val ringColor = MaterialTheme.colorScheme.outline
    val haptics = LocalHapticFeedback.current
    val description = stringResource(R.string.workout_set_done, name)
    IconToggleButton(
        checked = checked,
        onCheckedChange = { completed ->
            haptics.performHapticFeedback(if (completed) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onCheckedChange(completed)
        },
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .drawWithCache {
                    // The spring overshoots: the shape uses the clamped value, size and turn the raw one.
                    val t = progress.coerceIn(0f, 1f)
                    val side = size.minDimension * lerp(0.8f, 1f, progress)
                    val path = morph.toPath(t) // normalized to a 1 x 1 square
                    val ring = Stroke(width = 2.dp.toPx() / side)
                    onDrawBehind {
                        rotate(40f * progress) {
                            translate(center.x - side / 2, center.y - side / 2) {
                                scale(side, pivot = Offset.Zero) {
                                    drawPath(path, fillColor, alpha = t)
                                    drawPath(path, ringColor, alpha = 1f - t, style = ring)
                                }
                            }
                        }
                    }
                },
        ) {
            if (letter != null) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .graphicsLayer { alpha = 1f - progress.coerceIn(0f, 1f) },
                )
            }
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.graphicsLayer {
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = progress
                    scaleY = progress
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutExerciseSectionPreview() {
    val record = WorkoutSetUiState(reps = 8, weightKg = 80.0, isCompleted = true)
    AppTheme {
        WorkoutExerciseSection(
            exercise = WorkoutExerciseUiState(
                exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                sets = listOf(
                    WorkoutSetUiState(reps = 10, weightKg = 40.0, isCompleted = true, type = SetType.WARM_UP),
                    record,
                    WorkoutSetUiState(reps = 8, weightKg = 80.0),
                    WorkoutSetUiState(reps = 8, weightKg = 80.0),
                ),
            ),
            previousSets = listOf(LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true)),
            prSetKeys = setOf(record.key),
            weightUnit = WeightUnit.KG,
            defaultRestSeconds = 90,
            onRemove = {},
            onWarmUp = {},
            onAddSet = {},
            onEditRest = {},
            onStartRest = null,
            onOpenSet = {},
            onDeleteSet = {},
            onCompletedChange = { _, _ -> },
        )
    }
}
