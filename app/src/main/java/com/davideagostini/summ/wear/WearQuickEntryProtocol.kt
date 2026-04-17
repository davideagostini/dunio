package com.davideagostini.summ.wear

/**
 * Phone-side copy of the Wear quick-entry RPC contract.
 *
 * The constants are duplicated in the wear module on purpose so both apps can evolve independently
 * while still speaking the same narrow protocol.
 */
internal object WearQuickEntryProtocol {
    const val PHONE_CAPABILITY = "summ_phone_app"
    const val PATH_PREFIX = "/wear/quick-entry"
    const val PATH_CATEGORIES = "$PATH_PREFIX/categories"
    const val PATH_SAVE = "$PATH_PREFIX/save"
}
