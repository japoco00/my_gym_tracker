package com.jacopo.mygymtracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jacopo.mygymtracker.R

/** Consistency first: this week's training days and the streak, with the next routine below. */
class WeekWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(ROW, FULL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWidgetContent(context) { data -> WeekContent(data) }
    }

    private companion object {
        val ROW = DpSize(250.dp, 40.dp)

        /** Tall enough for the header, the week and the next routine without clipping. */
        val FULL = DpSize(250.dp, 160.dp)
    }

    @Composable
    private fun WeekContent(data: WidgetData) = WidgetSurface {
        if (LocalSize.current == ROW) {
            Row(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeekRow(data, cellSize = 24.dp, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(12.dp))
                FireIcon(GlanceTheme.colors.primary, 20.dp)
                Text(
                    text = data.weekStreak.toString(),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                )
            }
        } else {
            val context = LocalContext.current
            val next = data.next.labels(context)
            Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Headline(
                        text = context.getString(if (data.hasHistory) R.string.widget_this_week else R.string.widget_first_week),
                        fontSize = 16.sp,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    if (data.hasHistory) StreakPill(data.weekStreak)
                }
                Spacer(GlanceModifier.height(10.dp))
                WeekRow(data, cellSize = 32.dp)
                Spacer(GlanceModifier.defaultWeight())
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Overline(next.overline)
                        Headline(next.title, fontSize = 16.sp)
                    }
                    NextUpButton(next, size = 44.dp)
                }
            }
        }
    }
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}
