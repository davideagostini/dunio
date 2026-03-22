package com.davideagostini.summ.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
fun SignInScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
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
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    .height(80.dp)
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
        }
    }
}

@Composable
private fun SignInHeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 144.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_sign_in_headline),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
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
