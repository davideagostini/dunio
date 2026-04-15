package com.davideagostini.summ.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.davideagostini.summ.ui.categories.components.CategoryEditorScreen
import com.davideagostini.summ.ui.components.DeleteConfirmationDialog
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.theme.SummColors
import kotlinx.coroutines.launch

/**
 * Top-level screen for managing custom categories.
 *
 * The screen mostly renders the current list and bottom-sheet editor state while the ViewModel
 * handles validation and persistence.
 */
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
    var showFullScreenEditor by remember { mutableStateOf(uiState.sheetMode == CategorySheetMode.Add || uiState.sheetMode == CategorySheetMode.Edit) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowSheetHide },
    )

    // Add/Edit own the fullscreen editor flow. Success remains there too, so save never snaps back to a sheet.
    LaunchedEffect(uiState.sheetMode) {
        if (uiState.sheetMode == CategorySheetMode.Add || uiState.sheetMode == CategorySheetMode.Edit) {
            showFullScreenEditor = true
        } else if (uiState.sheetMode == CategorySheetMode.Hidden) {
            showFullScreenEditor = false
        }
    }

    val dismissFullscreenEditor: () -> Unit = {
        showFullScreenEditor = false
        scope.launch {
            kotlinx.coroutines.delay(220)
            onEvent(CategoriesEvent.DismissSheet)
        }
    }

    BackHandler(enabled = showFullScreenEditor) {
        dismissFullscreenEditor()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title          = {
                    Text(
                        stringResource(R.string.categories_title),
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

    // The action state keeps the compact sheet presentation. Fullscreen editor states never render here.
    if ((uiState.sheetMode == CategorySheetMode.Action || uiState.sheetMode == CategorySheetMode.Success) && !showFullScreenEditor) {
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

    AnimatedVisibility(
        visible = showFullScreenEditor,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        ) {
            CategoryEditorScreen(
                uiState = uiState,
                onEvent = onEvent,
                onDismiss = dismissFullscreenEditor,
            )
        }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────────
    if (uiState.showDeleteDialog) {
        val catName = uiState.selectedCategory?.name.orEmpty()
        DeleteConfirmationDialog(
            title = stringResource(R.string.category_delete_title, catName),
            message = stringResource(R.string.category_delete_message),
            isLoading = uiState.isSaving,
            onConfirm = { onEvent(CategoriesEvent.ConfirmDelete) },
            onDismiss = { onEvent(CategoriesEvent.DismissDeleteDialog) },
        )
    }
}
