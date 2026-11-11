package com.jacopo.mygymtracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.history.TrainingWeek
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.EmptyStateText
import com.jacopo.mygymtracker.ui.components.FloatingDockClearance
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.SectionLabel
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.format.formatWeekdayTime
import com.jacopo.mygymtracker.ui.format.formatWithUnit
import com.jacopo.mygymtracker.ui.format.shortDateFormatter
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onWorkoutClick: (sessionId: Int) -> Unit,
    onBodyWeightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            AppHeader(
                title = stringResource(R.string.history_title),
                scrollBehavior = scrollBehavior,
                pills = { if (!uiState.isLoading) HeaderPills(uiState.workoutCount, uiState.streakWeeks) },
            ) {
                WeekStrip(days = uiState.weekDays, today = uiState.today, trainedDays = uiState.trainedDays)
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FloatingDockClearance),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item(key = "body-weight") {
                BodyWeightItem(
                    latest = uiState.latestBodyWeight,
                    weightUnit = uiState.weightUnit,
                    onClick = onBodyWeightClick,
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 16.dp),
                )
            }
            if (uiState.weeks.isEmpty()) {
                item(key = "empty") {
                    EmptyStateText(
                        text = stringResource(R.string.history_empty),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            uiState.weeks.forEachIndexed { weekIndex, week ->
                item(key = "week-${week.start}") {
                    SectionLabel(
                        text = weekLabel(week.start, thisWeek = uiState.weekDays.firstOrNull()),
                        modifier = Modifier
                            .animateItem()
                            .padding(top = if (weekIndex == 0) 0.dp else 16.dp),
                    )
                }
                itemsIndexed(items = week.workouts, key = { _, workout -> workout.id }) { index, workout ->
                    WorkoutListItem(
                        workout = workout,
                        weightUnit = uiState.weightUnit,
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = week.workouts.size),
                        onClick = { onWorkoutClick(workout.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderPills(workoutCount: Int, streakWeeks: Int) {
    Pill(
        icon = painterResource(R.drawable.ic_fitness_center),
        text = pluralStringResource(R.plurals.history_workout_count, workoutCount, workoutCount),
    )
    if (streakWeeks > 0) {
        Pill(
            icon = painterResource(R.drawable.ic_history),
            text = pluralStringResource(R.plurals.history_week_streak, streakWeeks, streakWeeks),
        )
    }
}

/** The current week, one day per column; trained days get a cookie, today an outline. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeekStrip(days: List<LocalDate>, today: LocalDate, trainedDays: Set<LocalDate>) {
    val trainedDescription = stringResource(R.string.history_day_trained)
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    val locale = LocalConfiguration.current.locales[0]
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        days.forEach { day ->
            val isTrained = day in trainedDays
            val isToday = day == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    if (isTrained) stateDescription = trainedDescription
                },
            ) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val dayModifier = when {
                    isTrained -> Modifier.background(MaterialTheme.colorScheme.primary, cookie)
                    isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else -> Modifier
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).then(dayModifier)) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isTrained || isToday) FontWeight.SemiBold else null,
                        color = when {
                            isTrained -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.primary
                            day.isAfter(today) -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun weekLabel(start: LocalDate, thisWeek: LocalDate?): String = when (start) {
    thisWeek -> stringResource(R.string.history_this_week)
    thisWeek?.minusWeeks(1) -> stringResource(R.string.history_last_week)
    else -> stringResource(R.string.history_week_of, shortDateFormatter().format(start))
}

/** Opens the body weight log: the latest weight and its date, or a hint when nothing is logged. */
@Composable
private fun BodyWeightItem(
    latest: BodyWeight?,
    weightUnit: WeightUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        modifier = modifier,
        supportingContent = {
            Text(text = if (latest != null) formatDate(latest.date) else stringResource(R.string.body_weight_hint))
        },
        trailingContent = latest?.let {
            {
                Text(
                    text = weightUnit.formatWithUnit(it.weightKg),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) {
        Text(text = stringResource(R.string.body_weight_title), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WorkoutListItem(
    workout: WorkoutSummary,
    weightUnit: WeightUnit,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationSeconds = Duration.between(workout.startedAt, workout.finishedAt).seconds
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        overlineContent = {
            Text(
                text = "${formatWeekdayTime(workout.startedAt)} · ${formatDuration(durationSeconds)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pluralStringResource(R.plurals.history_set_count, workout.setCount, workout.setCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (workout.volumeKg > 0.0) Text(weightUnit.formatVolume(workout.volumeKg))
            }
        },
    ) {
        Text(
            text = workout.routineName,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val today = LocalDate.of(2025, 10, 15)
    val thisWeek = today.minusDays(2)
    fun workout(id: Int, routineName: String, day: LocalDate, minutes: Long): WorkoutSummary {
        val startedAt = day.atTime(18, 30).atZone(ZoneId.systemDefault()).toInstant()
        return WorkoutSummary(id, routineName, startedAt, startedAt.plusSeconds(minutes * 60), 18, 4_320.0)
    }
    AppTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                weeks = listOf(
                    TrainingWeek(thisWeek, listOf(workout(3, "Push Day", today, 62), workout(2, "Leg Day", thisWeek, 55))),
                    TrainingWeek(thisWeek.minusWeeks(1), listOf(workout(1, "Pull Day", thisWeek.minusDays(4), 48))),
                ),
                streakWeeks = 2,
                today = today,
                weekDays = (0L..6L).map(thisWeek::plusDays),
                trainedDays = setOf(today, thisWeek),
                latestBodyWeight = BodyWeight(date = today, weightKg = 78.4),
                isLoading = false,
            ),
            onWorkoutClick = {},
            onBodyWeightClick = {},
        )
    }
}
