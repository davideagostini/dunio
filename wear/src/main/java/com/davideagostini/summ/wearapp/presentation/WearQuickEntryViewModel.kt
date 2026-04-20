package com.davideagostini.summ.wearapp.presentation

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.data.WearQuickEntryRepository
import com.davideagostini.summ.wearapp.model.WearCategory
import com.davideagostini.summ.wearapp.model.WearSaveResult
import com.davideagostini.summ.wearapp.navigation.WearNavigationEvent
import com.davideagostini.summ.wearapp.navigation.WearQuickEntryRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private val usSymbols = DecimalFormatSymbols(Locale.US)
private val amountFormatter = DecimalFormat("#,##0.00", usSymbols)
private val amountInputSanitizer = Regex("[^0-9,.-]")

/**
 * Linear watch flow. The navigation intentionally stays shallow so the whole task fits the wrist.
 */
internal enum class WearQuickEntryStep {
    Type,
    Amount,
    Category,
    Confirm,
    Success,
}

@Immutable
internal data class WearQuickEntryUiState(
    val step: WearQuickEntryStep = WearQuickEntryStep.Type,
    val type: String = "expense",
    val amount: String = "",
    val currency: String = "EUR",
    val categories: List<WearCategory> = emptyList(),
    val quickCategories: List<WearCategory> = emptyList(),
    val selectedCategory: WearCategory? = null,
    val skipCategoryStep: Boolean = false,
    val showAllCategories: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val pendingCount: Int = 0,
)

/**
 * Watch-side state holder.
 *
 * It orchestrates category loading, local validation, queued saves, and automatic retry attempts
 * without exposing the transport details to the UI.
 */
internal class WearQuickEntryViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = WearQuickEntryRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(WearQuickEntryUiState())
    val uiState = _uiState.asStateFlow()
    val pendingCount = uiState
        .map { it.pendingCount }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = _uiState.value.pendingCount,
        )
    private val saveInFlight = AtomicBoolean(false)
    private var loadCategoriesJob: Job? = null
    private var prefetchCategoriesJob: Job? = null

    private val navigationEvents = Channel<WearNavigationEvent>(Channel.BUFFERED)
    val navigateToRoute = navigationEvents.receiveAsFlow()

    init {
        // Pending entries now live in the Data Layer. Listening here keeps the chip aligned both
        // when the watch queues a new entry locally and when the phone later consumes and deletes
        // the same pending item after reconnecting.
        viewModelScope.launch {
            repository.observePendingCount().collectLatest { pendingCount ->
                _uiState.update { it.copy(pendingCount = pendingCount) }
            }
        }
        syncQuickCategories("expense")
        loadCategories("expense")
        warmCategoriesCache("income")
    }

    fun onAction(action: WearQuickEntryAction) {
        when (action) {
            is WearQuickEntryAction.SelectType -> selectType(action.type)
            is WearQuickEntryAction.AmountChanged -> updateAmount(action.value)
            WearQuickEntryAction.ContinueFromAmount -> continueFromAmount()
            is WearQuickEntryAction.SelectCategory -> selectCategory(action.category)
            WearQuickEntryAction.ShowAllCategories -> showAllCategories()
            WearQuickEntryAction.RetryCategories -> retryCategories()
            WearQuickEntryAction.Save -> save()
            WearQuickEntryAction.RefreshPendingCount -> refreshPendingCount()
        }
    }

    fun syncFromNavigation(route: String?) {
        _uiState.update { state ->
            when (route) {
                WearQuickEntryRoute.Type -> {
                    if (state.step == WearQuickEntryStep.Type && !state.showAllCategories) state
                    else state.copy(step = WearQuickEntryStep.Type, showAllCategories = false, successMessage = null, errorMessage = null)
                }
                WearQuickEntryRoute.Amount -> {
                    if (state.step == WearQuickEntryStep.Amount && !state.showAllCategories) state
                    else state.copy(step = WearQuickEntryStep.Amount, showAllCategories = false, successMessage = null, errorMessage = null)
                }
                WearQuickEntryRoute.Category -> {
                    if (state.step == WearQuickEntryStep.Category && !state.showAllCategories) state
                    else state.copy(step = WearQuickEntryStep.Category, showAllCategories = false, successMessage = null, errorMessage = null)
                }
                WearQuickEntryRoute.AllCategories -> {
                    if (state.step == WearQuickEntryStep.Category && state.showAllCategories) state
                    else state.copy(step = WearQuickEntryStep.Category, showAllCategories = true, successMessage = null, errorMessage = null)
                }
                WearQuickEntryRoute.Confirm -> {
                    if (state.step == WearQuickEntryStep.Confirm) state
                    else state.copy(step = WearQuickEntryStep.Confirm, showAllCategories = false, successMessage = null, errorMessage = null)
                }
                WearQuickEntryRoute.Success -> {
                    if (state.step == WearQuickEntryStep.Success) state
                    else state.copy(step = WearQuickEntryStep.Success)
                }
                null -> state
                else -> state
            }
        }
    }

    fun refreshPendingCount() {
        viewModelScope.launch {
            val pendingCount = repository.pendingCount()
            _uiState.update { it.copy(pendingCount = pendingCount) }
        }
    }

    private fun selectType(type: String) {
        val cachedCategories = repository.readCachedCategories(type)
        _uiState.update {
            it.copy(
                type = type,
                step = WearQuickEntryStep.Amount,
                categories = cachedCategories,
                quickCategories = cachedCategories.take(3),
                selectedCategory = null,
                skipCategoryStep = false,
                showAllCategories = false,
                successMessage = null,
                errorMessage = null,
            )
        }
        navigate(WearQuickEntryRoute.Amount)
        loadCategories(type)
    }

    private fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value, successMessage = null, errorMessage = null) }
    }

    private fun continueFromAmount() {
        val amount = parseAmount(_uiState.value.amount)
        if (amount == null || amount <= 0.0) {
            _uiState.update {
                it.copy(errorMessage = getApplication<Application>().getString(R.string.wear_amount_validation))
            }
            return
        }
        _uiState.update {
            it.copy(
                step = if (it.selectedCategory != null) WearQuickEntryStep.Confirm else WearQuickEntryStep.Category,
                successMessage = null,
                errorMessage = null,
            )
        }
        navigate(if (_uiState.value.selectedCategory != null) WearQuickEntryRoute.Confirm else WearQuickEntryRoute.Category)
        if (_uiState.value.categories.isEmpty()) {
            loadCategories(_uiState.value.type)
        }
    }

    private fun selectCategory(category: WearCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                skipCategoryStep = false,
                step = WearQuickEntryStep.Confirm,
                showAllCategories = false,
                successMessage = null,
                errorMessage = null,
            )
        }
        navigate(WearQuickEntryRoute.Confirm)
    }

    private fun showAllCategories() {
        _uiState.update { it.copy(showAllCategories = true, errorMessage = null) }
        navigate(WearQuickEntryRoute.AllCategories)
    }

    private fun retryCategories() {
        loadCategories(_uiState.value.type)
    }

    private fun save(): Boolean {
        if (!saveInFlight.compareAndSet(false, true)) return false

        val state = _uiState.value
        val amount = parseAmount(state.amount)
        val category = state.selectedCategory
        if (state.isSaving || amount == null || amount <= 0.0 || category == null) {
            saveInFlight.set(false)
            return false
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // Let Compose commit the loading state before the transport work starts so the watch UI
                // feels responsive even when the phone request resolves very quickly.
                yield()
                val startedAt = System.currentTimeMillis()
                runCatching {
                    repository.saveEntry(
                        type = state.type,
                        amount = amount,
                        category = category,
                    )
                }.onSuccess { result ->
                    val elapsed = System.currentTimeMillis() - startedAt
                    val minLoadingMillis = 350L
                    if (elapsed < minLoadingMillis) {
                        delay(minLoadingMillis - elapsed)
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            step = WearQuickEntryStep.Success,
                            successMessage = when (result) {
                                WearSaveResult.Saved -> getApplication<Application>().getString(R.string.wear_success_saved_message)
                                is WearSaveResult.Queued -> getApplication<Application>().getString(
                                    R.string.wear_success_queued_message,
                                    result.pendingCount,
                                )
                            },
                        )
                    }
                    // The critical section ends as soon as the save result is known. Keeping the
                    // latch active through the success delay makes a second offline entry feel
                    // blocked even though the first save already completed.
                    saveInFlight.set(false)
                    navigate(WearQuickEntryRoute.Success)
                    delay(2000L)
                    resetFlow()
                }.onFailure { throwable ->
                    saveInFlight.set(false)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: getApplication<Application>().getString(R.string.wear_generic_error),
                        )
                    }
                }
            } finally {
                saveInFlight.set(false)
            }
        }
        return true
    }

    fun formattedAmount(): String {
        val amount = parseAmount(_uiState.value.amount) ?: 0.0
        return "${currencySymbol(_uiState.value.currency)}${amountFormatter.format(amount)}"
    }

    private fun loadCategories(type: String) {
        loadCategoriesJob?.cancel()
        loadCategoriesJob = viewModelScope.launch {
            val cachedCategories = repository.readCachedCategories(type)
            _uiState.update {
                it.copy(
                    categories = if (cachedCategories.isNotEmpty()) cachedCategories else it.categories,
                    quickCategories = if (cachedCategories.isNotEmpty()) cachedCategories.take(3) else it.quickCategories,
                    isLoadingCategories = cachedCategories.isEmpty(),
                    successMessage = null,
                    errorMessage = null,
                )
            }
            runCatching {
                repository.loadCategories(type)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        currency = response.currency,
                        categories = response.categories,
                        quickCategories = response.categories.take(3),
                        isLoadingCategories = false,
                        errorMessage = null,
                    )
                }
                warmCategoriesCache(oppositeType(type))
            }.onFailure { throwable ->
                if (throwable is CancellationException) {
                    _uiState.update { it.copy(isLoadingCategories = false) }
                    throw throwable
                }
                val fallbackCategories = repository.readCachedCategories(type)
                _uiState.update {
                    it.copy(
                        categories = fallbackCategories,
                        quickCategories = fallbackCategories.take(3),
                        isLoadingCategories = false,
                        errorMessage = if (fallbackCategories.isEmpty()) {
                            throwable.message ?: getApplication<Application>().getString(R.string.wear_generic_error)
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    private fun warmCategoriesCache(type: String) {
        if (repository.readCachedCategories(type).isNotEmpty()) return

        prefetchCategoriesJob?.cancel()
        prefetchCategoriesJob = viewModelScope.launch {
            runCatching { repository.loadCategories(type) }
        }
    }

    private fun syncQuickCategories(type: String) {
        val cachedCategories = repository.readCachedCategories(type).take(3)
        _uiState.update {
            it.copy(
                quickCategories = cachedCategories,
            )
        }
    }

    private fun navigate(route: String) {
        viewModelScope.launch {
            navigationEvents.send(WearNavigationEvent.Push(route))
        }
    }

    private fun resetNavigation(route: String) {
        viewModelScope.launch {
            navigationEvents.send(WearNavigationEvent.ResetTo(route))
        }
    }

    private fun resetFlow() {
        _uiState.update {
            it.copy(
                step = WearQuickEntryStep.Type,
                type = "expense",
                amount = "",
                selectedCategory = null,
                skipCategoryStep = false,
                showAllCategories = false,
                isLoadingCategories = false,
                isSaving = false,
                successMessage = null,
                errorMessage = null,
            )
        }
        syncQuickCategories("expense")
        resetNavigation(WearQuickEntryRoute.Type)
    }

    private fun parseAmount(value: String): Double? {
        // The watch keyboard may emit locale-specific punctuation. Normalize it before parsing.
        val sanitized = amountInputSanitizer.replace(value.trim(), "")
        if (sanitized.isBlank()) return null

        val lastComma = sanitized.lastIndexOf(',')
        val lastDot = sanitized.lastIndexOf('.')
        val decimalSeparator = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 && looksLikeDecimalSeparator(sanitized, lastComma) -> ','
            lastDot >= 0 && looksLikeDecimalSeparator(sanitized, lastDot) -> '.'
            else -> null
        }

        val normalized = if (decimalSeparator == null) {
            sanitized.replace(",", "").replace(".", "")
        } else {
            val decimalIndex = sanitized.lastIndexOf(decimalSeparator)
            val integerPart = sanitized.substring(0, decimalIndex).replace(",", "").replace(".", "")
            val decimalPart = sanitized.substring(decimalIndex + 1).replace(",", "").replace(".", "")
            if (decimalPart.isBlank()) integerPart else "$integerPart.$decimalPart"
        }

        return normalized.toDoubleOrNull()
    }

    private fun currencySymbol(currency: String): String =
        runCatching {
            Currency.getInstance(currency.trim().uppercase(Locale.ROOT)).getSymbol(Locale.getDefault())
        }.getOrElse { currency }

    private fun looksLikeDecimalSeparator(value: String, separatorIndex: Int): Boolean {
        val digitsBefore = value.substring(0, separatorIndex).count { it.isDigit() }
        val digitsAfter = value.substring(separatorIndex + 1).count { it.isDigit() }
        return digitsBefore > 0 && digitsAfter in 1..2
    }

    private fun oppositeType(type: String): String =
        if (type == "income") "expense" else "income"
}
