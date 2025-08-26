# 🏃‍♀️ FITFO AI - Personal Fitness Coach

> **Your AI-powered personal fitness companion for running, training, and wellness**

[![Android](https://img.shields.io/badge/Android-API%2026+-green.svg)](https://developer.android.com/about/versions/android-13)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🎯 **Overview**

FITFO AI is a comprehensive Android fitness application designed for personal use, featuring an intelligent AI coach that provides personalized training plans, real-time coaching, and comprehensive fitness analytics.

### ✨ **Key Features**

- 🤖 **AI Fitness Coach** - Personalized training and nutrition advice
- 🏃‍♂️ **Run Tracking** - GPS-based run sessions with real-time metrics
- 🎵 **Music Integration** - Spotify integration for motivational playlists
- 📊 **Health Analytics** - Google Fit integration for comprehensive health data
- 🗣️ **Voice Coaching** - Real-time audio guidance during workouts
- 📱 **Modern UI** - Dark-first design with Material 3 components

## 🏗️ **Architecture**

```
FITFO AI/
├── 📱 Presentation Layer (Jetpack Compose)
│   ├── 🎨 UI Components & Theme
│   ├── 🧭 Navigation & Screens
│   └── 📊 State Management
├── 🧠 Domain Layer (Clean Architecture)
│   ├── 📋 Use Cases
│   ├── 🏷️ Models
│   └── 🔄 Repositories
├── 💾 Data Layer
│   ├── 🗄️ Room Database
│   ├── 🌐 API Integrations
│   └── 🔐 Local Storage
└── 🤖 AI Services
    ├── 🧠 Google Gemini
    ├── 🗣️ ElevenLabs TTS
    └── 🎯 Fitness Coach Agent
```

## 🚀 **Getting Started**

### **Prerequisites**

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API 26+
- JDK 21
- Kotlin 2.0.21

### **Installation**

1. **Clone the repository**
   ```bash
   git clone https://github.com/jaredschueler07/fitfoai.git
   cd fitfoai
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory
   - Click "OK"

3. **Sync and Build**
   - Wait for Gradle sync to complete
   - Click the green "Run" button (▶️)
   - Select your device/emulator

### **First Run**

1. **Welcome Screen** - Get started with the app
2. **Connect Apps** - Link Google Fit and Spotify
3. **Personalize Profile** - Set your fitness goals and experience
4. **Set Event Goal** - Choose your target race or event
5. **Dashboard** - Access your personalized fitness hub

## 📱 **Screens & Features**

### **Onboarding Flow**
- 🏠 **Welcome** - Beautiful hero design with runner emoji
- 🔗 **Connect Apps** - Google Fit & Spotify integration
- 👤 **Personalize Profile** - Comprehensive fitness profile setup
- 🎯 **Set Event Goal** - Popular races or custom goals

### **Main App**
- 📊 **Dashboard** - Today's workout, weekly progress, training plan
- 🤖 **AI Coach** - Chat interface with Fitness Coach AI Agent
- 📈 **Progress** - Detailed analytics and performance tracking
- 👤 **Profile** - User settings and preferences

## 🎨 **Design System**

- **Theme**: Dark-first with lime accent color (#84cc16)
- **Typography**: Material 3 type scale
- **Components**: Custom buttons, cards, and form fields
- **Navigation**: Bottom navigation with 4 main tabs
- **Icons**: Custom icon set for fitness and coaching

## 🤖 **AI Integration**

### **Fitness Coach AI Agent**
- **Base Model**: GPT agent ("Fitness, Workout & Diet – PhD Coach")
- **Capabilities**:
  - Adaptive training plan adjustments
  - Nutrition guidance linked to fitness goals
  - Contextual in-run coaching (HR zones, endurance advice)
  - Periodization planning (weekly/monthly strategy)
  - Explainable, patient-friendly feedback

### **Voice Coaching**
- **TTS Engine**: ElevenLabs for natural voice synthesis
- **Smart Triggers**: ML-based coaching prompts
- **Real-time Feedback**: HR zone guidance, pace coaching

## 🔧 **Technical Stack**

### **Frontend**
- **UI Framework**: Jetpack Compose
- **Navigation**: Navigation Compose
- **State Management**: StateFlow/Flow
- **Theme**: Material 3

### **Backend & Data**
- **Database**: Room (SQLite)
- **Networking**: Ktor Client
- **Dependency Injection**: Hilt (planned)
- **Serialization**: Kotlinx Serialization

### **AI & External Services**
- **AI Model**: Google Gemini API
- **Text-to-Speech**: ElevenLabs API
- **Health Data**: Google Fit REST API
- **Music**: Spotify Web API

## 📊 **Project Status**

### ✅ **Completed (Phase 1 & 2)**
- [x] Project foundation with Clean Architecture
- [x] Complete UI implementation (6 screens)
- [x] Navigation system with bottom navigation
- [x] Dark theme with Material 3 design
- [x] AI Coach chat interface
- [x] Form validation and user feedback
- [x] GitHub repository setup

### 🚧 **In Progress (Phase 3)**
- [ ] GPS location services
- [ ] Run session management
- [ ] Real-time metrics calculation
- [ ] Room database implementation
- [ ] Google Fit API integration
- [ ] Spotify API integration
- [ ] Google Gemini AI integration
- [ ] ElevenLabs TTS integration

### 📋 **Planned (Phase 4)**
- [ ] Voice coaching system
- [ ] Smart trigger engine
- [ ] Performance analytics
- [ ] Data visualizations
- [ ] Accessibility features
- [ ] Testing and optimization

## 🛠️ **Development**

### **Project Structure**
```
app/src/main/java/com/runningcoach/v2/
├── 📱 MainActivity.kt
├── 🏗️ RunningCoachApplication.kt
├── 🧠 domain/
│   └── model/
├── 📱 presentation/
│   ├── 🎨 components/
│   ├── 🧭 navigation/
│   ├── 📱 screen/
│   └── 🎨 theme/
└── 💾 data/ (planned)
```

### **Build Configuration**
- **Android Gradle Plugin**: 8.12.1
- **Kotlin**: 2.0.21
- **Java Target**: 11
- **Compose BOM**: 2024.09.00
- **Min SDK**: 26
- **Target SDK**: 36

## 🤝 **Contributing**

This is a personal project, but suggestions and feedback are welcome!

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 **Acknowledgments**

- **Google Fit API** for health data integration
- **Spotify Web API** for music integration
- **Google Gemini** for AI capabilities
- **ElevenLabs** for voice synthesis
- **Jetpack Compose** for modern UI development

---

**Built with ❤️ for personal fitness and wellness**

*FITFO AI - Your AI-powered fitness companion*
