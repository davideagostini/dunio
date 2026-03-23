package com.davideagostini.summ.ui.entry

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.firebase.toFirestoreUserMessage
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.EntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class EntryUiState(
    val step: Int                   = 0,
    val type: String                = "expense",
    val date: Long                  = System.currentTimeMillis(),
    val description: String         = "",
    val price: String               = "",
    val selectedCategory: Category? = null,
    val descriptionError: String?   = null,
    val priceError: String?         = null,
    val operationErrorMessage: String? = null,
)

@HiltViewModel
class QuickEntryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val categoriesLoaded = MutableStateFlow(false)

    val categories: StateFlow<List<Category>> =
        categoryRepository.allCategories
            .onEach { categoriesLoaded.value = true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> =
        categoriesLoaded
            .map { loaded -> !loaded }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<EntryNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    fun handleEvent(event: EntryEvent) {
        when (event) {
            is EntryEvent.SelectType        -> _uiState.update { it.copy(type = event.type, operationErrorMessage = null) }
            is EntryEvent.UpdateDate        -> _uiState.update { it.copy(date = event.value, operationErrorMessage = null) }
            is EntryEvent.UpdateDescription -> _uiState.update { it.copy(description = event.value, descriptionError = null, operationErrorMessage = null) }
            is EntryEvent.UpdatePrice       -> _uiState.update { it.copy(price = event.value, priceError = null, operationErrorMessage = null) }
            is EntryEvent.SelectCategory    -> _uiState.update { it.copy(selectedCategory = event.category, operationErrorMessage = null) }
            EntryEvent.Reset                -> _uiState.value = EntryUiState()
            EntryEvent.Next                 -> advanceStep()
            EntryEvent.Back                 -> _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0), operationErrorMessage = null) }
            EntryEvent.Save                 -> save()
        }
    }

    private fun advanceStep() {
        val state = _uiState.value
        when (state.step) {
            0 -> _uiState.update { it.copy(step = 1) }
            1 -> _uiState.update { it.copy(step = 2) }
            2 -> {
                if (state.description.isBlank()) {
                    _uiState.update { it.copy(descriptionError = appContext.getString(R.string.quick_entry_validation_description)) }
                } else {
                    _uiState.update { it.copy(step = 3, descriptionError = null) }
                }
            }
            3 -> {
                val parsed = state.price.toDoubleOrNull()
                val error = when {
                    state.price.isBlank()             -> appContext.getString(R.string.quick_entry_validation_amount_required)
                    parsed == null || parsed <= 0     -> appContext.getString(R.string.quick_entry_validation_amount_positive)
                    else                              -> null
                }
                if (error != null) {
                    _uiState.update { it.copy(priceError = error) }
                } else {
                    _uiState.update { it.copy(step = 4, priceError = null) }
                }
            }
            4 -> {
                if (state.selectedCategory != null) {
                    _uiState.update { it.copy(step = 5) }
                }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val parsedPrice = state.price.toDoubleOrNull() ?: return
        val category    = state.selectedCategory ?: return

        // Quick entry runs as a fast-path form, but Firestore failures still need the same graceful handling.
        viewModelScope.launch {
            try {
                entryRepository.insert(
                    Entry(
                        type        = state.type,
                        description = state.description.trim(),
                        price       = parsedPrice,
                        category    = category.name,
                        date        = state.date,
                    )
                )
                _uiState.update { it.copy(step = 6, operationErrorMessage = null) }
                delay(1_500L)
                _navEvents.send(EntryNavEvent.Saved)
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }
}
