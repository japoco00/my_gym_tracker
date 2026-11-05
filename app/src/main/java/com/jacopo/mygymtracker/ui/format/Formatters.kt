package com.jacopo.mygymtracker.ui.format

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.ThemeMode
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import java.time.Instant
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

/** Total volume in whole units, e.g. `4320 kg`. */
@Composable
fun WeightUnit.formatVolume(volumeKg: Double): String =
    stringResource(R.string.weight_with_unit, fromKg(volumeKg).roundToLong().toString(), label())

/** A set with its unit, e.g. `80 kg × 8`, `+10 kg × 8`, `12 reps` or `1:30`. */
@Composable
fun formatSet(
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
): String = when {
    trackingType == TrackingType.DURATION -> formatDuration(durationSeconds.toLong())
    trackingType == TrackingType.REPS && weightKg > 0.0 ->
        stringResource(R.string.set_summary_added, weightUnit.format(weightKg), weightUnit.label(), reps)
    trackingType == TrackingType.REPS -> pluralStringResource(R.plurals.reps_count, reps, reps)
    else -> stringResource(R.string.set_summary, weightUnit.format(weightKg), weightUnit.label(), reps)
}

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
