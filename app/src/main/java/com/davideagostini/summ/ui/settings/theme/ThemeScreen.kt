package com.davideagostini.summ.ui.settings.theme

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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.SummColors
import com.davideagostini.summ.ui.theme.listItemShape

/**
 * Theme selection screen.
 *
 * This screen lets users preview and choose the app appearance without carrying any data logic of
 * its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val selectedTheme by AppThemeManager.currentTheme.collectAsStateWithLifecycle()
    val orderedOptions = AppThemeManager.supportedThemes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_theme_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_desc_back),
                    )
                }
            },
            colors = SummColors.topBarColors,
        )

        ThemeHeaderSection()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            itemsIndexed(orderedOptions, key = { _, option -> option.key }) { index, option ->
                ThemeListItem(
                    option = option,
                    index = index,
                    count = orderedOptions.size,
                    isSelected = option == selectedTheme,
                    onClick = { AppThemeManager.setTheme(context, option) },
                )
            }
        }
    }
}

@Composable
private fun ThemeHeaderSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_theme_screen_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ThemeListItem(
    option: SupportedAppTheme,
    index: Int,
    count: Int,
    isSelected: Boolean,
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
            .clickable(enabled = !isSelected, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
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
                    imageVector = option.icon(),
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(4.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = option.description(),
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
            }
        }
    }
}

@Composable
private fun SupportedAppTheme.label(): String = stringResource(
    when (this) {
        SupportedAppTheme.LIGHT -> R.string.settings_theme_option_light
        SupportedAppTheme.DARK -> R.string.settings_theme_option_dark
        SupportedAppTheme.SYSTEM -> R.string.settings_theme_option_system
    }
)

@Composable
private fun SupportedAppTheme.description(): String = stringResource(
    when (this) {
        SupportedAppTheme.LIGHT -> R.string.settings_theme_option_light_description
        SupportedAppTheme.DARK -> R.string.settings_theme_option_dark_description
        SupportedAppTheme.SYSTEM -> R.string.settings_theme_option_system_description
    }
)

private fun SupportedAppTheme.icon(): ImageVector = when (this) {
    SupportedAppTheme.LIGHT -> Icons.Default.LightMode
    SupportedAppTheme.DARK -> Icons.Default.DarkMode
    SupportedAppTheme.SYSTEM -> Icons.Default.BrightnessAuto
}
