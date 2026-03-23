package com.davideagostini.summ.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.onSurfaceDark
import com.davideagostini.summ.ui.theme.onSurfaceLight
import com.davideagostini.summ.ui.theme.surfaceContainerLowDark
import com.davideagostini.summ.ui.theme.surfaceContainerLowestLight
import com.davideagostini.summ.widget.components.quickEntryAction

// Small action-first widget that mirrors the Quick Settings tile.
// The whole surface behaves as a single tap target and launches Quick Entry.
class QuickAccessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Outer background matches the other widget containers so the launcher sees
            // a consistent card silhouette even before the user interacts with it.
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(surfaceContainerLowestLight, surfaceContainerLowDark))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Inner white card is the real interaction surface. We keep it full-size
                // so the widget reads as a compact square action instead of a tiny chip
                // floating inside a larger launcher cell.
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(28.dp)
                        .background(ColorProvider(androidx.compose.ui.graphics.Color.White, surfaceContainerLowDark))
                        .clickable(quickEntryAction(context))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reuse the official app icon so the widget stays visually aligned
                    // with both the launcher icon and the Quick Settings tile.
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground),
                        contentDescription = context.getString(R.string.widget_quick_access_name),
                        modifier = GlanceModifier.width(44.dp).height(44.dp),
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    // Short label clarifies the widget intent. Without it the square
                    // would look too similar to a launcher shortcut.
                    Text(
                        text = context.getString(R.string.widget_quick_entry_label),
                        style = TextStyle(
                            color = ColorProvider(onSurfaceLight, onSurfaceDark),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
