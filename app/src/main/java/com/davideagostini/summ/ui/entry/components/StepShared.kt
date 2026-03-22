package com.davideagostini.summ.ui.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R

@Composable
internal fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isPast   = index < currentStep
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isActive) 28.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isPast   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else     -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}

@Composable
internal fun StepTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun StepNavRow(onBack: (() -> Unit)?, onNext: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick  = onBack,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_back)) }
        }
        Button(
            onClick  = onNext,
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_next)) }
    }
}
