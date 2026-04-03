package com.davideagostini.summ.ui.auth.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.SummTheme

/**
 * Sign-in surface shown before the user joins a household.
 *
 * It is intentionally presentation-focused and receives all state from the auth gate.
 */
@Composable
fun SignInScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        SignInBackgroundGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SignInBrand()
                SignInHeroSection()
                if (!errorMessage.isNullOrBlank()) {
                    AuthErrorCard(errorMessage)
                }
            }

            Button(
                onClick = {
                    onDismissError()
                    onGoogleSignIn()
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .navigationBarsPadding(),
                shape = AppButtonShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                GoogleMark()
                Spacer(Modifier.size(10.dp))
                Text(
                    if (isSubmitting) {
                        stringResource(R.string.auth_signing_in)
                    } else {
                        stringResource(R.string.auth_continue_with_google)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { uriHandler.openUri("https://getsumm.app/privacy-policy/") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.auth_privacy_policy),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://getsumm.app/terms-of-service/") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.auth_terms_of_service),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Samsung Galaxy S25",
    showSystemUi = true,
    device = "spec:width=1080px,height=2340px,dpi=416"
)
@Preview(
    name = "Samsung Galaxy S25",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES, // <--- Change this line
    device = "spec:width=1080px,height=2340px,dpi=416"
)
@Composable
private fun SignInScreenPreview() {
    SummTheme {
        SignInScreen(
            isSubmitting = false,
            errorMessage = null,
            onDismissError = {},
            onGoogleSignIn = {}
        )
    }
}

@Composable
private fun SignInHeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_sign_in_headline),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.auth_sign_in_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SignInBackgroundGlow() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IncomeGreen.copy(alpha = 0.18f),
                        IncomeGreen.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                    radius = 920f,
                    center = Offset(520f, 120f),
                ),
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IncomeGreen.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    radius = 780f,
                    center = Offset(260f, 360f),
                ),
            )
    )
}

@Composable
private fun GoogleMark() {
    Surface(
        modifier = Modifier.size(22.dp),
        shape = MaterialTheme.shapes.small,
        color = Color.White.copy(alpha = 0.96f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "G",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4),
            )
        }
    }
}
