package com.jacopo.mygymtracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.warmup.warmUpSets
import com.jacopo.mygymtracker.ui.components.SetMarker
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.format.setMarker
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** Previews the warm-up sets that ramp up to [targetKg], to put before an exercise's sets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmUpSheet(
    exerciseName: String,
    targetKg: Double,
    isBarbell: Boolean,
    weightUnit: WeightUnit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
        WarmUpContent(
            exerciseName = exerciseName,
            targetKg = targetKg,
            sets = warmUpSets(targetKg, isBarbell, weightUnit),
            weightUnit = weightUnit,
            onAdd = { scope.launch { sheetState.hide() }.invokeOnCompletion { onAdd() } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WarmUpContent(
    exerciseName: String,
    targetKg: Double,
    sets: List<LoggedSet>,
    weightUnit: WeightUnit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.warm_up_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                R.string.warm_up_target,
                stringResource(R.string.weight_with_unit, weightUnit.format(targetKg), weightUnit.label()),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            sets.forEachIndexed { index, set ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = sets.size),
                    leadingContent = { SetMarker(label = setMarker(number = null, type = set.type)) },
                ) {
                    Text(text = formatSet(TrackingType.WEIGHT_REPS, weightUnit, set.reps, set.weightKg, set.durationSeconds))
                }
            }
        }
        val height = ButtonDefaults.MediumContainerHeight
        Button(
            onClick = onAdd,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            Text(text = stringResource(R.string.warm_up_add), style = ButtonDefaults.textStyleFor(height))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmUpContentPreview() {
    AppTheme {
        Surface {
            WarmUpContent(
                exerciseName = "Squat",
                targetKg = 100.0,
                sets = warmUpSets(100.0, isBarbell = true, WeightUnit.KG),
                weightUnit = WeightUnit.KG,
                onAdd = {},
            )
        }
    }
}
