# 🎨 RunningCoach App - Image Handling & Visual Design

## 📋 Overview

This document outlines the comprehensive image handling, visual design system, and asset management strategy for the RunningCoach app. It covers UI assets, coach personalities, design tokens, and visual guidelines.

## 🎨 Design System

### Material 3 Integration

#### Color Palette
```kotlin
object RunningCoachColors {
    // Primary Colors
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)
    
    // Secondary Colors
    val Secondary = Color(0xFF625B71)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1D192B)
    
    // Coach-Specific Colors
    val CoachBennett = Color(0xFF1976D2) // Blue
    val CoachMariana = Color(0xFF388E3C) // Green
    val CoachBecs = Color(0xFFFF9800) // Orange
    val CoachGoggins = Color(0xFFD32F2F) // Red
    
    // Background Colors
    val Background = Color(0xFFFFFBFE)
    val OnBackground = Color(0xFF1C1B1F)
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1C1B1F)
    
    // Error Colors
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)
}
```

#### Typography Scale
```kotlin
object RunningCoachTypography {
    val DisplayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        fontWeight = FontWeight.Normal
    )
    
    val DisplayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Normal
    )
    
    val DisplaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Normal
    )
    
    val HeadlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Normal
    )
    
    val HeadlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Normal
    )
    
    val HeadlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Normal
    )
    
    val TitleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Normal
    )
    
    val TitleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        fontWeight = FontWeight.Medium
    )
    
    val TitleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Medium
    )
    
    val BodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Normal
    )
    
    val BodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        fontWeight = FontWeight.Normal
    )
    
    val BodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontWeight = FontWeight.Normal
    )
    
    val LabelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Medium
    )
    
    val LabelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Medium
    )
    
    val LabelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Medium
    )
}
```

#### Shape System
```kotlin
object RunningCoachShapes {
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    
    val Circular = CircleShape
    val Pill = RoundedCornerShape(50)
}
```

## 👥 Coach Personality Visual System

### Coach Bennett (Enthusiastic & Philosophical)

#### Color Theme
```kotlin
object CoachBennettTheme {
    val Primary = Color(0xFF1976D2) // Warm Blue
    val PrimaryVariant = Color(0xFF1565C0)
    val Secondary = Color(0xFF42A5F5) // Light Blue
    val Background = Color(0xFFF5F9FF) // Very Light Blue
    val Surface = Color(0xFFFFFFFF)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFF000000)
    val OnBackground = Color(0xFF1A237E)
    val OnSurface = Color(0xFF1A237E)
}
```

#### Visual Elements
- **Avatar**: Warm, friendly face with encouraging smile
- **Background Pattern**: Subtle geometric patterns with blue tones
- **Icons**: Rounded, approachable iconography
- **Typography**: Friendly, readable fonts with slight warmth
- **Animations**: Smooth, encouraging transitions

#### Asset Specifications
```
Coach Bennett Avatar:
- Size: 120x120dp (480x480px @4x)
- Format: PNG with transparency
- Style: Warm, friendly, encouraging
- Background: Circular with blue gradient
- Expression: Smiling, approachable

Coach Bennett Background:
- Size: 360x200dp (1440x800px @4x)
- Format: PNG with transparency
- Style: Subtle geometric patterns
- Colors: Blue gradient with warm undertones
- Opacity: 15% overlay
```

### Mariana Fernández (Calming & Empowering)

#### Color Theme
```kotlin
object CoachMarianaTheme {
    val Primary = Color(0xFF388E3C) // Nature Green
    val PrimaryVariant = Color(0xFF2E7D32)
    val Secondary = Color(0xFF66BB6A) // Light Green
    val Background = Color(0xFFF1F8E9) // Very Light Green
    val Surface = Color(0xFFFFFFFF)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFF000000)
    val OnBackground = Color(0xFF1B5E20)
    val OnSurface = Color(0xFF1B5E20)
}
```

#### Visual Elements
- **Avatar**: Calm, confident face with gentle smile
- **Background Pattern**: Organic, nature-inspired patterns
- **Icons**: Flowing, organic iconography
- **Typography**: Serif fonts for elegance
- **Animations**: Gentle, flowing transitions

#### Asset Specifications
```
Coach Mariana Avatar:
- Size: 120x120dp (480x480px @4x)
- Format: PNG with transparency
- Style: Calm, confident, empowering
- Background: Circular with green gradient
- Expression: Gentle smile, confident eyes

Coach Mariana Background:
- Size: 360x200dp (1440x800px @4x)
- Format: PNG with transparency
- Style: Organic, nature-inspired patterns
- Colors: Green gradient with earth tones
- Opacity: 12% overlay
```

### Becs Gentry (High-Energy & Assertive)

#### Color Theme
```kotlin
object CoachBecsTheme {
    val Primary = Color(0xFFFF9800) // Vibrant Orange
    val PrimaryVariant = Color(0xFFF57C00)
    val Secondary = Color(0xFFFFB74D) // Light Orange
    val Background = Color(0xFFFFF3E0) // Very Light Orange
    val Surface = Color(0xFFFFFFFF)
    val OnPrimary = Color(0xFF000000)
    val OnSecondary = Color(0xFF000000)
    val OnBackground = Color(0xFFE65100)
    val OnSurface = Color(0xFFE65100)
}
```

#### Visual Elements
- **Avatar**: Dynamic, energetic face with determined expression
- **Background Pattern**: Bold, geometric patterns
- **Icons**: Sharp, dynamic iconography
- **Typography**: Bold, energetic fonts
- **Animations**: Quick, dynamic transitions

#### Asset Specifications
```
Coach Becs Avatar:
- Size: 120x120dp (480x480px @4x)
- Format: PNG with transparency
- Style: Dynamic, energetic, determined
- Background: Circular with orange gradient
- Expression: Determined, energetic, focused

Coach Becs Background:
- Size: 360x200dp (1440x800px @4x)
- Format: PNG with transparency
- Style: Bold, geometric patterns
- Colors: Orange gradient with high contrast
- Opacity: 18% overlay
```

### David Goggins (Brutal & Uncompromising)

#### Color Theme
```kotlin
object CoachGogginsTheme {
    val Primary = Color(0xFFD32F2F) // Aggressive Red
    val PrimaryVariant = Color(0xFFC62828)
    val Secondary = Color(0xFFEF5350) // Light Red
    val Background = Color(0xFFFFEBEE) // Very Light Red
    val Surface = Color(0xFFFFFFFF)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFF000000)
    val OnBackground = Color(0xFFB71C1C)
    val OnSurface = Color(0xFFB71C1C)
}
```

#### Visual Elements
- **Avatar**: Intense, focused face with determined expression
- **Background Pattern**: Sharp, angular patterns
- **Icons**: Bold, aggressive iconography
- **Typography**: Heavy, impactful fonts
- **Animations**: Sharp, impactful transitions

#### Asset Specifications
```
Coach Goggins Avatar:
- Size: 120x120dp (480x480px @4x)
- Format: PNG with transparency
- Style: Intense, focused, determined
- Background: Circular with red gradient
- Expression: Intense, focused, uncompromising

Coach Goggins Background:
- Size: 360x200dp (1440x800px @4x)
- Format: PNG with transparency
- Style: Sharp, angular patterns
- Colors: Red gradient with high contrast
- Opacity: 20% overlay
```

## 🖼️ Asset Management

### Asset Organization Structure
```
res/
├── drawable/
│   ├── coaches/
│   │   ├── bennett/
│   │   │   ├── avatar.png
│   │   │   ├── background.png
│   │   │   └── icon.png
│   │   ├── mariana/
│   │   │   ├── avatar.png
│   │   │   ├── background.png
│   │   │   └── icon.png
│   │   ├── becs/
│   │   │   ├── avatar.png
│   │   │   ├── background.png
│   │   │   └── icon.png
│   │   └── goggins/
│   │       ├── avatar.png
│   │       ├── background.png
│   │       └── icon.png
│   ├── icons/
│   │   ├── navigation/
│   │   ├── actions/
│   │   ├── metrics/
│   │   └── achievements/
│   ├── backgrounds/
│   │   ├── gradients/
│   │   ├── patterns/
│   │   └── overlays/
│   └── illustrations/
│       ├── onboarding/
│       ├── achievements/
│       └── empty_states/
├── mipmap/
│   ├── ic_launcher.png
│   ├── ic_launcher_round.png
│   └── ic_launcher_foreground.png
└── values/
    ├── colors.xml
    ├── themes.xml
    └── strings.xml
```

### Asset Naming Convention
```
Format: {category}_{name}_{size}_{density}.{extension}

Examples:
- coach_bennett_avatar_120dp.png
- icon_play_action_24dp.png
- background_gradient_primary_360dp.png
- illustration_onboarding_welcome_200dp.png
- pattern_geometric_benett_360dp.png
```

### Asset Specifications

#### Coach Avatars
```
Size: 120x120dp (480x480px @4x)
Format: PNG with transparency
Background: Circular with coach-specific gradient
Style: Consistent with coach personality
Quality: High resolution, crisp edges
File Size: <100KB per avatar
```

#### Background Patterns
```
Size: 360x200dp (1440x800px @4x)
Format: PNG with transparency
Style: Coach-specific patterns
Opacity: 12-20% overlay
Quality: Subtle, non-distracting
File Size: <200KB per pattern
```

#### Icons
```
Size: 24x24dp (96x96px @4x)
Format: PNG with transparency
Style: Material Design 3
Color: Adaptive to theme
Quality: Crisp, scalable
File Size: <50KB per icon
```

#### Illustrations
```
Size: 200x200dp (800x800px @4x)
Format: PNG with transparency
Style: Consistent with app design
Color: Coach-specific themes
Quality: High resolution
File Size: <300KB per illustration
```

## 🎭 Dynamic Theming System

### Theme Implementation
```kotlin
@Composable
fun RunningCoachTheme(
    coachPersonality: CoachPersonality = CoachPersonality.BENNETT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (coachPersonality) {
        CoachPersonality.BENNETT -> if (darkTheme) CoachBennettDarkColors else CoachBennettLightColors
        CoachPersonality.MARIANA -> if (darkTheme) CoachMarianaDarkColors else CoachMarianaLightColors
        CoachPersonality.BECS -> if (darkTheme) CoachBecsDarkColors else CoachBecsLightColors
        CoachPersonality.GOGGINS -> if (darkTheme) CoachGogginsDarkColors else CoachGogginsLightColors
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RunningCoachTypography,
        shapes = RunningCoachShapes,
        content = content
    )
}
```

### Coach-Specific Color Schemes
```kotlin
object CoachBennettLightColors {
    val primary = Color(0xFF1976D2)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFE3F2FD)
    val onPrimaryContainer = Color(0xFF0D47A1)
    val secondary = Color(0xFF42A5F5)
    val onSecondary = Color(0xFF000000)
    val secondaryContainer = Color(0xFFE1F5FE)
    val onSecondaryContainer = Color(0xFF01579B)
    val background = Color(0xFFF5F9FF)
    val onBackground = Color(0xFF1A237E)
    val surface = Color(0xFFFFFFFF)
    val onSurface = Color(0xFF1A237E)
}

object CoachBennettDarkColors {
    val primary = Color(0xFF90CAF9)
    val onPrimary = Color(0xFF000000)
    val primaryContainer = Color(0xFF1976D2)
    val onPrimaryContainer = Color(0xFFFFFFFF)
    val secondary = Color(0xFF81C784)
    val onSecondary = Color(0xFF000000)
    val secondaryContainer = Color(0xFF388E3C)
    val onSecondaryContainer = Color(0xFFFFFFFF)
    val background = Color(0xFF0D47A1)
    val onBackground = Color(0xFFE3F2FD)
    val surface = Color(0xFF1565C0)
    val onSurface = Color(0xFFE3F2FD)
}
```

## 🎨 UI Component Assets

### Navigation Icons
```
Dashboard Icon:
- Style: Home with running shoe
- Colors: Adaptive to theme
- Size: 24x24dp
- Animation: Subtle bounce on selection

Analytics Icon:
- Style: Chart with trend line
- Colors: Adaptive to theme
- Size: 24x24dp
- Animation: Smooth scale on selection

Plans Icon:
- Style: Calendar with checkmark
- Colors: Adaptive to theme
- Size: 24x24dp
- Animation: Checkmark animation on selection

Tracking Icon:
- Style: GPS location with pulse
- Colors: Adaptive to theme
- Size: 24x24dp
- Animation: Pulse animation when active

Profile Icon:
- Style: Person with settings
- Colors: Adaptive to theme
- Size: 24x24dp
- Animation: Rotation on selection
```

### Action Icons
```
Play Button:
- Style: Circular with play triangle
- Colors: Coach-specific primary
- Size: 48x48dp
- Animation: Scale and color transition

Pause Button:
- Style: Circular with pause bars
- Colors: Coach-specific secondary
- Size: 48x48dp
- Animation: Scale and color transition

Stop Button:
- Style: Circular with stop square
- Colors: Coach-specific error
- Size: 48x48dp
- Animation: Scale and color transition

Settings Icon:
- Style: Gear with coach accent
- Colors: Adaptive to theme
- Size: 24x24dp
- Animation: Rotation on selection
```

### Achievement Badges
```
Distance Badges:
- 5K: Bronze, silver, gold variants
- 10K: Bronze, silver, gold variants
- Half Marathon: Bronze, silver, gold variants
- Marathon: Bronze, silver, gold variants

Streak Badges:
- 7 days: Bronze flame
- 30 days: Silver flame
- 100 days: Gold flame
- 365 days: Diamond flame

Pace Badges:
- Personal Best: Star with time
- Consistent: Clock with checkmark
- Improvement: Trending up arrow

Style: Coach-specific colors and themes
Size: 64x64dp
Animation: Celebration animation on unlock
```

## 📱 Responsive Design Assets

### Screen Size Adaptations
```
Phone (320-480dp):
- Coach avatars: 80x80dp
- Background patterns: 240x120dp
- Icons: 20x20dp
- Illustrations: 120x120dp

Tablet (600-840dp):
- Coach avatars: 120x120dp
- Background patterns: 360x200dp
- Icons: 24x24dp
- Illustrations: 200x200dp

Large Tablet (840dp+):
- Coach avatars: 160x160dp
- Background patterns: 480x240dp
- Icons: 32x32dp
- Illustrations: 280x280dp
```

### Density Support
```
mdpi (1x):
- Base resolution assets
- File size optimization
- Quality: Good

hdpi (1.5x):
- 1.5x scaled assets
- File size: Moderate
- Quality: Better

xhdpi (2x):
- 2x scaled assets
- File size: Higher
- Quality: High

xxhdpi (3x):
- 3x scaled assets
- File size: Highest
- Quality: Excellent

xxxhdpi (4x):
- 4x scaled assets
- File size: Maximum
- Quality: Premium
```

## 🎬 Animation Assets

### Coach Selection Animations
```
Avatar Transition:
- Duration: 300ms
- Easing: EaseInOut
- Effect: Scale and fade
- Coach-specific color transition

Background Transition:
- Duration: 500ms
- Easing: EaseInOut
- Effect: Cross-fade with pattern morph
- Coach-specific pattern animation

Theme Transition:
- Duration: 400ms
- Easing: EaseInOut
- Effect: Color interpolation
- Smooth UI element transitions
```

### Run Tracking Animations
```
GPS Pulse:
- Duration: 2000ms
- Easing: Linear
- Effect: Expanding circle
- Color: Coach-specific primary

Pace Indicator:
- Duration: 1000ms
- Easing: EaseInOut
- Effect: Smooth value changes
- Color: Adaptive to performance

Progress Bar:
- Duration: 500ms
- Easing: EaseOut
- Effect: Smooth progress updates
- Color: Coach-specific gradient
```

## 🔧 Asset Optimization

### Image Compression
```
PNG Optimization:
- Tool: pngquant
- Quality: 85-95%
- Colors: 256 max
- File size reduction: 60-80%

WebP Conversion:
- Format: WebP with PNG fallback
- Quality: 85-95%
- File size reduction: 25-35%
- Browser support: Modern Android

Vector Graphics:
- Format: SVG for simple graphics
- Conversion: VectorDrawable for Android
- File size: Minimal
- Scalability: Perfect
```

### Asset Loading Strategy
```
Lazy Loading:
- Coach assets: Load on selection
- Background patterns: Preload active coach
- Icons: Load on demand
- Illustrations: Cache after first use

Caching Strategy:
- Coach avatars: Persistent cache
- Background patterns: Memory cache
- Icons: Disk cache
- Illustrations: Hybrid cache

Preloading:
- Active coach assets: Immediate
- Adjacent coach assets: Background
- Common icons: App startup
- Critical illustrations: Preload
```

## 📊 Asset Performance Metrics

### File Size Targets
```
Coach Avatars: <100KB each
Background Patterns: <200KB each
Icons: <50KB each
Illustrations: <300KB each
Total App Assets: <5MB
Initial Download: <2MB
```

### Loading Performance
```
Avatar Load Time: <100ms
Background Load Time: <200ms
Icon Load Time: <50ms
Illustration Load Time: <300ms
Theme Switch Time: <150ms
```

### Memory Usage
```
Active Coach Assets: <2MB
Cached Assets: <10MB
Total Memory Usage: <15MB
Memory Cleanup: Automatic
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
