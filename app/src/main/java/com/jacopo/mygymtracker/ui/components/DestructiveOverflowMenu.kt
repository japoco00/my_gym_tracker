package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.jacopo.mygymtracker.R

/** A ⋮ button whose menu holds one destructive [action], e.g. "Remove exercise". */
@Composable
fun DestructiveOverflowMenu(action: String, onAction: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val error = MaterialTheme.colorScheme.error
            DropdownMenuItem(
                text = { Text(action) },
                leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(textColor = error, leadingIconColor = error),
                onClick = {
                    expanded = false
                    onAction()
                },
            )
        }
    }
}
