package com.jacopo.mygymtracker.ui.workout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.ShapeBadge
import com.jacopo.mygymtracker.ui.components.popScale
import com.jacopo.mygymtracker.ui.components.rememberElapsedTime
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Instant

/**
 * The workout's title, time, volume and set progress. The big title and the pills collapse
 * into a regular top bar on scroll; the sets badge and the wavy progress bar stay visible.
 * Its colour runs behind the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHeader(
    routineName: String,
    startedAt: Instant,
    completedSets: Int,
    totalSets: Int,
    completedVolumeKg: Double,
    weightUnit: WeightUnit,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppHeader(
        title = routineName,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            SetsBadge(completed = completedSets, total = totalSets)
            DestructiveOverflowMenu(action = stringResource(R.string.workout_discard), onAction = onDiscard)
        },
        pills = {
            Pill {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(text = rememberElapsedTime(startedAt))
            }
            Pill { Text(text = weightUnit.formatVolume(completedVolumeKg)) }
        },
        collapsedSubtitle = { Text(text = rememberElapsedTime(startedAt)) },
    ) {
        LinearWavyProgressIndicator(
            progress = { if (totalSets == 0) 0f else completedSets / totalSets.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

/** "done/total sets" in a soft burst; it bumps each time a set is ticked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetsBadge(completed: Int, total: Int) {
    val scale by popScale(value = completed, from = 0.85f, pops = { old, new -> new > old })
    ShapeBadge(
        shape = MaterialShapes.SoftBurst,
        value = "$completed/$total",
        label = stringResource(R.string.workout_sets_badge),
        contentDescription = pluralStringResource(R.plurals.workout_sets_progress, total, completed, total),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun WorkoutHeaderPreview() {
    AppTheme {
        WorkoutHeader(
            routineName = "Push Day",
            startedAt = Instant.now().minusSeconds(754),
            completedSets = 3,
            totalSets = 8,
            completedVolumeKg = 1_920.0,
            weightUnit = WeightUnit.KG,
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            onBack = {},
            onDiscard = {},
        )
    }
}
