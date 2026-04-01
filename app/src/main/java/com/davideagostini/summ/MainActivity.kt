package com.davideagostini.summ

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.davideagostini.summ.tile.QuickEntryTileService
import com.davideagostini.summ.ui.navigation.AppNavGraph
import com.davideagostini.summ.ui.settings.theme.AppThemeManager
import com.davideagostini.summ.ui.settings.theme.SupportedAppTheme
import com.davideagostini.summ.ui.theme.SummTheme
import com.davideagostini.summ.ui.theme.surfaceContainerDark
import com.davideagostini.summ.ui.theme.surfaceContainerLight
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.davideagostini.summ.ui.settings.language.AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                surfaceContainerLight.toArgb(),
                surfaceContainerDark.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                surfaceContainerLight.toArgb(),
                surfaceContainerDark.toArgb(),
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val themePreference by AppThemeManager.currentTheme.collectAsStateWithLifecycle()
            val darkTheme = when (themePreference) {
                SupportedAppTheme.LIGHT -> false
                SupportedAppTheme.DARK -> true
                SupportedAppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            SummTheme(darkTheme = darkTheme) {
                AppNavGraph()
            }
        }
        requestTileAddIfNeeded()
    }

    private fun requestTileAddIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = getSharedPreferences("summ_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("tile_requested", false)) {
                val statusBarManager = getSystemService(StatusBarManager::class.java)
                statusBarManager.requestAddTileService(
                    ComponentName(this, QuickEntryTileService::class.java),
                    getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_wallet),
                    mainExecutor,
                    { }
                )
                prefs.edit { putBoolean("tile_requested", true) }
            }
        }
    }
}
