package com.davideagostini.summ.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Returns the correct corner shape for a list item at [index] out of [count] total items. */
fun listItemShape(index: Int, count: Int): RoundedCornerShape = when {
    count == 1       -> RoundedCornerShape(16.dp)
    index == 0       -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == count-1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else             -> RoundedCornerShape(4.dp)
}
