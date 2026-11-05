package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.formatWeight
import com.jacopo.mygymtracker.domain.model.parseWeight
import com.jacopo.mygymtracker.ui.format.formatDuration
import kotlin.math.abs
import kotlin.math.roundToInt

/** Below this difference a typed value and the stored one are the same (absorbs kg/lb round trips). */
private const val SAME_VALUE_TOLERANCE = 1e-6

@Composable
fun RepsField(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    NumberField(
        value = reps.toDouble(),
        onValueChange = { onRepsChange(it.roundToInt()) },
        format = { it.roundToInt().toString() },
        parse = { text -> text.trim().toIntOrNull()?.takeIf { it >= 0 }?.toDouble() },
        keyboardType = KeyboardType.Number,
        step = 1.0,
        label = label,
        shape = shape,
        modifier = modifier,
    )
}

/** Shows and edits [weightKg] in [unit]; always reports kilograms back. */
@Composable
fun WeightField(
    weightKg: Double,
    unit: WeightUnit,
    onWeightKgChange: (Double) -> Unit,
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    NumberField(
        value = unit.fromKg(weightKg),
        onValueChange = { onWeightKgChange(unit.toKg(it)) },
        format = ::formatWeight,
        parse = ::parseWeight,
        keyboardType = KeyboardType.Decimal,
        step = unit.step,
        label = label,
        shape = shape,
        modifier = modifier,
    )
}

/** Shows and edits [seconds] as `m:ss`; a plain number is read as seconds. */
@Composable
fun DurationField(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    NumberField(
        value = seconds.toDouble(),
        onValueChange = { onSecondsChange(it.roundToInt()) },
        format = { formatDuration(it.toLong()) },
        parse = ::parseDuration,
        keyboardType = KeyboardType.Number,
        step = DURATION_STEP_SECONDS,
        label = label,
        shape = shape,
        modifier = modifier,
    )
}

private const val DURATION_STEP_SECONDS = 15.0

/** `90`, `1:30` and `0:01:30` are all 90 seconds; `null` for anything else. */
private fun parseDuration(text: String): Double? {
    val parts = text.trim().split(':').map { part -> part.toIntOrNull()?.takeIf { it >= 0 } ?: return null }
    if (parts.size > 3) return null
    return parts.fold(0.0) { total, part -> total * 60 + part }
}

/**
 * A card with [label] and the ±[step] on top, then −, the number and +; outlined while typing.
 *
 * Keeps the typed text locally, so partial input like `22.` or an empty field is not
 * overwritten while typing. Only valid numbers are reported. The text is replaced when
 * [value] changes from outside (e.g. the ± buttons) and tidied up when focus leaves.
 */
@Composable
private fun NumberField(
    value: Double,
    onValueChange: (Double) -> Unit,
    format: (Double) -> String,
    parse: (String) -> Double?,
    keyboardType: KeyboardType,
    step: Double,
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(format(value)) }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        val typed = parse(text)
        if (typed == null || abs(typed - value) > SAME_VALUE_TOLERANCE) text = format(value)
    }

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "±${format(step)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                StepButton(onClick = { onValueChange((value - step).coerceAtLeast(0.0)) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.set_decrease, label),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { raw ->
                        text = raw
                        parse(raw)?.let(onValueChange)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        fontFeatureSettings = "tnum", // equal-width digits, so the number doesn't jiggle while stepping
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged {
                            isFocused = it.isFocused
                            if (!it.isFocused) text = format(value)
                        },
                )
                StepButton(onClick = { onValueChange(value + step) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.set_increase, label),
                    )
                }
            }
        }
    }
}

/** A 56dp round tonal button that squares its corners while pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(IconButtonDefaults.mediumRoundShape, IconButtonDefaults.mediumPressedShape),
        modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
        content = icon,
    )
}
