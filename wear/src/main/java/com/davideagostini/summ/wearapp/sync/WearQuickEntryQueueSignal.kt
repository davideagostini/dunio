package com.davideagostini.summ.wearapp.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small in-process signal for queue-size changes.
 *
 * The watch sync service and the foreground UI run in the same app process, but relying only on
 * SharedPreferences listeners has proven flaky for immediate UI updates. This signal gives the
 * ViewModel a deterministic stream every time the repository mutates the queue.
 */
internal object WearQuickEntryQueueSignal {
    private val _pendingCount = MutableStateFlow<Int?>(null)
    val pendingCount: StateFlow<Int?> = _pendingCount.asStateFlow()

    fun publish(count: Int) {
        _pendingCount.value = count
    }
}
