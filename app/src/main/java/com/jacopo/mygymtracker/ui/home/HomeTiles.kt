package com.jacopo.mygymtracker.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.htmlEncode
import androidx.graphics.shapes.Morph
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.home.BLUE_WHALE_KG
import com.jacopo.mygymtracker.domain.home.HEATMAP_WEEKS
import com.jacopo.mygymtracker.domain.home.HomeStats
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.PersonalRecord
import com.jacopo.mygymtracker.ui.format.dayMonthFormatter
import com.jacopo.mygymtracker.ui.format.formatCompact
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.formatRelativeDay
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.format.formatWithUnit
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.format.shortDateFormatter
import java.time.Duration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** Home's stats as a grid of tiles, or a single tile promising them until the first workout. */
@Composable
internal fun HomeTiles(
    uiState: HomeUiState,
    onOpenHistory: () -> Unit,
    onOpenWorkoutDetail: (sessionId: Int) -> Unit,
    onOpenBodyWeight: () -> Unit,
    onOpenExerciseProgress: (exerciseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = uiState.stats
    if (stats == null) {
        Tile(modifier = modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_bolt), contentDescription = null)
                Text(text = stringResource(R.string.home_no_stats), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StreakTile(stats.weekStreak, onOpenHistory, Modifier.weight(1f).fillMaxHeight())
            MonthTile(stats, uiState.today, uiState.weightUnit, onOpenHistory, Modifier.weight(1f).fillMaxHeight())
        }
        HeatmapTile(stats, uiState.today, onOpenWorkoutDetail)
        VolumeTile(stats, uiState.weightUnit)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            BodyWeightTile(
                latest = uiState.latestBodyWeight,
                trend = uiState.bodyWeightTrend,
                weightUnit = uiState.weightUnit,
                onClick = onOpenBodyWeight,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            RecordsTile(
                records = uiState.records,
                weightUnit = uiState.weightUnit,
                onOpenExerciseProgress = onOpenExerciseProgress,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        FunFactsTile(stats, uiState.weightUnit)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StreakTile(weekStreak: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val morph = remember { Morph(MaterialShapes.Sunny, MaterialShapes.Cookie12Sided) }
    Tile(onClick = onClick, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = modifier) {
        TileLabel(R.drawable.ic_whatshot, stringResource(R.string.home_streak))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .size(112.dp),
        ) {
            MorphingShape(morph, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.matchParentSize())
            Text(
                text = weekStreak.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
        Text(
            text = pluralStringResource(R.plurals.home_streak_weeks, weekStreak),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MonthTile(
    stats: HomeStats,
    today: LocalDate,
    weightUnit: WeightUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tile(onClick = onClick, color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) {
        TileLabel(R.drawable.ic_event, today.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()))
        Spacer(modifier = Modifier.weight(1f))
        BigNumber(
            value = stats.monthWorkouts.toString(),
            caption = pluralStringResource(R.plurals.home_month_workouts, stats.monthWorkouts),
        )
        BigNumber(
            value = formatCompact(weightUnit.fromKg(stats.monthVolumeKg)),
            caption = stringResource(R.string.home_lifted, weightUnit.label()),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** One cell per day of the last [HEATMAP_WEEKS] weeks, a column per week; trained days open their workout. */
@Composable
private fun HeatmapTile(stats: HomeStats, today: LocalDate, onOpenWorkoutDetail: (sessionId: Int) -> Unit) {
    val locale = Locale.getDefault()
    val colors = MaterialTheme.colorScheme
    Tile {
        TileLabel(R.drawable.ic_event, stringResource(R.string.home_consistency))
        Caption(
            pluralStringResource(
                R.plurals.home_heatmap_caption,
                stats.heatmapWorkouts,
                stats.heatmapWorkouts,
                HEATMAP_WEEKS,
            ),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            repeat(HEATMAP_WEEKS) { week ->
                val weekStart = stats.heatmapStart.plusWeeks(week.toLong())
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    // Labels the week holding the 1st, so labels are at least four weeks apart and never overlap.
                    val newMonth = weekStart.plusDays(6).takeIf { it.dayOfMonth <= 7 }?.month
                    Text(
                        text = newMonth?.getDisplayName(TextStyle.SHORT_STANDALONE, locale).orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                    )
                    repeat(7) { dayIndex ->
                        val day = weekStart.plusDays(dayIndex.toLong())
                        val workoutId = stats.trainedDays[day]
                        val description = formatDate(day)
                        val cell = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .then(if (day == today) Modifier.todayRing(colors.primary) else Modifier)
                        when {
                            workoutId != null -> Box(
                                cell
                                    .clip(CircleShape)
                                    .background(colors.primary)
                                    .clickable { onOpenWorkoutDetail(workoutId) }
                                    .semantics { contentDescription = description },
                            )
                            day > today -> Spacer(cell)
                            else -> Box(cell.background(colors.surfaceContainerHighest, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
    }
}

/** A ring just outside the cell, like an outline with a small offset. */
private fun Modifier.todayRing(color: Color) = drawBehind {
    val width = 2.dp.toPx()
    drawCircle(color, radius = size.minDimension / 2 + 1.dp.toPx() + width / 2, style = Stroke(width))
}

@Composable
private fun VolumeTile(stats: HomeStats, weightUnit: WeightUnit) {
    val max = stats.weeklyVolume.maxOf { it.volumeKg }.takeIf { it > 0.0 } ?: 1.0
    val dayMonth = remember { dayMonthFormatter() }
    Tile {
        TileLabel(R.drawable.ic_show_chart, stringResource(R.string.home_weekly_volume))
        Caption(stringResource(R.string.home_volume_caption, weightUnit.label()))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            stats.weeklyVolume.forEachIndexed { index, week ->
                val isNow = index == stats.weeklyVolume.lastIndex
                val color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    VolumeLabel(formatCompact(weightUnit.fromKg(week.volumeKg)), color, isNow)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((92.dp * (week.volumeKg / max).toFloat()).coerceAtLeast(4.dp))
                            .background(
                                if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp),
                            ),
                    )
                    VolumeLabel(
                        text = if (isNow) stringResource(R.string.home_volume_now) else week.start.format(dayMonth),
                        color = color,
                        bold = isNow,
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeLabel(text: String, color: Color, bold: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.Bold else null,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun BodyWeightTile(
    latest: BodyWeight?,
    trend: List<BodyWeight>,
    weightUnit: WeightUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tile(onClick = onClick, modifier = modifier) {
        TileLabel(R.drawable.ic_show_chart, stringResource(R.string.body_weight_title))
        Spacer(modifier = Modifier.weight(1f))
        if (latest == null) {
            Caption(stringResource(R.string.body_weight_hint))
            return@Tile
        }
        BigNumber(value = weightUnit.format(latest.weightKg), caption = weightUnit.label())
        if (trend.size < 2) return@Tile
        val delta = latest.weightKg - trend.first().weightKg
        val sign = if (delta < 0) "−" else "+"
        Caption(
            stringResource(
                R.string.home_body_weight_since,
                sign + weightUnit.formatWithUnit(abs(delta)),
                shortDateFormatter().format(trend.first().date),
            ),
        )
        Sparkline(
            trend = trend,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(40.dp),
        )
    }
}

/** [trend]'s weights over time, at least two of them, ending in a dot on the latest. */
@Composable
private fun Sparkline(trend: List<BodyWeight>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val inset = 4.dp.toPx()
        val firstDay = trend.first().date.toEpochDay()
        val days = (trend.last().date.toEpochDay() - firstDay).toFloat()
        val lowest = trend.minOf { it.weightKg }
        val range = (trend.maxOf { it.weightKg } - lowest).takeIf { it > 0.0 } ?: 1.0
        fun BodyWeight.point() = Offset(
            x = inset + (date.toEpochDay() - firstDay) / days * (size.width - 2 * inset),
            y = size.height - inset - ((weightKg - lowest) / range).toFloat() * (size.height - 2 * inset),
        )
        val path = Path()
        trend.forEachIndexed { index, weight ->
            val point = weight.point()
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = inset, center = trend.last().point())
    }
}

@Composable
private fun RecordsTile(
    records: List<PersonalRecord>,
    weightUnit: WeightUnit,
    onOpenExerciseProgress: (exerciseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Tile(modifier = modifier) {
        TileLabel(R.drawable.ic_emoji_events, stringResource(R.string.home_records))
        if (records.isEmpty()) {
            Caption(stringResource(R.string.home_records_empty), Modifier.padding(top = 8.dp))
        }
        records.forEach { record ->
            val set = record.set
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { record.exercise.id?.let(onOpenExerciseProgress) }
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = record.exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.home_record_summary,
                        formatSet(record.exercise.trackingType, weightUnit, set.reps, set.weightKg, set.durationSeconds),
                        formatRelativeDay(record.achievedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FunFactsTile(stats: HomeStats, weightUnit: WeightUnit) {
    val locale = Locale.getDefault()
    val longest = stats.longestWorkout
    val minutes = Duration.between(longest.startedAt, longest.finishedAt).toMinutes()
    Tile {
        TileLabel(R.drawable.ic_bolt, stringResource(R.string.home_fun_facts))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            FactChip(
                stringResource(
                    R.string.home_fact_favourite_day,
                    stats.favouriteDay.getDisplayName(TextStyle.FULL, locale).htmlEncode(),
                ),
            )
            FactChip(
                stringResource(
                    R.string.home_fact_longest,
                    longest.routineName.htmlEncode(),
                    stringResource(R.string.home_minutes, minutes),
                ),
            )
            FactChip(
                stringResource(
                    R.string.home_fact_whales,
                    weightUnit.formatVolume(stats.totalVolumeKg).htmlEncode(),
                    String.format(locale, "%.1f", stats.totalVolumeKg / BLUE_WHALE_KG),
                ),
            )
            FactChip(
                pluralStringResource(
                    R.plurals.home_fact_workouts_since,
                    stats.totalWorkouts,
                    stats.totalWorkouts,
                    formatDate(stats.firstWorkoutDay).htmlEncode(),
                ),
            )
        }
    }
}

@Composable
private fun FactChip(html: String) {
    Text(
        text = rememberHighlighted(html, MaterialTheme.colorScheme.primary),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** A rounded tile of the grid; clickable if [onClick] is set. */
@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
    if (onClick == null) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = color, modifier = modifier, content = body)
    } else {
        Surface(onClick = onClick, shape = MaterialTheme.shapes.extraLarge, color = color, modifier = modifier, content = body)
    }
}

@Composable
private fun TileLabel(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BigNumber(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Text(text = caption, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** [html]'s text with its `<b>` words extra bold in [color]. Encode anything inserted into [html] first. */
@Composable
internal fun rememberHighlighted(html: String, color: Color): AnnotatedString = remember(html, color) {
    val parsed = AnnotatedString.fromHtml(html)
    buildAnnotatedString {
        append(parsed.text)
        parsed.spanStyles.forEach {
            addStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold), it.start, it.end)
        }
    }
}

/** [morph] slowly going back and forth between its shapes while turning, as big as fits in this space. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MorphingShape(morph: Morph, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "morphingShape")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "morph",
    )
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "turn",
    )
    Spacer(
        modifier = modifier.drawWithCache {
            val side = size.minDimension
            val path = morph.toPath(progress) // normalized to a 1 x 1 square
            onDrawBehind {
                rotate(turn) {
                    translate((size.width - side) / 2, (size.height - side) / 2) {
                        scale(side, pivot = Offset.Zero) { drawPath(path, color) }
                    }
                }
            }
        },
    )
}
