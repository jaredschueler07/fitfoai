package com.runningcoach.v2.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dark Color Scheme (Primary theme based on wireframe)
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    primaryContainer = AppColors.PrimaryVariant,
    onPrimaryContainer = AppColors.OnPrimary,
    
    secondary = AppColors.Neutral700,
    onSecondary = AppColors.OnSurface,
    secondaryContainer = AppColors.Neutral800,
    onSecondaryContainer = AppColors.OnSurface,
    
    tertiary = AppColors.Info,
    onTertiary = AppColors.OnPrimary,
    
    background = AppColors.Background,
    onBackground = AppColors.OnBackground,
    
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.OnSurfaceVariant,
    
    error = AppColors.Error,
    onError = AppColors.OnPrimary,
    errorContainer = AppColors.Error,
    onErrorContainer = AppColors.OnPrimary,
    
    outline = AppColors.Neutral700,
    outlineVariant = AppColors.Neutral800
)

// Light Color Scheme (fallback, but app is dark-first)
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    primaryContainer = AppColors.PrimaryVariant,
    onPrimaryContainer = AppColors.OnPrimary,
    
    secondary = AppColors.Neutral700,
    onSecondary = AppColors.OnBackground,
    
    background = AppColors.OnBackground,
    onBackground = AppColors.Background,
    
    surface = AppColors.OnSurface,
    onSurface = AppColors.Background,
    
    error = AppColors.Error,
    onError = AppColors.OnPrimary
)

@Composable
fun RunningCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Force dark theme as per PRD (dark theme first design)
    forceDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        forceDarkTheme || darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
