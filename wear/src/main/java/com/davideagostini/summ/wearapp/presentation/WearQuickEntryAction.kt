/**
 * User intent actions for the Wear OS quick-entry flow.
 *
 * These sealed-interface actions represent every discrete user interaction that
 * can occur during the quick-entry wizard. The UI layer dispatches them via
 * [WearQuickEntryViewModel.onAction], and the ViewModel handles each subtype
 * to update state, trigger navigation, or start asynchronous work.
 *
 * Navigation side-effects are modelled separately in [WearNavigationEvent]
 * because they are one-shot events produced by the ViewModel (not direct user
 * intent), whereas the actions here describe input coming from the screen.
 */
package com.davideagostini.summ.wearapp.presentation

import com.davideagostini.summ.wearapp.model.WearCategory

/**
 * Sealed hierarchy of all user-driven actions in the quick-entry flow.
 *
 * Each action corresponds to exactly one UI gesture (tap, text edit, etc.)
 * and carries the minimum data required to process it.
 */
internal sealed interface WearQuickEntryAction {

    /**
     * The user selected a transaction type on the first screen.
     *
     * @property type Either "expense" or "income".
     */
    data class SelectType(val type: String) : WearQuickEntryAction

    /**
     * The user edited the amount text field.
     *
     * @property value The raw text currently displayed in the input field.
     */
    data class AmountChanged(val value: String) : WearQuickEntryAction

    /**
     * The user pressed the "Continue" button on the amount screen,
     * signalling that the entered amount is final and the flow should advance.
     */
    data object ContinueFromAmount : WearQuickEntryAction

    /**
     * The user tapped a category to associate it with the entry.
     *
     * @property category The selected [WearCategory].
     */
    data class SelectCategory(val category: WearCategory) : WearQuickEntryAction

    /**
     * The user tapped "All categories" to expand the full list
     * instead of the quick-access subset.
     */
    data object ShowAllCategories : WearQuickEntryAction

    /**
     * The user tapped "Retry" after a category-loading failure,
     * requesting another load attempt.
     */
    data object RetryCategories : WearQuickEntryAction

    /**
     * The user confirmed the entry on the review screen and wants to save it.
     * The ViewModel will attempt to forward it to the phone or queue it locally.
     */
    data object Save : WearQuickEntryAction

    /**
     * Requests an immediate refresh of the pending-entry count.
     * Typically triggered when the Activity resumes so the queue chip stays accurate.
     */
    data object RefreshPendingCount : WearQuickEntryAction
}
