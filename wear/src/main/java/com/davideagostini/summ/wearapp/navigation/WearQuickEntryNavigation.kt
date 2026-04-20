/**
 * Navigation infrastructure for the Wear OS quick-entry flow.
 *
 * This file defines the route constants, navigation event types, and the
 * main screen composable that hosts the Wear Navigation framework.
 *
 * The navigation graph is linear and shallow (Type → Amount → Category →
 * Confirm → Success), which maps well to Wear OS's swipe-to-go-back gesture.
 * The same [CategoryStep] composable is reused for both the quick-category
 * subset and the full-category list, distinguished by the route.
 *
 * Key types:
 * - [WearQuickEntryRoute]: string constants for each navigation destination.
 * - [WearNavigationEvent]: ViewModel-driven navigation commands (push or reset).
 * - [WearQuickEntryScreen]: the root composable that wires the NavHost.
 */
package com.davideagostini.summ.wearapp.navigation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryUiState
import com.davideagostini.summ.wearapp.ui.steps.AmountStep
import com.davideagostini.summ.wearapp.ui.steps.CategoryStep
import com.davideagostini.summ.wearapp.ui.steps.ConfirmStep
import com.davideagostini.summ.wearapp.ui.steps.SuccessStep
import com.davideagostini.summ.wearapp.ui.steps.TypeStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * String constants for each navigation destination in the quick-entry wizard.
 *
 * These are used as route identifiers in the Wear Navigation [SwipeDismissableNavHost]
 * and as keys by the ViewModel to synchronise its internal step enum with the
 * back-stack state.
 */
internal object WearQuickEntryRoute {
    /** First screen: choose between expense and income. */
    const val Type = "type"
    /** Second screen: enter the monetary amount. */
    const val Amount = "amount"
    /** Third screen: choose a category (quick-access subset). */
    const val Category = "category"
    /** Third screen variant: browse the full category list. */
    const val AllCategories = "all_categories"
    /** Fourth screen: review and confirm the entry before saving. */
    const val Confirm = "confirm"
    /** Final screen: success confirmation with auto-dismiss. */
    const val Success = "success"
}

/**
 * Sealed navigation commands produced by the ViewModel and consumed by
 * the [WearQuickEntryScreen] composable.
 *
 * Using a Channel-based event flow (instead of driving navigation directly
 * from state changes) ensures that navigation is one-shot and avoids
 * duplicate navigations on recomposition or configuration changes.
 */
internal sealed interface WearNavigationEvent {

    /**
     * Push a new route onto the back stack.
     *
     * @property route The destination route from [WearQuickEntryRoute].
     */
    data class Push(val route: String) : WearNavigationEvent

    /**
     * Reset the entire back stack to a single destination.
     *
     * Used when the flow completes (success → auto-reset back to the type
     * selection screen) to avoid leaving intermediate screens on the stack.
     *
     * @property route The destination to reset to.
     */
    data class ResetTo(val route: String) : WearNavigationEvent
}

/**
 * Root composable that hosts the Wear Navigation graph for the quick-entry flow.
 *
 * This screen:
 * 1. Creates and remembers a [SwipeDismissableNavController].
 * 2. Collects [WearNavigationEvent]s from the ViewModel and translates them
 *    into navigation controller calls (push or reset).
 * 3. Syncs the current route back to the ViewModel via [onSyncRoute] so the
 *    ViewModel can keep its internal step enum aligned with the back stack.
 * 4. Renders the correct step composable for each route inside a
 *    [SwipeDismissableNavHost] wrapped in an [AppScaffold].
 *
 * The [pendingCountFlow] is collected separately inside the Type destination
 * because it changes independently of the main UI state and should not cause
 * unnecessary recompositions of other screens.
 *
 * @param uiState           The current UI state from the ViewModel.
 * @param pendingCountFlow  A state flow of the pending-entry count.
 * @param formattedAmount   Pre-formatted amount string for the confirmation screen.
 * @param onAction          Callback to dispatch user actions to the ViewModel.
 * @param navigationEvents  Flow of one-shot navigation events from the ViewModel.
 * @param onSyncRoute       Callback invoked when the current route changes, so the
 *                          ViewModel can synchronise its step state.
 */
@Composable
internal fun WearQuickEntryScreen(
    uiState: WearQuickEntryUiState,
    pendingCountFlow: StateFlow<Int>,
    formattedAmount: String,
    onAction: (WearQuickEntryAction) -> Unit,
    navigationEvents: Flow<WearNavigationEvent>,
    onSyncRoute: (String?) -> Unit,
) {
    val navController = rememberSwipeDismissableNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    /**
     * Collects navigation events from the ViewModel and executes them on the
     * nav controller. [Push] uses launchSingleTop to prevent duplicate
     * destinations. [ResetTo] pops the entire back stack and navigates
     * to the target as the new root.
     */
    LaunchedEffect(navigationEvents) {
        navigationEvents.collect { event ->
            when (event) {
                is WearNavigationEvent.Push -> {
                    val route = event.route
                    if (navController.currentDestination?.route == route) return@collect
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
                is WearNavigationEvent.ResetTo -> {
                    navController.navigate(event.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    /**
     * Syncs the current navigation route back to the ViewModel whenever it
     * changes (including when the user swipes back). This allows the
     * ViewModel to update its step enum and clear stale messages.
     */
    LaunchedEffect(currentRoute) {
        onSyncRoute(currentRoute)
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearQuickEntryRoute.Type,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(WearQuickEntryRoute.Type) {
                val pendingCount by pendingCountFlow.collectAsStateWithLifecycle()
                TypeStep(
                    pendingCount = pendingCount,
                    onAction = onAction,
                )
            }
            composable(WearQuickEntryRoute.Amount) {
                AmountStep(
                    uiState = uiState,
                    onAction = onAction,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(WearQuickEntryRoute.Category) {
                CategoryStep(
                    uiState = uiState.copy(showAllCategories = false),
                    onAction = onAction,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(WearQuickEntryRoute.AllCategories) {
                CategoryStep(
                    uiState = uiState.copy(showAllCategories = true),
                    onAction = onAction,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(WearQuickEntryRoute.Confirm) {
                ConfirmStep(
                    uiState = uiState,
                    formattedAmount = formattedAmount,
                    onAction = onAction,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(WearQuickEntryRoute.Success) {
                SuccessStep(message = uiState.successMessage)
            }
        }
    }
}
