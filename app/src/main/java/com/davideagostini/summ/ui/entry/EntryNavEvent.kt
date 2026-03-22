package com.davideagostini.summ.ui.entry

sealed class EntryNavEvent {
    data object Saved : EntryNavEvent()
}
