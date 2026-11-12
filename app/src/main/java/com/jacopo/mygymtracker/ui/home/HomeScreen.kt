package com.jacopo.mygymtracker.ui.home

import androidx.collection.intListOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.text.htmlEncode
import androidx.graphics.shapes.Morph
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.home.Greeting
import com.jacopo.mygymtracker.domain.home.HomeSituation
import com.jacopo.mygymtracker.domain.home.TimeOfDay
import com.jacopo.mygymtracker.domain.home.homeStats
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.domain.progress.PersonalRecord
import com.jacopo.mygymtracker.ui.components.ActiveWorkoutCard
import com.jacopo.mygymtracker.ui.components.FloatingDockClearance
import com.jacopo.mygymtracker.ui.components.StartConflictDialog
import com.jacopo.mygymtracker.ui.format.formatRelativeDay
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset

/** The start tab: a greeting, the next workout to start and the stats worth a glance. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onStartWorkout: (Routine) -> Unit,
    onStartEmptyWorkout: () -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenRoutines: () -> Unit,
    onResumeWorkout: (sessionId: Int) -> Unit,
    onDiscardActiveAndStart: () -> Unit,
    onDismissStartConflict: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkoutDetail: (sessionId: Int) -> Unit,
    onOpenBodyWeight: () -> Unit,
    onOpenExerciseProgress: (exerciseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeWorkout = uiState.activeWorkout
    val startConflict = uiState.startConflict
    if (startConflict != null && activeWorkout != null) {
        StartConflictDialog(
            activeWorkout = activeWorkout,
            routine = startConflict,
            onResume = {
                onDismissStartConflict()
                activeWorkout.id?.let(onResumeWorkout)
            },
            onDiscardAndStart = onDiscardActiveAndStart,
            onDismiss = onDismissStartConflict,
        )
    }

    // Here rather than in the header, so scrolling the header away and back doesn't replay it.
    val greetingShown = remember { MutableTransitionState(false).apply { targetState = true } }
    Scaffold(modifier = modifier) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            // Edge to edge, so the header's shape runs under the status bar; the bottom clears the floating dock.
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + FloatingDockClearance),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                PosterHeader(
                    uiState = uiState,
                    greetingShown = greetingShown,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                )
            }
            item(key = "actions") {
                if (activeWorkout != null) {
                    ActiveWorkoutCard(
                        workout = activeWorkout,
                        onResume = { activeWorkout.id?.let(onResumeWorkout) },
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
                    )
                } else {
                    StartSplitButton(
                        upNext = uiState.upNext,
                        trainedToday = uiState.situation == HomeSituation.TrainedToday,
                        onStartWorkout = onStartWorkout,
                        onCreateRoutine = onCreateRoutine,
                        onOpenRoutines = onOpenRoutines,
                        onStartEmptyWorkout = onStartEmptyWorkout,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
                    )
                }
            }
            item(key = "tiles") {
                HomeTiles(
                    uiState = uiState,
                    onOpenHistory = onOpenHistory,
                    onOpenWorkoutDetail = onOpenWorkoutDetail,
                    onOpenBodyWeight = onOpenBodyWeight,
                    onOpenExerciseProgress = onOpenExerciseProgress,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

/** A big greeting over a slowly morphing shape, with the avatar that opens the settings. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PosterHeader(
    uiState: HomeUiState,
    greetingShown: MutableTransitionState<Boolean>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie4Sided) }
    val motion = MaterialTheme.motionScheme
    Box(modifier = modifier.fillMaxWidth()) {
        // Bigger than the header and partly off screen: it doesn't change the header's size.
        MorphingShape(
            morph = morph,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .matchParentSize()
                .wrapContentSize(Alignment.TopEnd, unbounded = true)
                .offset(x = 110.dp, y = (-80).dp)
                .size(340.dp),
        )
        Avatar(
            name = uiState.name,
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
        )
        Column(modifier = Modifier.padding(start = 24.dp, top = 80.dp, end = 20.dp)) {
            uiState.greeting?.let { greeting ->
                AnimatedVisibility(
                    visibleState = greetingShown,
                    enter = fadeIn(motion.slowEffectsSpec()) + slideInVertically(motion.slowSpatialSpec()) { it / 3 },
                ) {
                    Text(
                        text = rememberHighlighted(greeting.html(), MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.displayLarge,
                    )
                }
            }
            subline(uiState)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** The name's initial, or a person without a name, in a cookie. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Avatar(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.settings_title)
    val initial = name.trim().take(1).uppercase()
    Surface(
        onClick = onClick,
        shape = MaterialShapes.Cookie7Sided.toShape(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (initial.isEmpty()) {
                Icon(Icons.Default.Person, contentDescription = null)
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

/** What the last workout was, or nothing while one is running. */
@Composable
private fun subline(uiState: HomeUiState): String? {
    if (uiState.activeWorkout != null) return null
    val last = uiState.lastWorkout ?: return stringResource(R.string.home_no_workouts)
    return if (uiState.situation == HomeSituation.TrainedToday) {
        stringResource(
            R.string.home_trained_today,
            last.routineName,
            stringResource(R.string.home_minutes, Duration.between(last.startedAt, last.finishedAt).toMinutes()),
            uiState.weightUnit.formatVolume(last.volumeKg),
        )
    } else {
        stringResource(R.string.routines_last_workout, last.routineName, formatRelativeDay(last.finishedAt))
    }
}

/**
 * Starts [upNext], or creates the first routine without one; the arrow lists the other ways to start.
 * Tonal once trained today: the next routine is an option, not the call to action.
 */
@Composable
private fun StartSplitButton(
    upNext: Routine?,
    trainedToday: Boolean,
    onStartWorkout: (Routine) -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenRoutines: () -> Unit,
    onStartEmptyWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = SplitButtonDefaults.MediumContainerHeight
    val colors = if (trainedToday) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
    val label = when {
        upNext == null -> stringResource(R.string.routines_create)
        trainedToday -> stringResource(R.string.home_next_routine, upNext.name)
        else -> stringResource(R.string.routine_start, upNext.name)
    }
    var menuOpen by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (menuOpen) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = { if (upNext == null) onCreateRoutine() else onStartWorkout(upNext) },
                    shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                    colors = colors,
                    contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                    modifier = Modifier.heightIn(size),
                ) {
                    Icon(
                        imageVector = if (upNext == null) Icons.Default.Add else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(SplitButtonDefaults.leadingButtonIconSizeFor(size)),
                    )
                    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                    Text(text = label, style = ButtonDefaults.textStyleFor(size), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            trailingButton = {
                val state = stringResource(if (menuOpen) R.string.state_expanded else R.string.state_collapsed)
                SplitButtonDefaults.TrailingButton(
                    checked = menuOpen,
                    onCheckedChange = { menuOpen = it },
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                    colors = colors,
                    contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                    modifier = Modifier
                        .heightIn(size)
                        .semantics { stateDescription = state },
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.home_more_ways_to_start),
                        modifier = Modifier
                            .size(SplitButtonDefaults.trailingButtonIconSizeFor(size))
                            .graphicsLayer { rotationZ = arrowRotation },
                    )
                }
            },
        )
        val gap = with(LocalDensity.current) { 4.dp.roundToPx() }
        val menuPosition = remember(gap) {
            // End-aligned, so the menu opens under the arrow that toggles it; above the button when there's no room below.
            MenuAnchorPosition.Custom(
                xCandidates = {
                    intListOf(if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - menuSize.width else anchorBounds.left)
                },
                yCandidates = { intListOf(anchorBounds.bottom + gap, anchorBounds.top - gap - menuSize.height) },
            )
        }
        DropdownMenuPopup(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(menuPosition),
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                val count = if (upNext != null) 2 else 1
                if (upNext != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_other_routine)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_list), contentDescription = null) },
                        shape = MenuDefaults.itemShape(index = 0, count = count).shape,
                        onClick = {
                            menuOpen = false
                            onOpenRoutines()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.routines_empty_workout)) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    shape = MenuDefaults.itemShape(index = count - 1, count = count).shape,
                    onClick = {
                        menuOpen = false
                        onStartEmptyWorkout()
                    },
                )
            }
        }
    }
}

/** The greeting as HTML, its key words in `<b>`. */
@Composable
private fun Greeting.html(): String = when (this) {
    is Greeting.Hello -> {
        val (plain, named) = when (timeOfDay) {
            TimeOfDay.MORNING -> R.string.home_hello_morning to R.string.home_hello_morning_named
            TimeOfDay.AFTERNOON -> R.string.home_hello_afternoon to R.string.home_hello_afternoon_named
            TimeOfDay.EVENING -> R.string.home_hello_evening to R.string.home_hello_evening_named
            TimeOfDay.NIGHT -> R.string.home_hello_night to R.string.home_hello_night_named
        }
        if (name == null) stringResource(plain) else stringResource(named, name.htmlEncode())
    }
    is Greeting.Welcome ->
        if (name == null) stringResource(R.string.home_welcome) else stringResource(R.string.home_welcome_named, name.htmlEncode())
    Greeting.FirstRoutine -> stringResource(R.string.home_first_routine)
    is Greeting.StillRunning -> stringResource(R.string.home_still_running, routineName.htmlEncode())
    Greeting.SetsMissYou -> stringResource(R.string.home_sets_miss_you)
    is Greeting.SetsDone -> stringResource(R.string.home_sets_done, done, total)
    Greeting.NiceWork -> stringResource(R.string.home_nice_work)
    Greeting.RestIsTraining -> stringResource(R.string.home_rest_is_training)
    Greeting.GoEat -> stringResource(R.string.home_go_eat)
    is Greeting.DaysOff -> pluralStringResource(R.plurals.home_days_off, days, days)
    Greeting.BarMissedYou -> stringResource(R.string.home_bar_missed_you)
    is Greeting.EasyRestart -> stringResource(R.string.home_easy_restart, routineName.htmlEncode())
    Greeting.NextSession -> stringResource(R.string.home_next_session)
    is Greeting.Calling -> stringResource(R.string.home_calling, routineName.htmlEncode())
    is Greeting.WeeksInARow -> pluralStringResource(R.plurals.home_weeks_in_a_row, weeks, weeks)
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun HomeScreenPreview() {
    val today = LocalDate.now()
    val routines = listOf("Push", "Pull", "Legs")
    // Every two or three days, newest first.
    val history = (0 until 40).map { i ->
        val startedAt = today.minusDays(1L + i * 5 / 2).atTime(18, 0).toInstant(ZoneOffset.UTC)
        WorkoutSummary(
            id = i,
            routineName = routines[i % 3],
            startedAt = startedAt,
            finishedAt = startedAt.plusSeconds(2_700L + i * 97 % 1_500),
            setCount = 18,
            volumeKg = 3_800.0 + i * 431 % 1_700,
            routineId = i % 3,
        )
    }
    val bench = Exercise(id = 1, name = "Bench Press", category = Category.CHEST)
    val squat = Exercise(id = 2, name = "Squat", category = Category.LEGS)
    AppTheme {
        HomeScreen(
            uiState = HomeUiState(
                greeting = Greeting.Hello(TimeOfDay.EVENING, "Jacopo"),
                situation = HomeSituation.Usual,
                today = today,
                name = "Jacopo",
                upNext = Routine(id = 3, name = "Legs"),
                lastWorkout = history.first(),
                stats = homeStats(history, today, ZoneOffset.UTC, DayOfWeek.MONDAY),
                records = listOf(
                    PersonalRecord(bench, LoggedSet(reps = 5, weightKg = 100.0, isCompleted = true), history[0].startedAt),
                    PersonalRecord(squat, LoggedSet(reps = 3, weightKg = 140.0, isCompleted = true), history[4].startedAt),
                ),
                latestBodyWeight = BodyWeight(today, 78.4),
                bodyWeightTrend = (11 downTo 0).map { BodyWeight(today.minusWeeks(it.toLong()), 80.2 - (11 - it) * 0.15) },
                isLoading = false,
            ),
            onOpenSettings = {},
            onStartWorkout = {},
            onStartEmptyWorkout = {},
            onCreateRoutine = {},
            onOpenRoutines = {},
            onResumeWorkout = {},
            onDiscardActiveAndStart = {},
            onDismissStartConflict = {},
            onOpenHistory = {},
            onOpenWorkoutDetail = {},
            onOpenBodyWeight = {},
            onOpenExerciseProgress = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenNewUserPreview() {
    AppTheme {
        HomeScreen(
            uiState = HomeUiState(greeting = Greeting.FirstRoutine, today = LocalDate.now(), isLoading = false),
            onOpenSettings = {},
            onStartWorkout = {},
            onStartEmptyWorkout = {},
            onCreateRoutine = {},
            onOpenRoutines = {},
            onResumeWorkout = {},
            onDiscardActiveAndStart = {},
            onDismissStartConflict = {},
            onOpenHistory = {},
            onOpenWorkoutDetail = {},
            onOpenBodyWeight = {},
            onOpenExerciseProgress = {},
        )
    }
}
