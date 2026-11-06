package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jacopo.mygymtracker.R

/** The "+ Add set" text button under an exercise's sets. */
@Composable
fun AddSetButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.action_add_set))
    }
}

/** The tonal "+ Add exercises" button at the end of an exercise list. */
@Composable
fun AddExercisesButton(onClick: () -> Unit, text: String = stringResource(R.string.workout_add_exercises)) {
    FilledTonalButton(onClick = onClick, shapes = ButtonDefaults.shapes()) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = text)
    }
}
