package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.ui.theme.AppTheme

/**
 * One set as a line: a [leading] marker (a tick box or the set number), the set's [value] and
 * optional [trailing] info. The workout, a finished workout and the routine editor all use it,
 * so a set looks the same everywhere.
 *
 * @param onClick opens the set; null makes the line read-only.
 * @param isHighlighted fills the line as a pill, e.g. the next set to do.
 * @param isDimmed greys the value out, e.g. a set that's already done.
 */
@Composable
fun SetRow(
    value: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false,
    isDimmed: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // Opaque, so a swipe-to-delete colour only shows where the swipe has uncovered it.
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        leading()
        val shape = if (isHighlighted) CircleShape else RoundedCornerShape(12.dp)
        val color = if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
        val bodyModifier = Modifier
            .weight(1f)
            .padding(start = 4.dp)
        val body = @Composable {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isHighlighted) FontWeight.SemiBold else null,
                    color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    modifier = Modifier.weight(1f),
                )
                trailing()
            }
        }
        if (onClick != null) {
            Surface(onClick = onClick, shape = shape, color = color, modifier = bodyModifier, content = body)
        } else {
            Surface(shape = shape, color = color, modifier = bodyModifier, content = body)
        }
    }
}

/** The set's number in a small circle, as [SetRow]'s leading marker when there's nothing to tick. */
@Composable
fun SetNumber(number: Int, modifier: Modifier = Modifier) {
    // 48dp wide like a tick box's touch target, so numbered and tickable rows line up.
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(48.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        ) {
            Text(text = number.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetRowPreview() {
    AppTheme {
        SetRow(value = "80 kg × 8", leading = { SetNumber(number = 2) }, onClick = {}, isHighlighted = true)
    }
}
