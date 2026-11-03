package com.jacopo.mygymtracker.ui.workout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
import com.jacopo.mygymtracker.ui.components.SetRow
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.components.popScale
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * One exercise of the workout as a checklist: a line per set with last time's values.
 * Tick a set when it's done, tap it to edit it, swipe it to delete it.
 * The next set to do is highlighted.
 */
@Composable
fun WorkoutExerciseSection(
    exercise: WorkoutExerciseUiState,
    previousSets: List<LoggedSet>,
    weightUnit: WeightUnit,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onOpenSet: (setKey: String) -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onCompletedChange: (setKey: String, completed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sets = exercise.sets
    val doneCount = sets.count { it.isCompleted }
    val nextIndex = sets.indexOfFirst { !it.isCompleted }
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
            DestructiveOverflowMenu(action = stringResource(R.string.workout_remove_exercise), onAction = onRemove)
        }
        sets.forEachIndexed { index, set ->
            key(set.key) {
                SwipeToDeleteContainer(onDelete = { onDeleteSet(set.key) }) {
                    val trackingType = exercise.exercise.trackingType
                    val previous = previousSets.getOrNull(index)
                    SetRow(
                        value = formatSet(trackingType, weightUnit, set.reps, set.weightKg, set.durationSeconds),
                        leading = {
                            SetCheck(
                                number = index + 1,
                                checked = set.isCompleted,
                                onCheckedChange = { completed -> onCompletedChange(set.key, completed) },
                            )
                        },
                        onClick = { onOpenSet(set.key) },
                        isHighlighted = index == nextIndex,
                        isDimmed = set.isCompleted,
                    ) {
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
        TextButton(onClick = onAddSet, modifier = Modifier.padding(start = 52.dp)) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.action_add_set))
        }
    }
}

/** An empty ring, or a filled cookie with a tick once the set is done; it pops on every change. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetCheck(number: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scale by popScale(value = checked, from = 0.6f)
    val description = stringResource(R.string.workout_set_done, number)
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie9Sided.toShape()),
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutExerciseSectionPreview() {
    AppTheme {
        WorkoutExerciseSection(
            exercise = WorkoutExerciseUiState(
                exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                sets = listOf(
                    WorkoutSetUiState(reps = 8, weightKg = 80.0, isCompleted = true),
                    WorkoutSetUiState(reps = 8, weightKg = 80.0),
                    WorkoutSetUiState(reps = 8, weightKg = 80.0),
                ),
            ),
            previousSets = listOf(LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true)),
            weightUnit = WeightUnit.KG,
            onRemove = {},
            onAddSet = {},
            onOpenSet = {},
            onDeleteSet = {},
            onCompletedChange = { _, _ -> },
        )
    }
}
