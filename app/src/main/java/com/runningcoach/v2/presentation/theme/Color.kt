package com.runningcoach.v2.presentation.theme

import androidx.compose.ui.graphics.Color

// Athletic Blue Gradient Theme with Coral Accents
object AppColors {
    // Athletic Blue Gradient Colors
    val DeepBlue = Color(0xFF1e3a5f)     // Deep blue gradient start
    val AthleteBlue = Color(0xFF4a7c97)   // Deep blue gradient end
    val CoralAccent = Color(0xFFff6b6b)   // Coral accent primary
    val CoralAccentSecondary = Color(0xFFff8c42) // Coral accent secondary
    
    // Primary Colors - Athletic Blue with Coral Accent
    val Primary = CoralAccent            // Coral for primary actions
    val PrimaryVariant = CoralAccentSecondary // Secondary coral
    val OnPrimary = Color(0xFFffffff)    // White text on coral
    
    // Background Colors - Athletic Blue Gradient Theme
    val Background = DeepBlue             // Deep blue background
    val Surface = Color(0xFF2d4a6b)       // Athletic blue surface
    val SurfaceVariant = Color(0xFF3a5a7a) // Lighter athletic blue surface
    val GradientStart = DeepBlue          // For gradient backgrounds
    val GradientEnd = AthleteBlue         // For gradient backgrounds
    
    // Neutral Colors for Athletic Blue Theme
    val Neutral900 = Color(0xFF1a2f47)    // Dark blue-tinted cards
    val Neutral800 = Color(0xFF2a4661)    // Blue-tinted borders
    val Neutral700 = Color(0xFF3a5a7a)    // Blue-tinted dividers
    val Neutral500 = Color(0xFF6b8ba8)    // Blue-tinted secondary text
    val Neutral400 = Color(0xFF8ba3bd)    // Blue-tinted placeholder text
    val Neutral300 = Color(0xFFb8c9d9)    // Blue-tinted body text
    
    // Text Colors
    val OnBackground = Color(0xFFfafafa)  // Almost white text
    val OnSurface = Color(0xFFfafafa)     // Almost white text
    val OnSurfaceVariant = Color(0xFFd1d5db) // Neutral text
    
    // Status Colors
    val Success = Color(0xFF22c55e)       // green-500
    val Warning = Color(0xFFf59e0b)       // amber-500
    val Error = Color(0xFFef4444)         // red-500
    val Info = Color(0xFF3b82f6)          // blue-500
    
    // Card and Component Colors - Athletic Theme
    val CardBackground = Neutral900       // Athletic blue card backgrounds
    val CardBorder = Neutral800           // Athletic blue card borders
    val Divider = Neutral700              // Athletic blue dividers and separators
    
    // Interactive Colors - Athletic Theme
    val PrimaryHover = CoralAccentSecondary // Coral hover states
    val SurfaceHover = Color(0xFF3f5d7a)    // Athletic blue hover state
    
    // GPS Status Colors
    val GPSExcellent = Color(0xFF22c55e)   // Green for excellent signal
    val GPSGood = Color(0xFFf59e0b)        // Amber for good signal
    val GPSFair = Color(0xFFf97316)        // Orange for fair signal
    val GPSPoor = Error                    // Red for poor signal
    val GPSInactive = Neutral500           // Gray for inactive
}
