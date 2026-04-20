/**
 * Entry-point Activity for the Wear OS quick-entry module.
 *
 * This Activity is declared as the LAUNCHER activity in the manifest so it
 * opens when the user taps the app icon on the watch. It is intentionally
 * thin and delegates all logic to the ViewModel and composable screen:
 *
 * 1. **Theme** – wraps the entire content in [DunioWearTheme] to apply the
 *    dark colour scheme optimised for round Wear OS displays.
 * 2. **ViewModel** – obtains a [WearQuickEntryViewModel] via the default
 *    ViewModelProvider.Factory (the ViewModel takes an Application parameter).
 * 3. **State collection** – collects the UI state as a lifecycle-aware
 *    Compose state so the screen recomposes only while the Activity is started.
 * 4. **Pending-count refresh** – uses [LifecycleResumeEffect] to refresh the
 *    pending-entry count every time the Activity comes to the foreground,
 *    ensuring the queue chip is accurate even after the phone has consumed
 *    entries while the watch was in the background.
 * 5. **Screen host** – renders [WearQuickEntryScreen] which contains the
 *    Wear Navigation framework and all step composables.
 */
package com.davideagostini.summ.wearapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.davideagostini.summ.wearapp.navigation.WearQuickEntryScreen
import com.davideagostini.summ.wearapp.theme.DunioWearTheme

class WearQuickEntryActivity : ComponentActivity() {

    /**
     * Called when the Activity is first created.
     *
     * Enables edge-to-edge rendering (no system bars on Wear OS) and sets
     * the Compose content tree. The content block is the only place where
     * the ViewModel, state, and navigation are wired together.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DunioWearTheme {
                val viewModel: WearQuickEntryViewModel = viewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

                /**
                 * Refreshes the pending-entry count every time the Activity
                 * resumes. This is necessary because the phone app may have
                 * consumed queued entries while the watch was in the background,
                 * and the Data Layer listener might have missed the event.
                 */
                LifecycleResumeEffect(Unit) {
                    viewModel.onAction(WearQuickEntryAction.RefreshPendingCount)
                    onPauseOrDispose { }
                }

                WearQuickEntryScreen(
                    uiState = uiState,
                    pendingCountFlow = viewModel.pendingCount,
                    formattedAmount = viewModel.formattedAmount(),
                    onAction = viewModel::onAction,
                    navigationEvents = viewModel.navigateToRoute,
                    onSyncRoute = viewModel::syncFromNavigation,
                )
            }
        }
    }
}
