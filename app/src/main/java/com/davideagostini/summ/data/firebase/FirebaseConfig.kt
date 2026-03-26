package com.davideagostini.summ.data.firebase

import android.content.Context
import com.davideagostini.summ.R

object FirebaseConfig {
    fun getDefaultWebClientId(context: Context): String? {
        val staticValue = runCatching {
            context.getString(R.string.default_web_client_id)
        }.getOrNull()?.takeIf(String::isNotBlank)

        if (staticValue != null) {
            return staticValue
        }

        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resourceId == 0) {
            return null
        }

        return context.resources.getString(resourceId).takeIf(String::isNotBlank)
    }
}
