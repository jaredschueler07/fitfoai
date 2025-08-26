# RunningCoachV2 Wireframe Reference Guide

## Overview

The `project/runningcoach-ai` folder contains a **React/TypeScript web application** that serves as a comprehensive wireframe and UI reference for the RunningCoachV2 Android app. This document provides detailed analysis and implementation guidance based on the wireframe code.

## Project Structure

### Technology Stack
- **Framework**: React 19.1.1 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS (utility classes)
- **Architecture**: Component-based with TypeScript interfaces

### File Organization
```
project/runningcoach-ai/
├── App.tsx                 # Main application component
├── types.ts               # TypeScript interfaces and enums
├── components/
│   ├── WelcomeScreen.tsx
│   ├── ConnectAppsScreen.tsx
│   ├── PersonalizeProfileScreen.tsx
│   ├── SetEventGoalScreen.tsx
│   ├── DashboardScreen.tsx
│   ├── FitnessGPTScreen.tsx
│   ├── BottomNavBar.tsx
│   ├── ProgressChart.tsx
│   └── icons/             # 15+ SVG icon components
└── package.json
```

## Application Flow

### Screen Navigation
1. **Welcome Screen** → App introduction and onboarding start
2. **Connect Apps** → Integration with Fitbit, Google Fit, Spotify
3. **Personalize Profile** → User profile setup (age, height, weight)
4. **Set Event Goal** → Race goal configuration
5. **Dashboard** → Main dashboard with workout tracking
6. **FitnessGPT** → AI coach interface

### Bottom Navigation
- **Home** (Dashboard)
- **AI Coach** (FitnessGPT)
- **Progress** (Placeholder)
- **Profile** (Placeholder)

## Design System

### Color Palette
```css
/* Primary Colors */
--background-dark: #121212
--background-black: #000000
--accent-lime: #84cc16 (lime-400)
--accent-lime-hover: #a3e635 (lime-300)

/* Neutral Colors */
--neutral-800: #262626
--neutral-900: #171717
--neutral-700: #404040
--neutral-500: #737373
--neutral-400: #a3a3a3

/* Text Colors */
--text-white: #ffffff
--text-neutral-400: #a3a3a3
--text-neutral-500: #737373
```

### Typography
- **Font Family**: System sans-serif
- **Header**: `text-3xl font-bold`
- **Subheader**: `text-lg font-semibold`
- **Body**: `text-sm`
- **Mono**: `font-mono` (for time displays)

### Spacing & Layout
- **Container Padding**: `p-6`
- **Section Margins**: `mt-8`, `mb-3`
- **Card Padding**: `p-6`
- **Button Padding**: `py-3 px-5` (regular), `py-4 px-6` (large)

### Component Patterns

#### Cards
```css
bg-neutral-900 border border-neutral-800 rounded-2xl p-6
```

#### Buttons
```css
/* Primary Button */
bg-lime-400 text-black font-bold py-3 px-5 rounded-full

/* Disabled Button */
bg-neutral-600 disabled:cursor-not-allowed
```

#### Input Fields
```css
bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 pl-12 pr-4
focus:outline-none focus:ring-2 focus:ring-lime-400
```

## Data Models

### Core Interfaces

```typescript
enum AppScreen {
  Welcome = 'WELCOME',
  ConnectApps = 'CONNECT_APPS',
  PersonalizeProfile = 'PERSONALIZE_PROFILE',
  SetEventGoal = 'SET_EVENT_GOAL',
  Dashboard = 'DASHBOARD',
  FitnessGPT = 'FITNESS_GPT',
}

enum FitnessLevel {
  Beginner = 'Beginner',
  Intermediate = 'Intermediate',
  Advanced = 'Advanced',
}

enum RunningGoal {
  FiveK = '5K',
  TenK = '10K',
  HalfMarathon = 'Half Marathon',
  Marathon = 'Marathon',
}

interface Coach {
  id: string;
  name: string;
  style: string;
}

interface RaceGoal {
  name: string;
  date: string;
  distance: string;
  targetTime?: string;
}

type ConnectedApp = 'fitbit' | 'google_fit' | 'spotify';

interface UserProfile {
  name: string;
  age?: number;
  height?: string;
  weight?: string;
  fitnessLevel?: FitnessLevel;
  connectedApps: ConnectedApp[];
  goal?: RaceGoal;
  coach: Coach;
}
```

## Key Screen Implementations

### Dashboard Screen

**Layout Structure:**
1. **Header**: Welcome message + user avatar
2. **Today's Guided Run Card**: Coach info + start button
3. **Weekly Activity Chart**: Progress visualization
4. **Training Plan**: Current week's schedule
5. **Past Workouts**: Recent activity list

**Key Features:**
- Dynamic coach assignment
- Real-time workout tracking
- Progress visualization
- Training plan display

### Onboarding Flow

**Multi-step Process:**
1. **Name Input**: Text field with user icon
2. **Fitness Level**: Dropdown selection
3. **Running Goals**: Goal type selection
4. **Coach Selection**: Coach personality choice

**Form Validation:**
- Required field validation
- Progressive disclosure
- State management between steps

### Connect Apps Screen

**Integration Options:**
- **Fitbit**: Health data sync
- **Google Fit**: Activity tracking
- **Spotify**: Music integration

**UI Pattern:**
- App cards with icons
- Connection status indicators
- Skip option for later setup

## Icon System

### Available Icons (15+ components)
- `CalendarIcon.tsx` - Date/time related
- `ChartIcon.tsx` - Progress/analytics
- `ChatIcon.tsx` - Communication
- `CoachIcon.tsx` - Coach-related features
- `FitbitIcon.tsx` - Fitbit integration
- `GoogleFitIcon.tsx` - Google Fit integration
- `HomeIcon.tsx` - Navigation
- `ProfileIcon.tsx` - User profile
- `SettingsIcon.tsx` - App settings
- `SpotifyIcon.tsx` - Music integration
- `UserIcon.tsx` - User-related features
- `PlusIcon.tsx` - Add/create actions
- `GoalIcon.tsx` - Goal setting
- `SparklesIcon.tsx` - AI/features
- `SendIcon.tsx` - Communication

## Android Implementation Guidance

### Jetpack Compose Conversion

#### Color System
```kotlin
val Lime400 = Color(0xFF84CC16)
val Lime300 = Color(0xFFA3E635)
val Neutral800 = Color(0xFF262626)
val Neutral900 = Color(0xFF171717)
val Neutral700 = Color(0xFF404040)
val Neutral500 = Color(0xFF737373)
val Neutral400 = Color(0xFFA3A3A3)
```

#### Typography
```kotlin
val Typography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        color = Neutral400
    )
)
```

#### Component Patterns
```kotlin
// Card Component
@Composable
fun CoachCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Neutral900
        ),
        border = BorderStroke(1.dp, Neutral800),
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

// Primary Button
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime400,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
    }
}
```

### Navigation Implementation

#### Bottom Navigation
```kotlin
@Composable
fun BottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Neutral900.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        listOf(
            "home" to "Home",
            "ai_coach" to "AI Coach", 
            "progress" to "Progress",
            "profile" to "Profile"
        ).forEach { (route, title) ->
            NavigationBarItem(
                icon = { /* Icon component */ },
                label = { Text(title) },
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Lime400,
                    selectedTextColor = Lime400,
                    unselectedIconColor = Neutral500,
                    unselectedTextColor = Neutral500
                )
            )
        }
    }
}
```

## Implementation Priorities

### Phase 1: Core Screens
1. **Welcome Screen** - App introduction
2. **Dashboard** - Main user interface
3. **Bottom Navigation** - App navigation

### Phase 2: Onboarding
1. **Connect Apps** - Third-party integrations
2. **Personalize Profile** - User setup
3. **Set Event Goal** - Goal configuration

### Phase 3: Advanced Features
1. **FitnessGPT Screen** - AI coach interface
2. **Progress Tracking** - Analytics and charts
3. **Profile Management** - User settings

## Design Principles

1. **Dark Theme First** - Consistent dark UI throughout
2. **Lime Accent** - Use lime-400 for primary actions
3. **Card-based Layout** - Organize content in cards
4. **Rounded Corners** - 16dp radius for cards, 50% for buttons
5. **Consistent Spacing** - 24dp between sections, 16dp within cards
6. **Typography Hierarchy** - Clear text sizing and weights
7. **Icon Consistency** - Use consistent icon style and sizing

## Notes

- The wireframe uses Tailwind CSS classes that need to be converted to Android equivalents
- All measurements are in pixels and need conversion to dp/sp
- The React state management patterns can inform Android ViewModel design
- The TypeScript interfaces provide excellent guidance for Kotlin data classes
- The component structure can guide Android Compose component organization

This wireframe reference provides a solid foundation for implementing a modern, user-friendly running coach app with a cohesive design system.
