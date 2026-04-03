package com.davideagostini.summ.ui.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.firebase.toFirestoreUserMessage
import com.davideagostini.summ.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
/**
 * ViewModel for category management.
 *
 * This feature is intentionally small: it observes category data, drives the add/edit/delete sheet
 * state, and performs validation before delegating persistence to the repository layer.
 */
class CategoriesViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: CategoryRepository,
) : ViewModel() {
    private val categoriesLoaded = MutableStateFlow(false)

    val categories: StateFlow<List<Category>> = repository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = categoriesLoaded
        .map { loaded -> !loaded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    fun handleEvent(event: CategoriesEvent) {
        when (event) {
            CategoriesEvent.StartAdd        -> _uiState.update {
                it.copy(
                    sheetMode = CategorySheetMode.Add,
                    editName = "",
                    editEmoji = "",
                    nameError = null,
                    operationErrorMessage = null,
                )
            }
            is CategoriesEvent.Select       -> _uiState.update {
                it.copy(
                    sheetMode = CategorySheetMode.Action,
                    selectedCategory = event.category,
                    operationErrorMessage = null,
                )
            }
            CategoriesEvent.StartEdit       -> {
                val cat = _uiState.value.selectedCategory ?: return
                _uiState.update {
                    it.copy(
                        sheetMode = CategorySheetMode.Edit,
                        editName = cat.name,
                        editEmoji = cat.emoji,
                        nameError = null,
                        operationErrorMessage = null,
                    )
                }
            }
            CategoriesEvent.RequestDelete   -> _uiState.update { it.copy(showDeleteDialog = true, operationErrorMessage = null) }
            CategoriesEvent.DismissSheet    -> _uiState.update { CategoriesUiState() }
            is CategoriesEvent.UpdateFormName  -> _uiState.update { it.copy(editName = event.value, nameError = null, operationErrorMessage = null) }
            is CategoriesEvent.UpdateFormEmoji -> _uiState.update { it.copy(editEmoji = event.value, operationErrorMessage = null) }
            CategoriesEvent.SaveAdd         -> saveAdd()
            CategoriesEvent.SaveEdit        -> saveEdit()
            CategoriesEvent.ConfirmDelete   -> confirmDelete()
            CategoriesEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }

    private fun saveAdd() {
        val state = _uiState.value
        if (state.editName.isBlank()) {
            _uiState.update { it.copy(nameError = appContext.getString(R.string.category_validation_name_required)) }
            return
        }

        // Firestore write rejections must be translated into UI state instead of bubbling up as a crash.
        viewModelScope.launch {
            try {
                repository.insert(Category(name = state.editName.trim(), emoji = state.editEmoji.trim().ifEmpty { "📦" }))
                _uiState.update { it.copy(sheetMode = CategorySheetMode.Success, operationErrorMessage = null) }
                delay(1_500L)
                _uiState.update { CategoriesUiState() }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }

    private fun saveEdit() {
        val state = _uiState.value
        val cat   = state.selectedCategory ?: return
        if (state.editName.isBlank()) {
            _uiState.update { it.copy(nameError = appContext.getString(R.string.category_validation_name_required)) }
            return
        }
        viewModelScope.launch {
            try {
                repository.update(cat.copy(name = state.editName.trim(), emoji = state.editEmoji.trim().ifEmpty { "📦" }))
                _uiState.update { it.copy(sheetMode = CategorySheetMode.Success, operationErrorMessage = null) }
                delay(1_500L)
                _uiState.update { CategoriesUiState() }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }

    private fun confirmDelete() {
        val cat = _uiState.value.selectedCategory ?: return

        // Delete uses the same protection as save flows because a denied delete is still a rejected write.
        viewModelScope.launch {
            try {
                repository.delete(cat)
                _uiState.update { it.copy(sheetMode = CategorySheetMode.Success, showDeleteDialog = false, operationErrorMessage = null) }
                delay(1_500L)
                _uiState.update { CategoriesUiState() }
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        showDeleteDialog = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(appContext),
                    )
                }
            }
        }
    }
}
