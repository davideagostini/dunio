package com.davideagostini.summ.ui.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EmojiSection(val name: String, val emojis: List<String>)

private val EMOJI_SECTIONS = listOf(
    EmojiSection("Finance",     listOf("💰","💵","💴","💶","💷","💳","💸","📊","📈","📉","🏦","💹","🏧","💱","🪙","🤑")),
    EmojiSection("Food",        listOf("🍕","🍔","🍟","🌮","🍜","🍣","🥗","🥩","🍳","☕","🧃","🍺","🍷","🍰","🧁","🍫")),
    EmojiSection("Transport",   listOf("🚗","🚕","🚌","🚂","✈️","🚢","🏍️","🚲","🛵","⛽","🅿️","🚦","🛻","🚐","🛳️","🚁")),
    EmojiSection("Home",        listOf("🏠","🏡","🛋️","🔑","💡","🧹","🔧","🪴","🛁","🚿","🧺","🪣","🛒","🪟","🛏️","🍳")),
    EmojiSection("Health",      listOf("💊","🏥","🩺","🏃","🧘","🏋️","🦷","👓","🩹","❤️","🧬","💉","🩻","🧪","🫀","🧠")),
    EmojiSection("Leisure",     listOf("🎮","🎬","🎵","📚","🎨","⚽","🏊","🚴","🎭","🎯","🎲","🏖️","🎪","🎢","🎸","🎤")),
    EmojiSection("Work",        listOf("💼","💻","📱","🖥️","📝","📋","⌨️","🗂️","📌","🖊️","📎","🔍","📞","🖨️","📡","🔬")),
    EmojiSection("Shopping",    listOf("🛍️","👗","👟","💄","💎","👜","🧴","📦","👒","🧸","🎀","👠","🕶️","⌚","💍","🪮")),
    EmojiSection("Other",       listOf("📦","🎁","🌍","🌟","🔥","⚡","🌈","🎉","🏆","🎖️","🌺","🍀","⭐","🌙","☀️","🌊")),
)

@Composable
internal fun EmojiPickerGrid(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns  = GridCells.Fixed(6),
        modifier = modifier.fillMaxWidth(),
    ) {
        EMOJI_SECTIONS.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text       = section.name,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 2.dp),
                )
            }
            items(section.emojis) { emoji ->
                EmojiCell(
                    emoji    = emoji,
                    selected = emoji == selectedEmoji,
                    onClick  = { onEmojiSelected(emoji) },
                )
            }
        }
    }
}

@Composable
private fun EmojiCell(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}
