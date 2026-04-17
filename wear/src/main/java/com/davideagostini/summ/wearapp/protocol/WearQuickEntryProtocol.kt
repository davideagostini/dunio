package com.davideagostini.summ.wearapp.protocol

/**
 * Shared RPC contract used by the watch app when talking to the phone app through the Wear Data
 * Layer. The watch only needs two operations in V1: load categories and save a quick entry.
 */
internal object WearQuickEntryProtocol {
    const val PHONE_CAPABILITY = "summ_phone_app"
    const val PATH_PREFIX = "/wear/quick-entry"
    const val PATH_CATEGORIES = "$PATH_PREFIX/categories"
    const val PATH_SAVE = "$PATH_PREFIX/save"
}
