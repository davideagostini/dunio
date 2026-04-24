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
import com.davideagostini.summ.data.repository.CategoryUsageRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.format.parseAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val isSaving: Boolean           = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * ViewModel behind the quick-entry flow.
 *
 * It drives the step-by-step expense/income editor, validates input at each step, and emits
 * navigation events once a transaction has been saved successfully.
 */
class QuickEntryViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryUsageRepository: CategoryUsageRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val categoriesLoaded = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val allCategories: StateFlow<List<Category>> =
        categoryRepository.allCategories
            .onEach { categoriesLoaded.value = true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = combine(
        allCategories,
        uiState.map { state -> state.type },
    ) { categories, _ -> categories }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostUsedCategories: StateFlow<List<Category>> = combine(
        sessionRepository.sessionState,
        uiState.map { state -> state.type },
        allCategories,
    ) { sessionState, type, categories ->
        val householdId = (sessionState as? SessionState.Ready)?.household?.id
        if (householdId == null) {
            null
        } else {
            CategoryUsageRequest(
                householdId = householdId,
                type = type,
                categories = categories,
            )
        }
    }.flatMapLatest { request ->
        if (request == null) {
            flowOf(emptyList())
        } else {
            categoryUsageRepository.observeMostUsedCategories(
                householdId = request.householdId,
                type = request.type,
                categories = request.categories,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> =
        categoriesLoaded
            .map { loaded -> !loaded }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _navEvents = Channel<EntryNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    private data class CategoryUsageRequest(
        val householdId: String,
        val type: String,
        val categories: List<Category>,
    )

    fun handleEvent(event: EntryEvent) {
        when (event) {
            is EntryEvent.SelectType        -> _uiState.update {
                it.copy(
                    type = event.type,
                    selectedCategory = it.selectedCategory?.takeIf { category -> category.type == event.type },
                    operationErrorMessage = null,
                )
            }
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
                val parsed = parseAmount(state.price)
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
        if (state.isSaving) return
        val parsedPrice = parseAmount(state.price) ?: return
        val category    = state.selectedCategory ?: return

        // Quick entry runs as a fast-path form, but Firestore failures still need the same graceful handling.
        _uiState.update { it.copy(isSaving = true, operationErrorMessage = null) }
        viewModelScope.launch {
            try {
                entryRepository.insert(
                    Entry(
                        type        = state.type,
                        description = state.description.trim(),
                        price       = parsedPrice,
                        category    = category.name,
                        categoryKey = category.systemKey,
                        date        = state.date,
                    )
                )
                markCategoryUsedSafely(
                    type = state.type,
                    category = category,
                )
                _uiState.update { it.copy(step = 6, operationErrorMessage = null, isSaving = false) }
                delay(1_500L)
                _navEvents.send(EntryNavEvent.Saved)
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(appContext),
                    )
                }
            }
        }
    }

    private suspend fun currentHouseholdId(): String =
        (sessionRepository.sessionState.first() as? SessionState.Ready)?.household?.id
            ?: sessionRepository.requireHouseholdId()

    private suspend fun markCategoryUsedSafely(
        type: String,
        category: Category,
    ) {
        try {
            categoryUsageRepository.markUsed(
                householdId = currentHouseholdId(),
                type = type,
                category = category,
            )
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
        }
    }
}
