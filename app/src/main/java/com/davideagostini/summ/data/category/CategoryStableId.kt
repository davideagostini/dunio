package com.davideagostini.summ.data.category

import com.davideagostini.summ.data.entity.Category
import java.util.Locale

fun Category.stableUsageId(): String =
    systemKey
        ?: id.ifBlank {
            name.trim().lowercase(Locale.ROOT)
        }
