package com.davideagostini.summ.ui.settings.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val exportRepository: ExportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun exportHouseholdBackup(uri: Uri?) = export(uri, ExportType.HOUSEHOLD_JSON) {
        exportRepository.exportHouseholdBackupJson(it)
    }

    fun exportEntries(uri: Uri?) = export(uri, ExportType.ENTRIES_CSV) {
        exportRepository.exportEntriesCsv(it)
    }

    fun exportAssets(uri: Uri?) = export(uri, ExportType.ASSETS_CSV) {
        exportRepository.exportAssetsCsv(it)
    }

    fun consumeFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun export(
        uri: Uri?,
        exportType: ExportType,
        block: suspend (Uri) -> Unit,
    ) {
        if (uri == null || _uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    activeExport = exportType,
                    feedbackMessage = null,
                    errorMessage = null,
                )
            }

            runCatching { block(uri) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            activeExport = null,
                            feedbackMessage = appContext.getString(R.string.settings_export_success),
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            activeExport = null,
                            errorMessage = throwable.message ?: appContext.getString(R.string.settings_export_error_generic),
                        )
                    }
                }
        }
    }
}
