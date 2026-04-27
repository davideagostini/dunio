package com.davideagostini.summ.ui.navigation

// Root navigation shell for the mobile app: owns auth gating, route wiring, and shared overlay visibility.
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import com.davideagostini.summ.R
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.auth.AuthGateScreen
import com.davideagostini.summ.ui.auth.SessionViewModel
import com.davideagostini.summ.ui.entry.QuickEntryScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
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

    // Shared overlay state lives here so dashboard, assets, entries, and month close can coordinate
    // with the shell. This remains true in Navigation 3: not every piece of visible UI should be
    // modeled as a destination. Some UI is still shell-owned state.
    val navigationState = rememberAppNavigationState()
    val currentKey = navigationState.currentKey
    val context = LocalContext.current

    var showEntrySheet by remember { mutableStateOf(false) }
    var showEntriesFullscreenEditor by remember { mutableStateOf(false) }
    var showAssetsFullscreenEditor by remember { mutableStateOf(false) }
    var showDashboardGetStarted by remember { mutableStateOf(false) }
    var openAssetsAddOnNextVisit by remember { mutableStateOf(false) }
    var showMonthPickerOverlay by remember { mutableStateOf(false) }
    var allowEntrySheetHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowEntrySheetHide },
    )
    val dismissEntrySheet: () -> Unit = {
        scope.launch {
            allowEntrySheetHide = true
            sheetState.hide()
            showEntrySheet = false
            allowEntrySheetHide = false
        }
        Unit
    }

    val entryProvider = appEntryProvider(
        navigationState = navigationState,
        readyState = readyState,
        sessionUiStateIsSubmitting = sessionUiState.isSubmitting,
        sessionUiStateErrorMessage = sessionUiState.errorMessage,
        onUpdateHouseholdCurrency = sessionViewModel::updateHouseholdCurrency,
        onConsumeSessionError = sessionViewModel::consumeError,
        onSignOut = sessionViewModel::signOut,
        onOpenQuickEntry = { showEntrySheet = true },
        onEntriesFullscreenEditVisibilityChanged = { showEntriesFullscreenEditor = it },
        onAssetsFullscreenEditVisibilityChanged = { showAssetsFullscreenEditor = it },
        onMonthPickerVisibilityChanged = { showMonthPickerOverlay = it },
        onDashboardGetStartedVisibilityChanged = { showDashboardGetStarted = it },
        openAssetsAddOnLaunch = openAssetsAddOnNextVisit,
        onAssetsAddConsumed = { openAssetsAddOnNextVisit = false },
    )
    val navEntries = navigationState.toDecoratedEntries(entryProvider)

    Box(modifier = Modifier.fillMaxSize()) {
        // NavDisplay is now the production navigation host for the phone shell.
        // The entries come from AppNavigationState rather than from a NavController-owned graph,
        // which means the app now truly owns its back stack as recommended by Navigation 3.
        NavDisplay(
            entries = navEntries,
            modifier = Modifier.matchParentSize(),
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            },
            predictivePopTransitionSpec = { _: Int ->
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            },
            onBack = {
                if (!navigationState.goBack()) {
                    context.findActivity()?.finish()
                }
            }
        )

        // Bottom-bar visibility is now derived from typed navigation keys plus shell overlays.
        if (shouldShowBottomBar(
                currentKey = currentKey,
                showDashboardGetStarted = showDashboardGetStarted,
                showMonthPickerOverlay = showMonthPickerOverlay,
                showEntriesFullscreenEditor = showEntriesFullscreenEditor,
                showAssetsFullscreenEditor = showAssetsFullscreenEditor,
            )
        ) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SummBottomBar(
                    selectedTab = navigationState.selectedTopLevel,
                    onNavigateDashboard = { navigationState.selectTopLevel(DashboardKey) },
                    onNavigateEntries = { navigationState.selectTopLevel(EntriesKey) },
                    onAddEntry = { showEntrySheet = true },
                    onNavigateAssets = { navigationState.selectTopLevel(AssetsKey) },
                    onNavigateSettings = { navigationState.selectTopLevel(SettingsKey) },
                )
            }
        }
    }

    // Quick entry is a modal sheet above the shell, so dismissal must restore the sheet flag explicitly.
    if (showEntrySheet) {
        ModalBottomSheet(
            onDismissRequest = dismissEntrySheet,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            QuickEntryScreen(
                resolvedSessionState = readyState,
                onDismiss = dismissEntrySheet,
            )
        }
    }
}

/**
 * Walks up the Context chain until it finds an Activity.
 *
 * This is used only as the final fallback when Navigation 3 reports that the
 * shell no longer wants to handle "back" (Dashboard root). At that point the
 * correct behavior is to let the app screen finish.
 */
private tailrec fun android.content.Context.findActivity(): android.app.Activity? =
    when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun SummBottomBar(
    selectedTab: TopLevelKey,
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
                    label = stringResource(R.string.dashboard_title),
                    icon = if (selectedTab == DashboardKey) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                    selected = selectedTab == DashboardKey,
                    onClick = onNavigateDashboard,
                )
                NavTab(
                    label = stringResource(R.string.entries_action_title),
                    icon = if (selectedTab == EntriesKey) Icons.Filled.Receipt else Icons.Outlined.Receipt,
                    selected = selectedTab == EntriesKey,
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
                    icon = if (selectedTab == AssetsKey) Icons.Filled.Wallet else Icons.Outlined.Wallet,
                    selected = selectedTab == AssetsKey,
                    onClick = onNavigateAssets,
                )
                NavTab(
                    label = stringResource(R.string.settings_title),
                    icon = if (selectedTab == SettingsKey) Icons.Filled.Settings else Icons.Outlined.Settings,
                    selected = selectedTab == SettingsKey,
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
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
