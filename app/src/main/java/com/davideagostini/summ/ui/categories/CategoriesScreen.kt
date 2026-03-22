package com.davideagostini.summ.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.ui.categories.components.CategoryActionSheet
import com.davideagostini.summ.ui.categories.components.CategoryCard
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.SummColors
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    CategoriesContent(
        categories = categories,
        uiState    = uiState,
        onEvent    = viewModel::handleEvent,
        onBack     = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesContent(
    categories: List<Category>,
    uiState: CategoriesUiState,
    onEvent: (CategoriesEvent) -> Unit,
    onBack: () -> Unit,
) {
    var allowSheetHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowSheetHide },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title          = { Text(stringResource(R.string.categories_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                colors = SummColors.topBarColors,
            )

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                itemsIndexed(categories, key = { _, cat -> cat.id }) { index, category ->
                    CategoryCard(
                        category = category,
                        index    = index,
                        count    = categories.size,
                        onClick  = { onEvent(CategoriesEvent.Select(category)) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick        = { onEvent(CategoriesEvent.StartAdd) },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            shape          = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add_category))
        }
    }

    // ── Add / Action / Edit / Success sheet ───────────────────────────────────
    if (uiState.sheetMode != CategorySheetMode.Hidden) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState       = sheetState,
            containerColor   = Color.Transparent,
            dragHandle       = null,
            scrimColor       = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            CategoryActionSheet(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = {
                    scope.launch {
                        allowSheetHide = true
                        sheetState.hide()
                        onEvent(CategoriesEvent.DismissSheet)
                        allowSheetHide = false
                    }
                },
            )
        }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────────
    if (uiState.showDeleteDialog) {
        val catName = uiState.selectedCategory?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { onEvent(CategoriesEvent.DismissDeleteDialog) },
            shape            = RoundedCornerShape(20.dp),
            title            = { Text(stringResource(R.string.category_delete_title, catName), fontWeight = FontWeight.SemiBold) },
            text             = {
                Text(
                    stringResource(R.string.category_delete_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CategoriesEvent.ConfirmDelete) }) {
                    Text(stringResource(R.string.action_delete), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(CategoriesEvent.DismissDeleteDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
