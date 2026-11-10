package com.jacopo.mygymtracker.ui.format

import android.icu.text.CompactDecimalFormat
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.ThemeMode
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.formatWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

/** `m:ss`, or `h:mm:ss` from one hour on. */
fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, secs)
    }
}

/** Weekday, date and time in the device's locale and time zone, e.g. `Mon, Jul 14 · 6:30 PM`. */
fun formatWeekdayTime(instant: Instant): String {
    val locale = Locale.getDefault()
    val dateTime = instant.atZone(ZoneId.systemDefault())
    val date = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale)
    return "${date.format(dateTime)} · ${DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(dateTime)}"
}

/** Date in the device's locale and time zone, e.g. `Jul 14, 2025`. */
fun formatDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(instant.atZone(ZoneId.systemDefault()))

/** [date] in the device's locale, e.g. `Jul 14, 2025`. */
fun formatDate(date: LocalDate): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)

/** `Today`, `Yesterday`, `3 days ago`, then a short date from one week on. */
fun formatRelativeDay(instant: Instant): String =
    DateUtils.getRelativeTimeSpanString(
        instant.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
    ).toString()

/** Day and abbreviated month in the device's locale and time zone, e.g. `Jul 14` or `14 Jul`. */
fun shortDateFormatter(): DateTimeFormatter {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
        .withZone(ZoneId.systemDefault())
}

/** Numeric day and month in the device's locale, e.g. `14/7` or `7/14`. */
fun dayMonthFormatter(): DateTimeFormatter {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dM"), locale)
}

/** A short number in the device's locale, e.g. `4.3K`. */
fun formatCompact(value: Double): String =
    CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(value)

/** A weight with its unit, e.g. `78.4 kg`. */
@Composable
fun WeightUnit.formatWithUnit(weightKg: Double): String =
    stringResource(R.string.weight_with_unit, format(weightKg), label())

/** Total volume in whole units, e.g. `4320 kg`. */
@Composable
fun WeightUnit.formatVolume(volumeKg: Double): String =
    stringResource(R.string.weight_with_unit, fromKg(volumeKg).roundToLong().toString(), label())

/** A set with its unit, e.g. `80 kg × 8`, `+10 kg × 8`, `12 reps` or `1:30`, plus ` · RPE 8.5` if [rpe] is set. */
@Composable
fun formatSet(
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
    rpe: Double? = null,
): String {
    val summary = when {
        trackingType == TrackingType.DURATION -> formatDuration(durationSeconds.toLong())
        trackingType == TrackingType.REPS && weightKg > 0.0 ->
            stringResource(R.string.set_summary_added, weightUnit.format(weightKg), weightUnit.label(), reps)
        trackingType == TrackingType.REPS -> pluralStringResource(R.plurals.reps_count, reps, reps)
        else -> stringResource(R.string.set_summary, weightUnit.format(weightKg), weightUnit.label(), reps)
    }
    return if (rpe == null) summary else stringResource(R.string.set_with_rpe, summary, formatWeight(rpe))
}

/** A set's leading marker: its [number] if it's a normal set, else its type's letter, e.g. `W`. */
@Composable
fun setMarker(number: Int?, type: SetType): String = when (type) {
    SetType.NORMAL -> number?.toString().orEmpty()
    SetType.WARM_UP -> stringResource(R.string.set_type_warm_up_short)
    SetType.DROP -> stringResource(R.string.set_type_drop_short)
    SetType.FAILURE -> stringResource(R.string.set_type_failure_short)
}

/** A set's name: `Set 2` if it's a normal set, else its type, e.g. `Warm-up`. */
@Composable
fun setName(number: Int?, type: SetType): String =
    if (type == SetType.NORMAL && number != null) stringResource(R.string.set_editor_title, number) else type.label()

@Composable
fun SetType.label(): String = stringResource(
    when (this) {
        SetType.NORMAL -> R.string.set_type_normal
        SetType.WARM_UP -> R.string.set_type_warm_up
        SetType.DROP -> R.string.set_type_drop
        SetType.FAILURE -> R.string.set_type_failure
    },
)

@Composable
fun WeightUnit.label(): String = stringResource(
    when (this) {
        WeightUnit.KG -> R.string.unit_kg
        WeightUnit.LB -> R.string.unit_lb
    },
)

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
fun TrackingType.label(): String = stringResource(
    when (this) {
        TrackingType.WEIGHT_REPS -> R.string.tracking_type_weight_reps
        TrackingType.REPS -> R.string.tracking_type_reps
        TrackingType.DURATION -> R.string.tracking_type_duration
    },
)

@Composable
fun Category.label(): String = stringResource(
    when (this) {
        Category.CHEST -> R.string.category_chest
        Category.BACK -> R.string.category_back
        Category.SHOULDERS -> R.string.category_shoulders
        Category.BICEPS -> R.string.category_biceps
        Category.TRICEPS -> R.string.category_triceps
        Category.LEGS -> R.string.category_legs
        Category.GLUTES -> R.string.category_glutes
        Category.CORE -> R.string.category_core
        Category.FULL_BODY -> R.string.category_full_body
        Category.CARDIO -> R.string.category_cardio
        Category.OTHER -> R.string.category_other
    },
)
