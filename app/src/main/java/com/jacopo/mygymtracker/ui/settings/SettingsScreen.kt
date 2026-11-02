package com.jacopo.mygymtracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.UserSettings.Companion.REST_STEP_SECONDS
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings?,
    onWeightUnitChange: (WeightUnit) -> Unit,
    onRestSecondsChange: (deltaSeconds: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        modifier = modifier,
    ) { innerPadding ->
        if (settings == null) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_weight_unit),
                style = MaterialTheme.typography.titleMedium,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                WeightUnit.entries.forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = settings.weightUnit == unit,
                        onClick = { onWeightUnitChange(unit) },
                        shape = SegmentedButtonDefaults.itemShape(index, WeightUnit.entries.size),
                        label = { Text(unit.label()) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.settings_rest_time),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.settings_rest_time_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onRestSecondsChange(-REST_STEP_SECONDS) },
                    enabled = settings.restSeconds > REST_STEP_SECONDS,
                ) {
                    Text(stringResource(R.string.rest_decrease, REST_STEP_SECONDS))
                }
                Text(
                    text = formatDuration(settings.restSeconds.toLong()),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { onRestSecondsChange(REST_STEP_SECONDS) }) {
                    Text(stringResource(R.string.rest_increase, REST_STEP_SECONDS))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            settings = UserSettings(),
            onWeightUnitChange = {},
            onRestSecondsChange = {},
        )
    }
}
