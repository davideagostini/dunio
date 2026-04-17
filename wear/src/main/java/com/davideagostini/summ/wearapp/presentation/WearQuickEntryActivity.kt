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

/**
 * Activity host for the Wear quick-entry flow.
 *
 * The Activity stays intentionally thin: theme + ViewModel wiring + screen host.
 */
class WearQuickEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DunioWearTheme {
                val viewModel: WearQuickEntryViewModel = viewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

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
