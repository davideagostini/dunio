package com.davideagostini.summ.ui.settings.export

enum class ExportType {
    HOUSEHOLD_JSON,
    ENTRIES_CSV,
    ASSETS_CSV,
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val activeExport: ExportType? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
)
