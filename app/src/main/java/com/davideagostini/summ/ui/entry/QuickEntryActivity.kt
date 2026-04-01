package com.davideagostini.summ.ui.entry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.davideagostini.summ.ui.settings.language.AppLanguageManager
import com.davideagostini.summ.ui.theme.SummTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickEntryActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SummTheme {
                QuickEntryScreen(
                    onDismiss = { finish() }
                )
            }
        }
    }
}
