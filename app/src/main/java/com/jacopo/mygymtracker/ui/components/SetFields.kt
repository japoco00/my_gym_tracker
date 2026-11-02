package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

/** A centered title above a column of set values. */
@Composable
fun ColumnHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** The headers above [SetFields], in the same columns. */
@Composable
fun RowScope.SetFieldHeaders(trackingType: TrackingType, weightUnit: WeightUnit) {
    when (trackingType) {
        TrackingType.DURATION -> ColumnHeader(stringResource(R.string.set_time), Modifier.weight(2f))
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val weightHeader = if (trackingType == TrackingType.REPS) {
                stringResource(R.string.set_added_weight_short, weightUnit.label())
            } else {
                weightUnit.label()
            }
            ColumnHeader(weightHeader, Modifier.weight(1f))
            ColumnHeader(stringResource(R.string.set_reps), Modifier.weight(1f))
        }
    }
}

/** The editable values of a performed set, laid out by [trackingType]: time, or weight and reps. */
@Composable
fun RowScope.SetFields(
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    reps: Int,
    weightKg: Double,
    durationSeconds: Int,
    onRepsChange: (Int) -> Unit,
    onWeightKgChange: (Double) -> Unit,
    onDurationChange: (seconds: Int) -> Unit,
) {
    when (trackingType) {
        TrackingType.DURATION -> DurationField(
            seconds = durationSeconds,
            onSecondsChange = onDurationChange,
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 2.dp),
        )
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            WeightField(
                weightKg = weightKg,
                unit = weightUnit,
                onWeightKgChange = onWeightKgChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
            )
            RepsField(
                reps = reps,
                onRepsChange = onRepsChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetFieldsPreview() {
    AppTheme {
        Row {
            SetFields(
                trackingType = TrackingType.REPS,
                weightUnit = WeightUnit.KG,
                reps = 12,
                weightKg = 10.0,
                durationSeconds = 0,
                onRepsChange = {},
                onWeightKgChange = {},
                onDurationChange = {},
            )
        }
    }
}
