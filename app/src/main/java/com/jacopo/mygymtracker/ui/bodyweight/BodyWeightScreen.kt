package com.jacopo.mygymtracker.ui.bodyweight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.BackButton
import com.jacopo.mygymtracker.ui.components.DateLineChart
import com.jacopo.mygymtracker.ui.components.EmptyStateText
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.SectionLabel
import com.jacopo.mygymtracker.ui.components.ShapeBadge
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.formatWithUnit
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightScreen(
    uiState: BodyWeightUiState,
    onBack: () -> Unit,
    onLogWeight: () -> Unit,
    onEntryClick: (BodyWeight) -> Unit,
    onDeleteEntry: (LocalDate) -> Unit,
    onSave: (date: LocalDate, weightKg: Double) -> Unit,
    onDismissEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.editor?.let { editor ->
        BodyWeightSheet(
            entry = editor.entry,
            initialWeightKg = uiState.latest?.weightKg ?: 0.0,
            today = uiState.today,
            weightUnit = uiState.weightUnit,
            onSave = onSave,
            onDismiss = onDismissEditor,
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = { BodyWeightHeader(uiState = uiState, scrollBehavior = scrollBehavior, onBack = onBack) },
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                onClick = onLogWeight,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.body_weight_log)) },
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        when {
            uiState.isLoading -> Unit
            uiState.entries.isEmpty() -> EmptyStateText(
                text = stringResource(R.string.body_weight_empty),
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 28.dp, vertical = 16.dp),
            )
            else -> BodyWeightContent(
                uiState = uiState,
                onEntryClick = onEntryClick,
                onDeleteEntry = onDeleteEntry,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** The title over the entry count, with the latest weight in a sunny badge. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BodyWeightHeader(
    uiState: BodyWeightUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
) {
    val entryCount = pluralStringResource(R.plurals.body_weight_entry_count, uiState.entries.size, uiState.entries.size)
    AppHeader(
        title = stringResource(R.string.body_weight_title),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            BackButton(onClick = onBack)
        },
        actions = {
            uiState.latest?.let { latest ->
                ShapeBadge(
                    shape = MaterialShapes.Sunny,
                    value = uiState.weightUnit.format(latest.weightKg),
                    label = uiState.weightUnit.label(),
                    size = 64.dp,
                    contentDescription = stringResource(
                        R.string.body_weight_latest_description,
                        uiState.weightUnit.formatWithUnit(latest.weightKg),
                    ),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        },
        pills = {
            if (!uiState.isLoading) {
                Pill(icon = painterResource(R.drawable.ic_history), text = entryCount)
            }
        },
        collapsedSubtitle = { Text(text = entryCount) },
    )
}

@Composable
private fun BodyWeightContent(
    uiState: BodyWeightUiState,
    onEntryClick: (BodyWeight) -> Unit,
    onDeleteEntry: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = uiState.entries.asReversed()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        // The bottom inset keeps the last row clear of the medium FAB.
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "chart") {
            BodyWeightChart(
                entries = uiState.entries,
                weightUnit = uiState.weightUnit,
                modifier = Modifier.padding(bottom = 22.dp),
            )
        }
        item(key = "entries") {
            SectionLabel(text = stringResource(R.string.body_weight_entries))
        }
        itemsIndexed(items = entries, key = { _, entry -> entry.date.toEpochDay() }) { index, entry ->
            val shapes = ListItemDefaults.segmentedShapes(index = index, count = entries.size)
            SwipeToDeleteContainer(
                onDelete = { onDeleteEntry(entry.date) },
                // Clipped so the red swipe background follows the row's rounded corners.
                modifier = Modifier
                    .animateItem()
                    .clip(shapes.shape),
            ) {
                SegmentedListItem(
                    onClick = { onEntryClick(entry) },
                    shapes = shapes,
                    overlineContent = { Text(text = formatDate(entry.date)) },
                ) {
                    Text(text = uiState.weightUnit.formatWithUnit(entry.weightKg), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** [entries], oldest first, as a line in [weightUnit]. */
@Composable
private fun BodyWeightChart(entries: List<BodyWeight>, weightUnit: WeightUnit, modifier: Modifier = Modifier) {
    DateLineChart(
        values = remember(entries, weightUnit) { entries.map { weightUnit.fromKg(it.weightKg) } },
        dates = remember(entries) { entries.map(BodyWeight::date) },
        highlightIndex = -1,
        formatsDuration = false,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .padding(16.dp)
            .height(200.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun BodyWeightScreenPreview() {
    val today = LocalDate.of(2025, 7, 14)
    AppTheme {
        BodyWeightScreen(
            uiState = BodyWeightUiState(
                entries = listOf(80.2, 79.6, 79.9, 78.8, 78.4).mapIndexed { index, weightKg ->
                    BodyWeight(date = today.minusDays(7L * (4 - index)), weightKg = weightKg)
                },
                today = today,
                isLoading = false,
            ),
            onBack = {},
            onLogWeight = {},
            onEntryClick = {},
            onDeleteEntry = {},
            onSave = { _, _ -> },
            onDismissEditor = {},
        )
    }
}
