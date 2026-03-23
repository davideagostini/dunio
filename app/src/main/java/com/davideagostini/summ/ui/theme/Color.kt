package com.davideagostini.summ.ui.theme

// Central color tokens for the mobile app: raw palette values plus semantic helpers used by shared components.
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light scheme tokens are kept explicit so the app can map each Material color role predictably.
val primaryLight                  = Color(0xFF006B5E)
val onPrimaryLight                = Color(0xFFFFFFFF)
val primaryContainerLight         = Color(0xFF9EF2E8)
val onPrimaryContainerLight       = Color(0xFF00201B)
val secondaryLight                = Color(0xFF4A6360)
val onSecondaryLight              = Color(0xFFFFFFFF)
val secondaryContainerLight       = Color(0xFFCCE8E4)
val onSecondaryContainerLight     = Color(0xFF05201D)
val tertiaryLight                 = Color(0xFF455E71)
val onTertiaryLight               = Color(0xFFFFFFFF)
val tertiaryContainerLight        = Color(0xFFC8E6FF)
val onTertiaryContainerLight      = Color(0xFF001D2D)
val errorLight                    = Color(0xFFBA1A1A)
val onErrorLight                  = Color(0xFFFFFFFF)
val errorContainerLight           = Color(0xFFFFDAD6)
val onErrorContainerLight         = Color(0xFF93000A)
val backgroundLight               = Color(0xFFF4FBFA)
val onBackgroundLight             = Color(0xFF161D1C)
val surfaceLight                  = Color(0xFFF4FBFA)
val onSurfaceLight                = Color(0xFF161D1C)
val surfaceVariantLight           = Color(0xFFDBE5E2)
val onSurfaceVariantLight         = Color(0xFF3F4946)
val outlineLight                  = Color(0xFF6F7976)
val outlineVariantLight           = Color(0xFFBEC9C5)
val scrimLight                    = Color(0xFF000000)
val inverseSurfaceLight           = Color(0xFF2B3130)
val inverseOnSurfaceLight         = Color(0xFFECF2F0)
val inversePrimaryLight           = Color(0xFF81D5C7)
val surfaceDimLight               = Color(0xFFD5DBDA)
val surfaceBrightLight            = Color(0xFFF4FBFA)
val surfaceContainerLowestLight   = Color(0xFFFFFFFF)
val surfaceContainerLowLight      = Color(0xFFEEF5F3)
val surfaceContainerLight         = Color(0xFFE8F0EE)
val surfaceContainerHighLight     = Color(0xFFE2EAE8)
val surfaceContainerHighestLight  = Color(0xFFDCE4E2)

// Dark scheme mirrors the light palette with contrast-safe values for the mobile-first surface system.
val primaryDark                   = Color(0xFF81D5C7)
val onPrimaryDark                 = Color(0xFF003731)
val primaryContainerDark          = Color(0xFF005148)
val onPrimaryContainerDark        = Color(0xFF9EF2E8)
val secondaryDark                 = Color(0xFFB1CCC8)
val onSecondaryDark               = Color(0xFF1C3531)
val secondaryContainerDark        = Color(0xFF324B48)
val onSecondaryContainerDark      = Color(0xFFCCE8E4)
val tertiaryDark                  = Color(0xFFABCAE3)
val onTertiaryDark                = Color(0xFF123245)
val tertiaryContainerDark         = Color(0xFF2D4759)
val onTertiaryContainerDark       = Color(0xFFC8E6FF)
val errorDark                     = Color(0xFFFFB4AB)
val onErrorDark                   = Color(0xFF690005)
val errorContainerDark            = Color(0xFF93000A)
val onErrorContainerDark          = Color(0xFFFFDAD6)
val backgroundDark                = Color(0xFF0E1514)
val onBackgroundDark              = Color(0xFFDCE4E2)
val surfaceDark                   = Color(0xFF0E1514)
val onSurfaceDark                 = Color(0xFFDCE4E2)
val surfaceVariantDark            = Color(0xFF3F4946)
val onSurfaceVariantDark          = Color(0xFFBEC9C5)
val outlineDark                   = Color(0xFF899390)
val outlineVariantDark            = Color(0xFF3F4946)
val scrimDark                     = Color(0xFF000000)
val inverseSurfaceDark            = Color(0xFFDCE4E2)
val inverseOnSurfaceDark          = Color(0xFF2B3130)
val inversePrimaryDark            = Color(0xFF006B5E)
val surfaceDimDark                = Color(0xFF0E1514)
val surfaceBrightDark             = Color(0xFF343B3A)
val surfaceContainerLowestDark    = Color(0xFF090F0E)
val surfaceContainerLowDark       = Color(0xFF161D1C)
val surfaceContainerDark          = Color(0xFF1A2120)
val surfaceContainerHighDark      = Color(0xFF252C2A)
val surfaceContainerHighestDark   = Color(0xFF2F3735)

// Semantic colors are reused for financial concepts so screens stay visually consistent.
val IncomeGreen = Color(0xFF006B5E)
val ExpenseRed  = Color(0xFFBA1A1A)

// Shared top app bar colors keep titles and navigation icons consistent across screens.
object SummColors {
    @OptIn(ExperimentalMaterial3Api::class)
    val topBarColors: TopAppBarColors
        @Composable get() = TopAppBarDefaults.topAppBarColors(
            containerColor          = colorScheme.surfaceContainer,
            scrolledContainerColor  = colorScheme.surfaceContainer,
            titleContentColor       = colorScheme.onSurface,
            navigationIconContentColor = colorScheme.onSurface,
            actionIconContentColor   = colorScheme.onSurface,
        )
}
