package com.jacopo.mygymtracker.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.home.HomeSituation
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle

/** Shows [content] with the latest [WidgetData], in the system's widget colours. */
suspend fun GlanceAppWidget.provideWidgetContent(context: Context, content: @Composable (WidgetData) -> Unit): Nothing {
    val data = context.observeWidgetData()
    val initial = data.first()
    // Keeps a live widget session current; a new session reads the data again anyway.
    provideContent {
        val current by data.collectAsState(initial)
        GlanceTheme { content(current) }
    }
}

/** [NextUp] in words, plus what its button does. */
class NextUpLabels(
    val overline: String,
    val title: String,
    val detail: String,
    @DrawableRes val icon: Int,
    val actionLabel: String,
    val request: WidgetRequest,
    /** Softer button for when training is already done today. */
    val tonal: Boolean = false,
)

fun NextUp.labels(context: Context): NextUpLabels = when (this) {
    is NextUp.Resume -> NextUpLabels(
        overline = context.getString(R.string.workout_in_progress),
        title = routineName,
        detail = context.resources.getQuantityString(R.plurals.workout_sets_progress, totalSets, completedSets, totalSets),
        icon = R.drawable.ic_play_arrow,
        actionLabel = context.getString(R.string.routine_resume, routineName),
        request = WidgetRequest.Resume(sessionId),
    )
    is NextUp.Start -> NextUpLabels(
        overline = when (situation) {
            HomeSituation.TrainedToday -> context.getString(R.string.widget_done_today)
            is HomeSituation.Away -> context.resources.getQuantityString(R.plurals.widget_days_off, situation.days, situation.days)
            else -> context.getString(R.string.widget_up_next)
        },
        title = routineName,
        detail = context.resources.getQuantityString(R.plurals.routine_exercise_count, exerciseCount, exerciseCount),
        icon = R.drawable.ic_play_arrow,
        actionLabel = context.getString(R.string.routine_start, routineName),
        request = WidgetRequest.Start(routineId),
        tonal = situation == HomeSituation.TrainedToday,
    )
    NextUp.CreateRoutine -> NextUpLabels(
        overline = context.getString(R.string.widget_no_routine),
        title = context.getString(R.string.widget_build_routine),
        detail = context.getString(R.string.widget_build_routine_hint),
        icon = R.drawable.ic_add,
        actionLabel = context.getString(R.string.routines_create),
        request = WidgetRequest.CreateRoutine,
    )
}

/** The widget's background; tapping it opens the app as it was. */
@Composable
fun WidgetSurface(content: @Composable () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        modifier = GlanceModifier.clickable(actionStartActivity(context.widgetIntent())),
        horizontalPadding = 0.dp,
        content = content,
    )
}

@Composable
fun Overline(text: String) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}

@Composable
fun Headline(text: String, fontSize: TextUnit, maxLines: Int = 1, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = fontSize, fontWeight = FontWeight.Bold),
        maxLines = maxLines,
    )
}

@Composable
fun Detail(text: String) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        maxLines = 1,
    )
}

/** Starts, resumes or creates, as [next] says. */
@Composable
fun NextUpButton(next: NextUpLabels, size: Dp, square: Boolean = false) {
    val onClick = actionStartActivity(LocalContext.current.widgetIntent(next.request))
    val colors = GlanceTheme.colors
    val background = if (next.tonal) colors.secondaryContainer else colors.primary
    val content = if (next.tonal) colors.onSecondaryContainer else colors.onPrimary
    val icon = ImageProvider(next.icon)
    val modifier = GlanceModifier.size(size)
    if (square) {
        SquareIconButton(icon, next.actionLabel, onClick, modifier, backgroundColor = background, contentColor = content)
    } else {
        CircleIconButton(icon, next.actionLabel, onClick, modifier, backgroundColor = background, contentColor = content)
    }
}

/** This week's days: a cookie for each one trained, a ring around today. */
@Composable
fun WeekRow(data: WidgetData, cellSize: Dp, modifier: GlanceModifier = GlanceModifier.fillMaxWidth()) {
    Row(modifier = modifier) {
        for (offset in 0L..6L) {
            DayCell(data.weekStart.plusDays(offset), data, cellSize, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, data: WidgetData, size: Dp, modifier: GlanceModifier) {
    val colors = GlanceTheme.colors
    val locale = LocalContext.current.resources.configuration.locales[0]
    val trained = day in data.trainedDays
    val future = day > data.today
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.dayOfWeek.getDisplayName(DateTextStyle.NARROW, locale),
            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.height(4.dp))
        Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
            when {
                trained -> DayShape(R.drawable.widget_day_cookie, colors.primary, size)
                future -> Unit
                else -> {
                    DayShape(R.drawable.widget_day_circle, colors.surfaceVariant, size)
                    if (day == data.today) DayShape(R.drawable.widget_day_ring, colors.primary, size)
                }
            }
            Text(
                text = day.dayOfMonth.toString(),
                style = TextStyle(
                    color = when {
                        trained -> colors.onPrimary
                        future -> colors.outline
                        else -> colors.onSurface
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun DayShape(@DrawableRes shape: Int, color: ColorProvider, size: Dp) {
    Image(
        provider = ImageProvider(shape),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(color),
    )
}

/** E.g. "🔥 3-week streak". */
@Composable
fun StreakPill(weeks: Int) {
    val colors = GlanceTheme.colors
    Row(
        modifier = GlanceModifier
            .height(28.dp)
            .background(colors.secondaryContainer)
            .cornerRadius(14.dp)
            .padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FireIcon(colors.onSecondaryContainer, 16.dp)
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = LocalContext.current.resources.getQuantityString(R.plurals.history_week_streak, weeks, weeks),
            style = TextStyle(color = colors.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
fun FireIcon(color: ColorProvider, size: Dp) {
    Image(
        provider = ImageProvider(R.drawable.ic_whatshot),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(color),
    )
}
