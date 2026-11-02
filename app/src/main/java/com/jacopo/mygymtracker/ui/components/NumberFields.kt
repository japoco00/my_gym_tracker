package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.formatWeight
import com.jacopo.mygymtracker.domain.model.parseWeight
import kotlin.math.abs
import kotlin.math.roundToInt

/** Below this difference a typed value and the stored one are the same (absorbs kg/lb round trips). */
private const val SAME_VALUE_TOLERANCE = 1e-6

@Composable
fun RepsField(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showStepper: Boolean = false,
) {
    NumberField(
        value = reps.toDouble(),
        onValueChange = { onRepsChange(it.roundToInt()) },
        format = { it.roundToInt().toString() },
        parse = { text -> text.trim().toIntOrNull()?.takeIf { it >= 0 }?.toDouble() },
        keyboardType = KeyboardType.Number,
        step = if (showStepper) 1.0 else null,
        modifier = modifier,
    )
}

/** Shows and edits [weightKg] in [unit]; always reports kilograms back. */
@Composable
fun WeightField(
    weightKg: Double,
    unit: WeightUnit,
    onWeightKgChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    showStepper: Boolean = false,
) {
    NumberField(
        value = unit.fromKg(weightKg),
        onValueChange = { onWeightKgChange(unit.toKg(it)) },
        format = ::formatWeight,
        parse = ::parseWeight,
        keyboardType = KeyboardType.Decimal,
        step = if (showStepper) unit.step else null,
        modifier = modifier,
    )
}

/**
 * Keeps the typed text locally, so partial input like `22.` or an empty field is not
 * overwritten while typing. Only valid numbers are reported. The text is replaced when
 * [value] changes from outside (e.g. a stepper) and tidied up when focus leaves.
 */
@Composable
private fun NumberField(
    value: Double,
    onValueChange: (Double) -> Unit,
    format: (Double) -> String,
    parse: (String) -> Double?,
    keyboardType: KeyboardType,
    step: Double?,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(format(value)) }
    LaunchedEffect(value) {
        val typed = parse(text)
        if (typed == null || abs(typed - value) > SAME_VALUE_TOLERANCE) text = format(value)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (step != null) {
            StepButton(label = "−${formatWeight(step)}") {
                onValueChange((value - step).coerceAtLeast(0.0))
            }
        }
        TextField(
            value = text,
            onValueChange = { raw ->
                text = raw
                parse(raw)?.let(onValueChange)
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (!it.isFocused) text = format(value) },
        )
        if (step != null) {
            StepButton(label = "+${formatWeight(step)}") { onValueChange(value + step) }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.padding(2.dp),
    ) {
        Text(text = label)
    }
}
