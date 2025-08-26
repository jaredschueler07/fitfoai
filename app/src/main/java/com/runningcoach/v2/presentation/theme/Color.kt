package com.runningcoach.v2.presentation.theme

import androidx.compose.ui.graphics.Color

// Dark Theme First Design (based on wireframe)
object AppColors {
    // Primary Colors - Lime Accent (#84cc16)
    val Primary = Color(0xFF84cc16)      // lime-400
    val PrimaryVariant = Color(0xFF65a30d) // lime-600
    val OnPrimary = Color(0xFF000000)    // Black text on lime
    
    // Background Colors - Dark Theme
    val Background = Color(0xFF000000)    // Black background
    val Surface = Color(0xFF121212)       // Dark surface
    val SurfaceVariant = Color(0xFF1a1a1a) // Slightly lighter surface
    
    // Neutral Colors (from wireframe)
    val Neutral900 = Color(0xFF171717)    // neutral-900 - Cards
    val Neutral800 = Color(0xFF262626)    // neutral-800 - Borders
    val Neutral700 = Color(0xFF404040)    // neutral-700 - Dividers
    val Neutral500 = Color(0xFF737373)    // neutral-500 - Secondary text
    val Neutral400 = Color(0xFF9ca3af)    // neutral-400 - Placeholder text
    val Neutral300 = Color(0xFFd1d5db)    // neutral-300 - Body text
    
    // Text Colors
    val OnBackground = Color(0xFFfafafa)  // Almost white text
    val OnSurface = Color(0xFFfafafa)     // Almost white text
    val OnSurfaceVariant = Color(0xFFd1d5db) // Neutral text
    
    // Status Colors
    val Success = Color(0xFF22c55e)       // green-500
    val Warning = Color(0xFFf59e0b)       // amber-500
    val Error = Color(0xFFef4444)         // red-500
    val Info = Color(0xFF3b82f6)          // blue-500
    
    // Card and Component Colors
    val CardBackground = Neutral900       // Card backgrounds
    val CardBorder = Neutral800           // Card borders
    val Divider = Neutral700              // Dividers and separators
    
    // Interactive Colors
    val PrimaryHover = Color(0xFF65a30d)  // lime-600 for hover states
    val SurfaceHover = Color(0xFF1f1f1f)  // Hover state for surfaces
}
