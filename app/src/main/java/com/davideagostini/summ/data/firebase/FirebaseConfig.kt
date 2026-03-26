package com.davideagostini.summ.data.firebase

import android.content.Context

object FirebaseConfig {
    fun getDefaultWebClientId(context: Context): String? {
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resourceId == 0) {
            return null
        }

        return context.resources.getString(resourceId).takeIf(String::isNotBlank)
    }
}
