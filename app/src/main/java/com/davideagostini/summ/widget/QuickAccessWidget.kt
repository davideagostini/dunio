package com.davideagostini.summ.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.surfaceContainerLowDark
import com.davideagostini.summ.widget.components.quickEntryAction

// Small action-first widget that mirrors the Quick Settings tile.
// The whole surface behaves as a single tap target and launches Quick Entry.
class QuickAccessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(28.dp)
                        .background(ColorProvider(Color.White, surfaceContainerLowDark))
                        .clickable(quickEntryAction(context))
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    // Reuse the official app icon so the widget stays visually aligned
                    // with both the launcher icon and the Quick Settings tile.
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground),
                        contentDescription = context.getString(R.string.widget_quick_access_name),
                        modifier = GlanceModifier.width(72.dp).height(72.dp),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}
