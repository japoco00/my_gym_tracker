package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The app's screen header: a rounded `surfaceContainer` block whose colour runs behind the
 * status bar. On scroll, the big [title] and the [pills] collapse into a regular top bar, which
 * shows [collapsedSubtitle] instead of the pills. [navigationIcon], [actions] and
 * [bottomContent] (a progress bar, search field, ...) stay visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    pills: @Composable RowScope.() -> Unit = {},
    collapsedSubtitle: @Composable () -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        ),
    ) {
        TwoRowsTopAppBar(
            title = { expanded ->
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (expanded) {
                        MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        LocalTextStyle.current
                    },
                )
            },
            subtitle = { expanded ->
                if (expanded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        content = pills,
                    )
                } else {
                    collapsedSubtitle()
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = 168.dp,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            scrollBehavior = scrollBehavior,
        )
        bottomContent()
    }
}
