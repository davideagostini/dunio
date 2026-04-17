package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.theme.WearThemeTokens

@Composable
internal fun SuccessStep(message: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 76.dp)
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
                        .size(38.dp)
                        .graphicsLayer { rotationZ = 45f },
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.wear_success),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = WearThemeTokens.onBackground.copy(alpha = 0.78f),
                )
            }
        }
    }
}
