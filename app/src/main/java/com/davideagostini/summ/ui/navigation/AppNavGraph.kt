package com.davideagostini.summ.ui.navigation

// Root navigation shell for the mobile app: owns auth gating, route wiring, and shared overlay visibility.
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.davideagostini.summ.R
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.assets.AssetsScreen
import com.davideagostini.summ.ui.auth.AuthGateScreen
import com.davideagostini.summ.ui.auth.SessionViewModel
import com.davideagostini.summ.ui.categories.CategoriesScreen
import com.davideagostini.summ.ui.dashboard.DashboardScreen
import com.davideagostini.summ.ui.entry.QuickEntryScreen
import com.davideagostini.summ.ui.settings.SettingsScreen
import com.davideagostini.summ.ui.settings.currency.CurrencyScreen
import com.davideagostini.summ.ui.settings.members.MembersScreen
import com.davideagostini.summ.ui.settings.monthclose.MonthCloseScreen
import com.davideagostini.summ.ui.settings.recurring.RecurringScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val sessionUiState by sessionViewModel.uiState.collectAsStateWithLifecycle()

    // The app stays behind the auth gate until the session is restored and household access is known.
    if (sessionUiState.isHouseholdTransitioning) {
        AuthGateScreen(sessionViewModel)
        return
    }

    val readyState = sessionState as? SessionState.Ready
    if (readyState == null) {
        AuthGateScreen(sessionViewModel)
        return
    }

    // Shared overlay state lives here so dashboard, assets, entries, and month close can coordinate with the shell.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showEntrySheet by remember { mutableStateOf(false) }
    var showEntriesFullscreenEditor by remember { mutableStateOf(false) }
    var showAssetsFullscreenEditor by remember { mutableStateOf(false) }
    var showMonthPickerOverlay by remember { mutableStateOf(false) }
    var allowEntrySheetHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowEntrySheetHide },
    )

    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // The NavHost keeps feature routes isolated while the shell manages cross-screen overlays.
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.matchParentSize(),
        ) {
            composable("entries") {
                // Entries reports overlay visibility so the shell can hide the bottom bar when needed.
                EntriesScreenWithOverlayState(
                    onFullscreenEditVisibilityChanged = { showEntriesFullscreenEditor = it },
                    onMonthPickerVisibilityChanged = { showMonthPickerOverlay = it },
                )
            }
            composable("dashboard") {
                // Dashboard only needs to report month-picker visibility.
                DashboardScreen(
                    onMonthPickerVisibilityChanged = { showMonthPickerOverlay = it },
                )
            }
            composable("assets") {
                // Assets also participates in the shared overlay state because it uses fullscreen editing.
                AssetsScreen(
                    onFullscreenEditVisibilityChanged = { showAssetsFullscreenEditor = it },
                    onMonthPickerVisibilityChanged = { showMonthPickerOverlay = it },
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateCurrency = { navigate("currency") },
                    onNavigateCategories = { navigate("categories") },
                    onNavigateMembers = { navigate("members") },
                    onNavigateRecurring = { navigate("recurring") },
                    onNavigateMonthClose = { navigate("month-close") },
                    onSignOut = sessionViewModel::signOut,
                    householdName = readyState.household.name,
                    householdId = readyState.household.id,
                    householdCurrency = readyState.household.currency,
                    userName = readyState.user.name,
                    userPhotoUrl = readyState.user.photoUrl,
                )
            }
            composable("currency") {
                CurrencyScreen(
                    selectedCurrency = readyState.household.currency,
                    isUpdatingCurrency = sessionUiState.isSubmitting,
                    errorMessage = sessionUiState.errorMessage,
                    onSelectCurrency = sessionViewModel::updateHouseholdCurrency,
                    onDismissError = sessionViewModel::consumeError,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("members") {
                MembersScreen(
                    householdId = readyState.household.id,
                    currentUserId = readyState.user.uid,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("categories") {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable("recurring") {
                RecurringScreen(onBack = { navController.popBackStack() })
            }
            composable("month-close") {
                // Month close uses the same shared month picker overlay as the other financial screens.
                MonthCloseScreen(
                    onBack = { navController.popBackStack() },
                    onMonthPickerVisibilityChanged = { showMonthPickerOverlay = it },
                )
            }
        }

        // Hide the bottom bar whenever a modal or fullscreen flow would compete for attention.
        if (currentRoute != "categories" &&
            currentRoute != "currency" &&
            currentRoute != "members" &&
            currentRoute != "recurring" &&
            currentRoute != "month-close" &&
            !showMonthPickerOverlay &&
            !showEntriesFullscreenEditor &&
            !showAssetsFullscreenEditor
        ) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SummBottomBar(
                    currentRoute = currentRoute,
                    onNavigateDashboard = { navigate("dashboard") },
                    onNavigateEntries = { navigate("entries") },
                    onAddEntry = { showEntrySheet = true },
                    onNavigateAssets = { navigate("assets") },
                    onNavigateSettings = { navigate("settings") },
                )
            }
        }
    }

    // Quick entry is a modal sheet above the shell, so dismissal must restore the sheet flag explicitly.
    if (showEntrySheet) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            QuickEntryScreen(
                resolvedSessionState = readyState,
                onDismiss = {
                    scope.launch {
                        allowEntrySheetHide = true
                        sheetState.hide()
                        showEntrySheet = false
                        allowEntrySheetHide = false
                    }
                },
            )
        }
    }
}

@Composable
private fun EntriesScreenWithOverlayState(
    onFullscreenEditVisibilityChanged: (Boolean) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
    viewModel: com.davideagostini.summ.ui.entries.EntriesViewModel = hiltViewModel(),
) {
    // The wrapper keeps navigation concerns out of the entries screen while still exposing overlay state to the shell.
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        com.davideagostini.summ.ui.components.FullScreenLoading()
        return
    }

    com.davideagostini.summ.ui.entries.EntriesContent(
        renderState = renderState,
        categories = categories,
        uiState = uiState,
        onEvent = viewModel::handleEvent,
        onFullscreenEditVisibilityChanged = onFullscreenEditVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

@Composable
private fun SummBottomBar(
    currentRoute: String?,
    onNavigateDashboard: () -> Unit,
    onNavigateEntries: () -> Unit,
    onAddEntry: () -> Unit,
    onNavigateAssets: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp)
            .navigationBarsPadding()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                NavTab(
                    label = stringResource(R.string.dashboard_nav_label),
                    icon = if (currentRoute == "dashboard") Icons.Filled.BarChart else Icons.Outlined.BarChart,
                    selected = currentRoute == "dashboard",
                    onClick = onNavigateDashboard,
                )
                NavTab(
                    label = stringResource(R.string.entries),
                    icon = if (currentRoute == "entries") Icons.Filled.Receipt else Icons.Outlined.Receipt,
                    selected = currentRoute == "entries",
                    onClick = onNavigateEntries,
                )
                NavTab(
                    label = stringResource(R.string.add_entry),
                    icon = Icons.Outlined.Add,
                    selected = false,
                    onClick = onAddEntry,
                )
                NavTab(
                    label = stringResource(R.string.dashboard_assets_label),
                    icon = if (currentRoute == "assets") Icons.Filled.Wallet else Icons.Outlined.Wallet,
                    selected = currentRoute == "assets",
                    onClick = onNavigateAssets,
                )
                NavTab(
                    label = stringResource(R.string.settings_title),
                    icon = if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                    selected = currentRoute == "settings",
                    onClick = onNavigateSettings,
                )
            }
        }
    }
}

@Composable
private fun NavTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
