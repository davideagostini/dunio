package com.davideagostini.summ.widgetlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.davideagostini.summ.ui.theme.SummTheme
import com.davideagostini.summ.ui.widgetlab.WidgetLabScreen

// Debug-only activity used to inspect widget layouts without going through the
// real launcher widget flow. It is intentionally excluded from production builds.
class WidgetLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SummTheme(dynamicColor = false) {
                WidgetLabScreen(onBack = ::finish)
            }
        }
    }
}

