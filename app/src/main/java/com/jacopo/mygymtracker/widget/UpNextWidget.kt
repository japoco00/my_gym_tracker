package com.jacopo.mygymtracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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

/** "What now?" in one tap: starts the next routine, or resumes the running workout. */
class UpNextWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(ROW, TILE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWidgetContent(context) { data -> UpNextContent(data.next.labels(LocalContext.current)) }
    }

    private companion object {
        val ROW = DpSize(180.dp, 40.dp)
        val TILE = DpSize(110.dp, 110.dp)
    }

    @Composable
    private fun UpNextContent(next: NextUpLabels) = WidgetSurface {
        if (LocalSize.current == ROW) {
            Row(
                modifier = GlanceModifier.fillMaxSize().padding(start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NextUpButton(next, size = 48.dp, square = true)
                Spacer(GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Overline(next.overline)
                    Headline(next.title, fontSize = 22.sp)
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
                // Weighted, so a short launcher cell clips the text rather than the button.
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Overline(next.overline)
                    Spacer(GlanceModifier.height(4.dp))
                    Headline(next.title, fontSize = 22.sp, maxLines = 2)
                    Spacer(GlanceModifier.height(2.dp))
                    Detail(next.detail)
                }
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    NextUpButton(next, size = 56.dp)
                }
            }
        }
    }
}

class UpNextWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpNextWidget()
}
