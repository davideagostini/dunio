package com.davideagostini.summ.wearapp.sync

import com.davideagostini.summ.wearapp.data.WearQuickEntryRepository
import com.davideagostini.summ.wearapp.protocol.WearQuickEntryProtocol
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Background watcher for phone availability.
 *
 * As soon as a nearby phone node reconnects, this service asks the repository to flush any quick
 * entries that were queued locally while the phone was unavailable.
 */
class WearQuickEntrySyncService : WearableListenerService() {
    override fun onPeerConnected(peer: Node) {
        triggerFlush()
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        if (capabilityInfo.name == WearQuickEntryProtocol.PHONE_CAPABILITY) {
            triggerFlush()
        }
    }

    private fun triggerFlush() {
        backgroundScope.launch {
            val repository = WearQuickEntryRepository(applicationContext)
            val retryScheduleMillis = listOf(0L, 1_500L, 4_000L, 8_000L)
            retryScheduleMillis.forEachIndexed { index, delayMillis ->
                if (index > 0) {
                    delay(delayMillis)
                }
                val remaining = repository.flushPendingEntries()
                if (remaining <= 0) {
                    return@launch
                }
            }
        }
    }

    private companion object {
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
