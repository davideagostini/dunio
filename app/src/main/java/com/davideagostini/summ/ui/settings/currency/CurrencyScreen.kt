package com.davideagostini.summ.ui.settings.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.format.currencySymbol
import com.davideagostini.summ.ui.format.normalizeCurrencyCode
import com.davideagostini.summ.ui.theme.SummColors
import com.davideagostini.summ.ui.theme.listItemShape
import java.util.Currency
import java.util.Locale

private data class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
)

/**
 * Currency selection screen for the active household.
 *
 * It is kept stateless so the parent flow can decide how and when the chosen currency is saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    selectedCurrency: String,
    isUpdatingCurrency: Boolean,
    errorMessage: String?,
    onSelectCurrency: (String) -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
) {
    var pendingSelection by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val options = remember {
        Locale.getAvailableLocales()
            .mapNotNull { locale ->
                val country = locale.country
                if (country.isBlank()) return@mapNotNull null
                runCatching { Currency.getInstance(locale) }.getOrNull()
            }
            .distinctBy { currency -> currency.currencyCode }
            .map { currency ->
                CurrencyOption(
                    code = currency.currencyCode,
                    symbol = currency.getSymbol(Locale.getDefault()),
                    displayName = currency.getDisplayName(Locale.getDefault()),
                )
            }
            .sortedWith(compareBy<CurrencyOption> { it.displayName.lowercase(Locale.getDefault()) }.thenBy { it.code })
    }
    val filteredOptions = remember(options, searchQuery) {
        val normalizedQuery = searchQuery.trim()
        if (normalizedQuery.isBlank()) {
            options
        } else {
            options.filter { option ->
                option.code.contains(normalizedQuery, ignoreCase = true) ||
                    option.symbol.contains(normalizedQuery, ignoreCase = true) ||
                    option.displayName.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val selectedCurrencyCode = normalizeCurrencyCode(selectedCurrency)
    val orderedOptions = remember(filteredOptions, selectedCurrencyCode) {
        filteredOptions.sortedWith(
            compareByDescending<CurrencyOption> { it.code == selectedCurrencyCode }
                .thenBy { it.displayName.lowercase(Locale.getDefault()) }
                .thenBy { it.code }
        )
    }

    LaunchedEffect(selectedCurrency, isUpdatingCurrency, errorMessage, pendingSelection) {
        val currentPending = pendingSelection ?: return@LaunchedEffect
        if (!isUpdatingCurrency && errorMessage == null && normalizeCurrencyCode(selectedCurrency) == currentPending) {
            pendingSelection = null
            onBack()
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
                    stringResource(R.string.settings_currency_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                }
            },
            colors = SummColors.topBarColors,
        )

        CurrencyHeaderSection(
            searchQuery = searchQuery,
            errorMessage = errorMessage,
            onSearchQueryChange = { searchQuery = it },
            onClearSearch = { searchQuery = "" },
        )

        CurrencyListSection(
            options = orderedOptions,
            selectedCurrencyCode = selectedCurrencyCode,
            isUpdatingCurrency = isUpdatingCurrency,
            onSelectCurrency = { currencyCode ->
                pendingSelection = currencyCode
                onDismissError()
                onSelectCurrency(currencyCode)
            },
        )
    }
}

@Composable
private fun CurrencyHeaderSection(
    searchQuery: String,
    errorMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!errorMessage.isNullOrBlank()) {
            AuthErrorCard(errorMessage)
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_currency_screen_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.settings_currency_search_placeholder)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_clear_search))
                    }
                }
            },
            shape = RoundedCornerShape(22.dp),
        )
    }
}

@Composable
private fun CurrencyListSection(
    options: List<CurrencyOption>,
    selectedCurrencyCode: String,
    isUpdatingCurrency: Boolean,
    onSelectCurrency: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(options, key = { _, option -> option.code }) { index, option ->
            CurrencyListItem(
                option = option,
                index = index,
                count = options.size,
                isSelected = selectedCurrencyCode == option.code,
                enabled = !isUpdatingCurrency,
                onClick = { onSelectCurrency(option.code) },
            )
        }
    }
}

@Composable
private fun CurrencyListItem(
    option: CurrencyOption,
    index: Int,
    count: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = listItemShape(index, count)
    val verticalPadding = when {
        count == 1 -> PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        index == 0 -> PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(start = 16.dp, end = 16.dp, top = 1.dp, bottom = 4.dp)
        else -> PaddingValues(horizontal = 16.dp, vertical = 1.dp)
    }

    Card(
        modifier = Modifier
            .padding(verticalPadding)
            .fillMaxWidth()
            .clickable(enabled = enabled && !isSelected, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(4.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${option.symbol}  ${option.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = currencySymbol(option.code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
