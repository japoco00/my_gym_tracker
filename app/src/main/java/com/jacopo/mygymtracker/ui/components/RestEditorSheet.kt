package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** "Rest 1:30" under an exercise: tonal when it has its own rest, outlined when it uses the default. */
@Composable
fun RestChip(restSeconds: Int?, defaultSeconds: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isCustom = restSeconds != null
    AssistChip(
        onClick = onClick,
        label = { Text(text = stringResource(R.string.rest_chip, formatDuration((restSeconds ?: defaultSeconds).toLong()))) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_timer),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        colors = if (isCustom) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            AssistChipDefaults.assistChipColors()
        },
        border = if (isCustom) null else AssistChipDefaults.assistChipBorder(enabled = true),
        modifier = modifier,
    )
}

/**
 * Edits the rest after each set of one exercise. `null` means the default from the settings.
 * Changes are reported as they happen, like the set editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestEditorSheet(
    exerciseName: String,
    restSeconds: Int?,
    defaultSeconds: Int,
    onRestChange: (seconds: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
        RestEditorContent(
            exerciseName = exerciseName,
            restSeconds = restSeconds,
            defaultSeconds = defaultSeconds,
            onRestChange = onRestChange,
            onDone = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestEditorContent(
    exerciseName: String,
    restSeconds: Int?,
    defaultSeconds: Int,
    onRestChange: (seconds: Int?) -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.rest_editor_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        DurationField(
            seconds = restSeconds ?: defaultSeconds,
            onSecondsChange = { onRestChange(it.coerceAtLeast(UserSettings.REST_STEP_SECONDS)) },
            label = stringResource(R.string.rest_editor_label),
            shape = ListItemDefaults.segmentedShapes(index = 0, count = 1).shape,
            modifier = Modifier.padding(top = 16.dp),
        )
        val height = ButtonDefaults.MediumContainerHeight
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            OutlinedButton(
                onClick = { onRestChange(null) },
                enabled = restSeconds != null,
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier
                    .weight(1f)
                    .height(height),
            ) {
                Text(
                    text = stringResource(R.string.rest_editor_use_default, formatDuration(defaultSeconds.toLong())),
                    style = ButtonDefaults.textStyleFor(height),
                )
            }
            Button(
                onClick = onDone,
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.height(height),
            ) {
                Text(text = stringResource(R.string.action_done), style = ButtonDefaults.textStyleFor(height))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RestEditorContentPreview() {
    AppTheme {
        Surface {
            RestEditorContent(
                exerciseName = "Squat",
                restSeconds = 180,
                defaultSeconds = 90,
                onRestChange = {},
                onDone = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RestChipPreview() {
    AppTheme {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                RestChip(restSeconds = null, defaultSeconds = 90, onClick = {})
                RestChip(restSeconds = 180, defaultSeconds = 90, onClick = {})
            }
        }
    }
}
