package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The expanded [AppHeader]'s big title style, for screens that put their own title in it. */
val Typography.headerTitle: TextStyle
    get() = displaySmall.copy(fontWeight = FontWeight.SemiBold)

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
    expandedHeight: Dp = 168.dp,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
    AppHeader(
        title = { expanded ->
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (expanded) MaterialTheme.typography.headerTitle else LocalTextStyle.current,
            )
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        pills = pills,
        collapsedSubtitle = collapsedSubtitle,
        expandedHeight = expandedHeight,
        bottomContent = bottomContent,
    )
}

/**
 * [AppHeader] with a custom [title], told whether the header is expanded, e.g. an editable name.
 *
 * @param expandedHeight the whole expanded bar, title and pills included.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: @Composable (expanded: Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    pills: @Composable RowScope.() -> Unit = {},
    collapsedSubtitle: @Composable () -> Unit = {},
    expandedHeight: Dp = 168.dp,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        ),
    ) {
        TwoRowsTopAppBar(
            title = title,
            subtitle = { expanded ->
                if (expanded) {
                    // Scrolls rather than clips when the pills outgrow a narrow screen.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 12.dp, bottom = 8.dp),
                        content = pills,
                    )
                } else {
                    collapsedSubtitle()
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = expandedHeight,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            scrollBehavior = scrollBehavior,
        )
        bottomContent()
    }
}