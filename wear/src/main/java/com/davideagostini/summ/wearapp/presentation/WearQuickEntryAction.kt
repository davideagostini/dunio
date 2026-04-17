package com.davideagostini.summ.wearapp.presentation

import com.davideagostini.summ.wearapp.model.WearCategory

/**
 * User-driven actions emitted by the Wear quick-entry UI.
 *
 * Navigation side effects remain separate because they are one-off effects produced by the
 * ViewModel, while these actions describe user intent coming from the screen.
 */
internal sealed interface WearQuickEntryAction {
    data class SelectType(val type: String) : WearQuickEntryAction
    data class AmountChanged(val value: String) : WearQuickEntryAction
    data object ContinueFromAmount : WearQuickEntryAction
    data class SelectCategory(val category: WearCategory) : WearQuickEntryAction
    data object ShowAllCategories : WearQuickEntryAction
    data object RetryCategories : WearQuickEntryAction
    data object Save : WearQuickEntryAction
    data object RefreshPendingCount : WearQuickEntryAction
}
