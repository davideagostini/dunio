package com.davideagostini.summ.ui.settings

import android.app.StatusBarManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.davideagostini.summ.BuildConfig
import com.davideagostini.summ.R
import com.davideagostini.summ.tile.QuickEntryTileService
import com.davideagostini.summ.ui.settings.components.SettingsInfoItem
import com.davideagostini.summ.ui.settings.components.SettingsNavItem
import com.davideagostini.summ.ui.settings.components.SettingsSectionLabel
import com.davideagostini.summ.ui.settings.language.AppLanguageManager
import com.davideagostini.summ.ui.settings.theme.AppThemeManager
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.SummColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateCurrency: () -> Unit,
    onNavigateLanguage: () -> Unit,
    onNavigateTheme: () -> Unit,
    onNavigateExport: () -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateMembers: () -> Unit,
    onNavigateRecurring: () -> Unit,
    onNavigateMonthClose: () -> Unit,
    onSignOut: () -> Unit,
    householdName: String,
    householdId: String,
    householdCurrency: String,
    userName: String,
    userPhotoUrl: String?,
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsStateWithLifecycle()
    val currentLanguageName = AppLanguageManager.displayName(currentLanguage.tag)
    val currentTheme by AppThemeManager.currentTheme.collectAsStateWithLifecycle()
    val currentThemeName = stringResource(
        when (currentTheme) {
            com.davideagostini.summ.ui.settings.theme.SupportedAppTheme.LIGHT -> R.string.settings_theme_option_light
            com.davideagostini.summ.ui.settings.theme.SupportedAppTheme.DARK -> R.string.settings_theme_option_dark
            com.davideagostini.summ.ui.settings.theme.SupportedAppTheme.SYSTEM -> R.string.settings_theme_option_system
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.settings_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            colors = SummColors.topBarColors,
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SettingsSectionLabel(stringResource(R.string.settings_account)) }

            item {
                AccountCard(
                    userName = userName,
                    userPhotoUrl = userPhotoUrl,
                    householdName = householdName,
                    householdId = householdId,
                    // The new clipboard API is suspend-based, so the copy action needs to run in a
                    // local coroutine instead of calling the deprecated synchronous manager.
                    onCopyHouseholdId = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipData.newPlainText("household_id", householdId).toClipEntry())
                        }
                    },
                    onSignOut = { showSignOutDialog = true },
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SettingsSectionLabel(stringResource(R.string.settings_preferences)) }

            item {
                SettingsNavItem(
                    icon = Icons.Default.AttachMoney,
                    title = stringResource(R.string.settings_currency_title),
                    subtitle = stringResource(R.string.settings_currency_subtitle, householdCurrency),
                    onClick = onNavigateCurrency,
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_subtitle, currentLanguageName),
                    onClick = onNavigateLanguage,
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.settings_theme_subtitle, currentThemeName),
                    onClick = onNavigateTheme,
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SettingsSectionLabel(stringResource(R.string.settings_household)) }

            item {
                SettingsNavItem(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.settings_members_title),
                    subtitle = stringResource(R.string.settings_members_subtitle),
                    onClick = onNavigateMembers,
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.Category,
                    title = stringResource(R.string.settings_categories_title),
                    subtitle = stringResource(R.string.settings_categories_subtitle),
                    onClick = onNavigateCategories,
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SettingsSectionLabel(stringResource(R.string.settings_finance)) }

            item {
                SettingsNavItem(
                    icon = Icons.Default.Autorenew,
                    title = stringResource(R.string.settings_recurring_title),
                    subtitle = stringResource(R.string.settings_recurring_subtitle),
                    onClick = onNavigateRecurring,
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.EventAvailable,
                    title = stringResource(R.string.settings_month_close_title),
                    subtitle = stringResource(R.string.settings_month_close_subtitle),
                    onClick = onNavigateMonthClose,
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.Add,
                    title = stringResource(R.string.settings_quick_tile_title),
                    subtitle = stringResource(R.string.settings_quick_tile_subtitle),
                    onClick = {
                        requestQuickSettingsTile(context)
                    },
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SettingsSectionLabel(stringResource(R.string.settings_data)) }

            item {
                SettingsNavItem(
                    icon = Icons.Default.FileDownload,
                    title = stringResource(R.string.settings_export_title),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    onClick = onNavigateExport,
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { SettingsSectionLabel(stringResource(R.string.settings_about)) }

            item {
                SettingsInfoItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_version),
                    value = BuildConfig.VERSION_NAME,
                )
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_sign_out_title), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.settings_sign_out_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    shape = AppButtonShape,
                ) {
                    Text(
                        stringResource(R.string.action_sign_out),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    shape = AppButtonShape,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun AccountCard(
    userName: String,
    userPhotoUrl: String?,
    householdName: String,
    householdId: String,
    onCopyHouseholdId: () -> Unit,
    onSignOut: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountInfoBlock(
                label = stringResource(R.string.settings_signed_in_as),
                value = userName,
                leading = {
                    AccountAvatar(
                        userName = userName,
                        userPhotoUrl = userPhotoUrl,
                    )
                },
            )

            AccountInfoBlock(
                label = stringResource(R.string.settings_current_household),
                value = householdName,
                actionLabel = stringResource(R.string.members_copy_id),
                onActionClick = onCopyHouseholdId,
            )

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = AppButtonShape,
            ) {
                Text(
                    stringResource(R.string.action_sign_out),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AccountInfoBlock(
    label: String,
    value: String,
    leading: @Composable (() -> Unit)? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.size(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(Modifier.size(12.dp))
                OutlinedButton(
                    onClick = onActionClick,
                    shape = AppButtonShape,
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun requestQuickSettingsTile(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.settings_quick_tile_not_supported), Toast.LENGTH_SHORT).show()
        return
    }

    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    if (statusBarManager == null) {
        Toast.makeText(context, context.getString(R.string.settings_quick_tile_not_supported), Toast.LENGTH_SHORT).show()
        return
    }

    statusBarManager.requestAddTileService(
        ComponentName(context, QuickEntryTileService::class.java),
        context.getString(R.string.tile_label),
        Icon.createWithResource(context, R.drawable.ic_wallet),
        context.mainExecutor,
        { result ->
            val message = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.settings_quick_tile_added
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> R.string.settings_quick_tile_not_added
                else -> R.string.settings_quick_tile_not_added
            }
            Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun AccountAvatar(
    userName: String,
    userPhotoUrl: String?,
) {
    val initials = userName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "S" }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(56.dp),
    ) {
        if (!userPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = userPhotoUrl,
                contentDescription = userName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
