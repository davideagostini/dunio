package com.davideagostini.summ

import android.app.Application
import com.davideagostini.summ.ui.settings.language.AppLanguageManager
import com.davideagostini.summ.ui.settings.theme.AppThemeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SummApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.initialize(this)
        AppThemeManager.initialize(this)
    }
}
