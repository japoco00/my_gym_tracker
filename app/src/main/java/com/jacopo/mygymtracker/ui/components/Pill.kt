package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/** A small rounded fact for a header, such as "12:34" or "1.9 t": an optional 18dp icon, then text. */
@Composable
fun Pill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .padding(start = 10.dp, end = 12.dp),
        content = content,
    )
}

/** A [Pill] with an [icon] before the [text]. */
@Composable
fun Pill(icon: Painter, text: String, modifier: Modifier = Modifier) {
    Pill(modifier = modifier) {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = text)
    }
}
