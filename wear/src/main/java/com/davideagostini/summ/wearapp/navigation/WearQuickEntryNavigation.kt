package com.davideagostini.summ.wearapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryUiState
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.steps.AmountStep
import com.davideagostini.summ.wearapp.ui.steps.CategoryStep
import com.davideagostini.summ.wearapp.ui.steps.ConfirmStep
import com.davideagostini.summ.wearapp.ui.steps.SuccessStep
import com.davideagostini.summ.wearapp.ui.steps.TypeStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal object WearQuickEntryRoute {
    const val Type = "type"
    const val Amount = "amount"
    const val Category = "category"
    const val AllCategories = "all_categories"
    const val Confirm = "confirm"
    const val Success = "success"
}

internal sealed interface WearNavigationEvent {
    data class Push(val route: String) : WearNavigationEvent
    data class ResetTo(val route: String) : WearNavigationEvent
}

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

    LaunchedEffect(currentRoute) {
        onSyncRoute(currentRoute)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WearThemeTokens.background),
    ) {
        TimeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
        )

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
