package com.davideagostini.summ.ui.settings.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.settings.components.SettingsNavItem
import com.davideagostini.summ.ui.theme.SummColors
import java.time.LocalDate

/**
 * Screen for exporting household data.
 *
 * It presents the available export formats and reflects the in-flight export state managed by
 * [ExportViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val today = LocalDate.now().toString()

    val householdBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = viewModel::exportHouseholdBackup,
    )
    val entriesCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = viewModel::exportEntries,
    )
    val assetsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = viewModel::exportAssets,
    )

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeFeedback()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.consumeError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_export_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_desc_back),
                    )
                }
            },
            colors = SummColors.topBarColors,
        )

        ExportHeaderSection()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsNavItem(
                    icon = Icons.Default.FileDownload,
                    title = stringResource(R.string.settings_export_household_title),
                    subtitle = stringResource(
                        if (uiState.activeExport == ExportType.HOUSEHOLD_JSON && uiState.isExporting) {
                            R.string.settings_export_status_running
                        } else {
                            R.string.settings_export_household_subtitle
                        }
                    ),
                    onClick = {
                        if (!uiState.isExporting) {
                            householdBackupLauncher.launch("summ-household-backup-$today.json")
                        }
                    },
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.settings_export_entries_title),
                    subtitle = stringResource(
                        if (uiState.activeExport == ExportType.ENTRIES_CSV && uiState.isExporting) {
                            R.string.settings_export_status_running
                        } else {
                            R.string.settings_export_entries_subtitle
                        }
                    ),
                    onClick = {
                        if (!uiState.isExporting) {
                            entriesCsvLauncher.launch("summ-entries-$today.csv")
                        }
                    },
                )
            }

            item {
                SettingsNavItem(
                    icon = Icons.Default.Wallet,
                    title = stringResource(R.string.settings_export_assets_title),
                    subtitle = stringResource(
                        if (uiState.activeExport == ExportType.ASSETS_CSV && uiState.isExporting) {
                            R.string.settings_export_status_running
                        } else {
                            R.string.settings_export_assets_subtitle
                        }
                    ),
                    onClick = {
                        if (!uiState.isExporting) {
                            assetsCsvLauncher.launch("summ-assets-$today.csv")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ExportHeaderSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_export_screen_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
