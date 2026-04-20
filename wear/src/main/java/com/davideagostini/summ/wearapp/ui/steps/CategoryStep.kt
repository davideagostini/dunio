/**
 * Third step of the Wear quick-entry wizard: choose a category.
 *
 * This screen is reused in two modes controlled by [WearQuickEntryUiState.showAllCategories]:
 * - **Quick mode** (default): shows the top 3 most-used categories from
 *   [WearQuickEntryUiState.quickCategories] plus an "All categories" button.
 * - **Full mode**: shows the complete [WearQuickEntryUiState.categories] list.
 *
 * While categories are loading, a [CircularProgressIndicator] is shown.
 * If loading fails or the list is empty, an error/empty message and a retry
 * button are displayed instead.
 *
 * Tapping a category dispatches [WearQuickEntryAction.SelectCategory] which
 * advances the flow to the confirmation screen.
 */
package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryUiState
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.BackTextButton
import com.davideagostini.summ.wearapp.ui.WearActionButton

/**
 * Composable for the category-selection screen.
 *
 * Layout structure (top to bottom):
 * 1. Title text (changes between "Choose category" and "All categories").
 * 2. Hint text (only in quick mode).
 * 3. Category content rendered by a `when` block:
 *    - **Loading**: centered [CircularProgressIndicator].
 *    - **Quick categories**: top-3 buttons + "All categories" link.
 *    - **Full categories**: all category buttons.
 *    - **Empty / error**: message text + retry button.
 * 4. [BackTextButton] to navigate back.
 *
 * A [LaunchedEffect] scrolls to the top when switching from quick to
 * full-category mode so the user sees the beginning of the full list.
 *
 * @param uiState  Current UI state with categories, loading, and error info.
 * @param onAction Callback to dispatch user actions to the ViewModel.
 * @param onBack   Callback invoked when the user presses the back button.
 */
@Composable
internal fun CategoryStep(
    uiState: WearQuickEntryUiState,
    onAction: (WearQuickEntryAction) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    LaunchedEffect(uiState.showAllCategories) {
        if (uiState.showAllCategories) {
            state.scrollToItem(0)
        }
    }
    ScreenScaffold(
        scrollState = state,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = contentPadding.calculateTopPadding() + WearStepTopInset,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = if (uiState.showAllCategories) {
                        stringResource(R.string.wear_all_categories_title)
                    } else {
                        stringResource(R.string.wear_categories_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = WearThemeTokens.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }

            if (!uiState.showAllCategories) {
                item {
                    Text(
                        text = stringResource(R.string.wear_categories_hint),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            when {
                uiState.isLoadingCategories -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                !uiState.showAllCategories && uiState.quickCategories.isNotEmpty() -> {
                    uiState.quickCategories.forEach { category ->
                        item {
                            WearActionButton(
                                label = category.name,
                                secondaryLabel = null,
                                iconEmoji = category.emoji,
                                onClick = { onAction(WearQuickEntryAction.SelectCategory(category)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WearThemeTokens.surfaceContainerHigh,
                                    contentColor = WearThemeTokens.onBackground,
                                    secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                                    iconColor = WearThemeTokens.onBackground,
                                ),
                            )
                        }
                    }
                    item {
                        WearActionButton(
                            label = stringResource(R.string.wear_show_all_categories),
                            secondaryLabel = null,
                            centeredLabel = true,
                            onClick = { onAction(WearQuickEntryAction.ShowAllCategories) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                secondaryContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }

                uiState.categories.isNotEmpty() -> {
                    uiState.categories.forEach { category ->
                        item {
                            WearActionButton(
                                label = category.name,
                                secondaryLabel = null,
                                iconEmoji = category.emoji,
                                onClick = { onAction(WearQuickEntryAction.SelectCategory(category)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WearThemeTokens.surfaceContainerHigh,
                                    contentColor = WearThemeTokens.onBackground,
                                    secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                                    iconColor = WearThemeTokens.onBackground,
                                ),
                            )
                        }
                    }
                }

                else -> {
                    item {
                        Text(
                            text = uiState.errorMessage ?: stringResource(R.string.wear_categories_empty),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = if (uiState.errorMessage != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                WearThemeTokens.onBackground.copy(alpha = 0.72f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        WearActionButton(
                            label = stringResource(R.string.wear_retry),
                            secondaryLabel = null,
                            onClick = { onAction(WearQuickEntryAction.RetryCategories) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WearThemeTokens.surfaceContainerHigh,
                                contentColor = WearThemeTokens.onBackground,
                                secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                                iconColor = WearThemeTokens.onBackground,
                            ),
                        )
                    }
                }
            }

            item {
                BackTextButton(onClick = onBack)
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
