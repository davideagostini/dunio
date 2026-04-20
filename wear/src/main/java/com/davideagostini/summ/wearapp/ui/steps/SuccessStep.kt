/**
 * Final step of the Wear quick-entry wizard: success confirmation.
 *
 * This screen is shown briefly after a successful save. It displays a
 * rotated check-mark icon inside a diamond-shaped badge, followed by
 * "Saved" text and an optional detail message (e.g. "The entry was saved
 * on your phone" or "N entry queued and will sync when your phone is back").
 *
 * The ViewModel automatically resets the flow and navigates back to the
 * type-selection screen after a 2-second delay, so this screen is transient
 * by design.
 */
package com.davideagostini.summ.wearapp.ui.steps
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.theme.WearThemeTokens

/**
 * Composable for the success-confirmation screen.
 *
 * Layout structure (vertically centred):
 * 1. Diamond-shaped badge with a check-mark icon (rotated Box + Icon).
 * 2. "Saved" title in large, semi-bold text.
 * 3. Optional detail message (e.g. queued count or saved confirmation).
 *
 * The scroll indicator is hidden because the content fits on one screen
 * and the user does not need to interact with this transient screen.
 *
 * @param message Optional detail message from the ViewModel describing
 *                the save result (immediate save vs. queued for sync).
 */
@Composable
internal fun SuccessStep(message: String?) {
    val scrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = scrollState,
        scrollIndicator = null,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 18.dp,
            ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 76.dp, height = 64.dp)
                            .graphicsLayer { rotationZ = -45f }
                            .background(
                                color = Color(0xFFBFE6FA),
                                shape = androidx.compose.foundation.shape.CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color(0xFF1C3C4E),
                            modifier = Modifier
                                .size(32.dp)
                                .graphicsLayer { rotationZ = 45f },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.wear_success),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = WearThemeTokens.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    if (!message.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = WearThemeTokens.onBackground.copy(alpha = 0.78f),
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}
