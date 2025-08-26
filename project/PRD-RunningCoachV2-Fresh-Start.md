# 🏃‍♀️ RunningCoachV2 - Product Requirements Document (PRD)
## Fresh Start Implementation Plan

---

**Document Version**: 1.1  
**Created**: January 2025  
**Status**: DRAFT - Ready for Implementation  
**Priority**: HIGH  
**Usage**: Personal Use Only  

---

## 📋 Executive Summary

This PRD outlines the complete rebuild of the RunningCoachV2 Android application from scratch, incorporating lessons learned from previous development challenges, build issues, and version compatibility problems. The app will be a comprehensive AI-powered running coach with third-party integrations, real-time voice coaching, advanced analytics, and a new **Fitness Coach AI Agent** trained on personal app data.

## 🎯 Project Vision & Goals

### Primary Vision
Create the ultimate AI-powered running companion that provides personalized coaching, seamless third-party app integration, and intelligent voice guidance to help runners achieve their goals.

### Core Objectives
1. **Seamless Integration**: Connect with Google Fit and Spotify for comprehensive data sync
2. **AI-Powered Coaching**: Provide intelligent, contextual coaching using Google Gemini, ElevenLabs TTS, and a Fitness Coach AI Agent
3. **Real-Time Guidance**: Deliver live audio coaching during runs with smart trigger systems
4. **Comprehensive Analytics**: Track progress with advanced performance insights
5. **Modern UX**: Implement dark-first design with Material 3 and Jetpack Compose

## 🚨 Critical Issues to Address (Lessons Learned)

### Build & Version Compatibility Issues

#### 1. **Gradle & Kotlin Version Conflicts**
**Current Problem**: 
- Kotlin 1.7.20 with outdated Compose Compiler 1.3.2
- Gradle 7.6.1 causing compatibility issues
- Android Gradle Plugin 7.3.1 is outdated

**Solution Approach**:
- Upgrade to modern Android Gradle Plugin and Kotlin versions
- Ensure Compose Compiler version matches Kotlin version
- Migrate from Java 1.8 to Java 11 for modern dependency support
- Use BOM (Bill of Materials) for consistent dependency versioning
- Implement gradual migration strategy to minimize breaking changes

#### 2. **Material Icons Compatibility**
**Current Problem**: Using non-existent Material Icons causing compilation errors

**Solution Approach**:
- Audit all icon references for correct Material Design icon names
- Add Material Icons Extended library for broader icon support
- Create icon mapping documentation for commonly confused icons
- Implement fallback icon system for missing icons

#### 3. **Java Version Compatibility**
**Current Problem**: Using Java 1.8 with modern dependencies requiring Java 11+

**Solution Approach**: 
- Migrate build configuration to target Java 11
- Update all module configurations for consistency
- Test compatibility with existing codebase
- Address any Java 11 specific syntax or API changes

### Architecture Issues Identified

#### 1. **Missing Dependency Injection Setup**
- Hilt modules not properly configured
- Repository interfaces without implementations
- Missing database initialization

#### 2. **Incomplete Theme System**
- Missing Theme.kt and Type.kt files
- Inconsistent color system implementation
- No proper dark theme support

#### 3. **Navigation Flow Issues**
- Missing screen implementations
- Broken navigation parameters
- Incomplete state management

## 📱 Product Requirements

### 1. **Core User Journey**
```
Welcome Screen → Connect Apps → Personalize Profile → Set Event Goal → Dashboard
     ↓              ↓              ↓                ↓              ↓
   Onboarding    App Integration   Profile Setup   Goal Setting   Main App
```

### 2. **Technical Architecture Requirements**

#### **Platform & Versions**
```kotlin
// MANDATORY VERSION REQUIREMENTS
Android {
    compileSdk = 34
    targetSdk = 34
    minSdk = 24 // Android 7.0+
}

Kotlin = "1.9.22"
Compose Compiler = "1.5.8"
Compose BOM = "2024.02.00"
Hilt = "2.48"
Room = "2.6.1"
```

#### **Architecture Pattern**
- **MVVM** with Clean Architecture
- **Repository Pattern** for data access
- **Hilt** for dependency injection
- **StateFlow/Flow** for reactive programming
- **Room** for local database
- **Ktor** for network operations

#### **UI Framework**
- **Jetpack Compose** (latest stable)
- **Material 3** design system
- **Dark theme first** approach
- **Navigation Compose** for routing

### 3. **Feature Requirements**

#### **A. Onboarding & Integration (Week 1-2)**
**Required Screens**:
1. WelcomeScreen - App introduction
2. ConnectAppsScreen - Google Fit, Spotify integration
3. PersonalizeProfileScreen - User profile setup
4. SetEventGoalScreen - Race goal configuration
5. DashboardScreen - Main application hub

**Integration Requirements**:
- **Google Fit REST API**: Activity data sync and health metrics
- **Spotify Web API**: Music preferences and playlist management
- **OAuth 2.0** authentication for all services
- **Offline capability** with data caching

#### **B. AI Coaching System (Week 3-4)**
**AI Integration Stack**:
- Google Gemini API for training plan generation
- ElevenLabs TTS for voice synthesis
- Custom GPT agent for fitness coaching
- ML Vertex AI for smart voice triggers

**Voice System Features**:
- **Real-time coaching** during runs
- **Context-aware triggers** based on performance
- **Voice line database** for caching
- **Audio priority system** for seamless music integration
- **Coach personality selection** (Bennett, Mariana, Becs, Goggins)

#### **C. Fitness Coach AI Agent**
- **Purpose**: Personalized fitness, workout, and diet advice.  
- **Base Model**: GPT agent ("Fitness, Workout & Diet – PhD Coach").  
- **Data Sources**: Health + fitness data from app (runs, HR, VO₂max, recovery, calories, sleep).  
- **Capabilities**:  
  - Adaptive training plan adjustments  
  - Nutrition guidance linked to fitness goals  
  - Contextual in-run coaching (HR zones, endurance advice)  
  - Periodization planning (weekly/monthly strategy)  
  - Explainable, patient-friendly feedback  
- **Integration Points**:  
  - AI Coach Tab → personalized plans and advice  
  - Voice Coach → live contextual adjustments  
  - Dashboard → summaries, weak points, recovery advice

#### **D. Run Tracking & Analytics (Week 5-6)**
- **GPS tracking** with high accuracy
- **Real-time metrics** (pace, distance, time)
- **Manual entry** for treadmill workouts
- **Performance analytics** with trend analysis
- **Goal progress tracking** with predictions

#### **E. UI/UX Implementation (Week 7-8)**
**Design System Challenges**:
- Implement dark-first theme with lime accent color (#84cc16)
- Create consistent typography system (text-3xl headers, text-lg subheaders, text-sm body)
- Establish proper spacing system (16dp, 24dp, 32dp)
- Design card-based layout with 16dp rounded corners
- Ensure accessibility compliance and high contrast support

**Navigation Structure**:
- Bottom Navigation with 4 tabs: Home (Dashboard), AI Coach, Progress, Profile
- Seamless navigation flow from onboarding to main app
- Proper state management across navigation transitions

### 4. **Data Management Requirements**

#### **Database Schema Challenges**
**Data Management Complexity**:
- Design comprehensive entities for User, TrainingPlan, Run, Coach, VoiceLine, CoachingEvent
- Implement proper relationships and foreign key constraints
- Handle database migrations and versioning
- Optimize query performance for large datasets
- Manage data synchronization between local and cloud storage

#### **API Integration Challenges**
**Security & Configuration**:
- Secure API key management through BuildConfig (Gemini, ElevenLabs, Spotify, Google Fit)
- Implement certificate pinning for HTTPS connections
- Design robust retry logic with exponential backoff
- Handle network timeouts and error scenarios
- Manage OAuth token refresh and expiration
- Implement proper request/response logging for debugging

## 🏗️ Implementation Roadmap

### **Phase 1: Foundation (Week 1-2)**
**Priority: 🔴 CRITICAL**

#### **Sprint 1.1: Project Setup**
- [ ] Create new Android project with correct versions
- [ ] Configure Gradle with updated dependencies
- [ ] Set up project structure (data/domain/presentation)
- [ ] Implement Hilt dependency injection
- [ ] Create base classes and utilities

#### **Sprint 1.2: Theme & Navigation**
- [ ] Implement Material 3 dark theme system
- [ ] Create color palette and typography
- [ ] Set up Navigation Compose
- [ ] Implement bottom navigation
- [ ] Create screen scaffolds

**Success Criteria**: App builds, launches, and navigates between screens

### **Phase 2: Core Features (Week 3-6)**

#### **Sprint 2.1: Data Layer**
- [ ] Implement Room database with all entities
- [ ] Create repository interfaces and implementations
- [ ] Set up data mappers and DTOs
- [ ] Implement local data caching
- [ ] Add database migrations

#### **Sprint 2.2: API Integrations**
- [ ] Implement Fitbit API integration
- [ ] Implement Google Fit API integration
- [ ] Implement Spotify API integration
- [ ] Add OAuth authentication flows
- [ ] Create data synchronization logic

#### **Sprint 2.3: AI Services**
- [ ] Integrate Google Gemini for plan generation
- [ ] Implement ElevenLabs TTS service
- [ ] Create voice line database
- [ ] Build smart trigger engine
- [ ] Implement personal fitness agent

**Success Criteria**: All integrations working, data syncing, AI responding

### **Phase 3: Advanced Features (Week 7-10)**

#### **Sprint 3.1: Run Tracking**
- [ ] Implement GPS location services
- [ ] Create run session management
- [ ] Add real-time metrics calculation
- [ ] Implement manual run entry
- [ ] Create run history management

#### **Sprint 3.2: Voice Coaching**
- [ ] Implement audio management system
- [ ] Create coaching trigger logic
- [ ] Add voice line generation
- [ ] Implement audio priority queuing
- [ ] Test music app integration

#### **Sprint 3.3: Analytics & UI Polish**
- [ ] Create performance analytics
- [ ] Implement progress tracking
- [ ] Add data visualizations
- [ ] Polish UI animations
- [ ] Implement accessibility features

**Success Criteria**: Complete feature set working, polished UX

### **Phase 4: Testing & Deployment (Week 11-12)**

#### **Sprint 4.1: Testing**
- [ ] Unit tests for all business logic
- [ ] Integration tests for APIs
- [ ] UI tests for critical flows
- [ ] Performance testing
- [ ] Security audit

#### **Sprint 4.2: Production Ready**
- [ ] Configure release builds
- [ ] Set up CI/CD pipeline
- [ ] Create app store assets
- [ ] Prepare privacy policy
- [ ] Final testing and bug fixes

**Success Criteria**: Production-ready app, store listing approved

## 🔧 Technical Specifications

### **Dependency Management Challenges**
**Core Technology Stack**:
- Modern Android development with Jetpack Compose and Material 3
- Clean Architecture with MVVM pattern and Hilt dependency injection
- Room database for local data persistence
- Ktor client for modern async networking
- Coroutines for reactive programming

**Key Integration Challenges**:
- Manage version compatibility across all dependencies
- Implement proper BOM (Bill of Materials) for version consistency
- Handle migration from older libraries to modern alternatives
- Ensure security best practices for encrypted data storage
- Optimize build times and app size with proper dependency management

### **Project Structure**
```
app/src/main/java/com/runningcoach/v2/
├── data/
│   ├── datasource/
│   │   ├── local/
│   │   │   ├── dao/
│   │   │   ├── database/
│   │   │   └── entities/
│   │   └── remote/
│   │       ├── api/
│   │       ├── dto/
│   │       └── interceptors/
│   ├── mapper/
│   └── repository/
├── domain/
│   ├── entity/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── navigation/
│   ├── screen/
│   │   ├── welcome/
│   │   ├── connectapps/
│   │   ├── profile/
│   │   ├── dashboard/
│   │   └── fitnessgpt/
│   ├── theme/
│   ├── ui/
│   │   ├── components/
│   │   └── icons/
│   └── viewmodel/
├── services/
│   ├── audio/
│   ├── location/
│   └── ai/
├── utils/
└── di/
```

## 🎨 Design System Requirements

### **Visual Design Challenges**
**Color System Implementation**:
- Establish dark-first theme with lime accent (#84cc16) as primary color
- Create comprehensive color palette with proper contrast ratios
- Implement semantic color tokens for error, success, warning states
- Ensure accessibility compliance with WCAG guidelines

**Typography System**:
- Design scalable typography system with proper hierarchy
- Implement responsive text sizing for different screen densities  
- Establish consistent line heights and letter spacing
- Support for multiple font weights and styles

**Layout & Spacing**:
- Create consistent spacing system (4dp, 8dp, 16dp, 24dp, 32dp, 48dp)
- Design flexible grid system for various screen sizes
- Implement proper component spacing and alignment
- Create reusable shape tokens for buttons, cards, and containers

## 📊 Success Metrics & KPIs

### **Technical Metrics**
- **Build Success Rate**: 100% (no build failures)
- **App Startup Time**: < 3 seconds cold start
- **Crash Rate**: < 0.1% (industry standard)
- **Battery Usage**: < 5% per hour during active tracking
- **Memory Usage**: < 200MB peak usage
- **Test Coverage**: > 85% code coverage

### **User Experience Metrics**
- **App Integration Rate**: > 70% users connect at least one service
- **Plan Completion Rate**: > 60% users complete generated training plans
- **Voice Coaching Engagement**: > 80% users enable voice coaching
- **User Retention**: > 70% 7-day retention, > 50% 30-day retention
- **App Store Rating**: > 4.5 stars average rating

### **Performance Benchmarks**
- **GPS Accuracy**: < 5 meter average deviation
- **Audio Latency**: < 200ms for voice coaching
- **API Response Time**: < 2 seconds for all integrations
- **Data Sync Speed**: Complete sync in < 30 seconds
- **Voice Synthesis**: < 3 seconds for average coaching message

## 🚨 Risk Management

### **Technical Risks & Mitigation**

#### **High Priority Risks**
1. **Version Compatibility Issues**
   - **Risk**: Dependency conflicts causing build failures
   - **Mitigation**: Use BOM for version management, pin versions explicitly
   - **Contingency**: Maintain compatibility matrix, automated testing

2. **API Integration Failures**
   - **Risk**: Third-party API changes breaking integrations
   - **Mitigation**: Implement robust error handling, fallback mechanisms
   - **Contingency**: Mock services for development, circuit breaker pattern

3. **Performance Issues**
   - **Risk**: Poor battery life, slow GPS, memory leaks
   - **Mitigation**: Continuous performance monitoring, optimization sprints
   - **Contingency**: Performance budgets, automated performance tests

#### **Medium Priority Risks**
1. **AI Service Reliability**
   - **Risk**: ElevenLabs/Gemini API downtime affecting core features
   - **Mitigation**: Local fallbacks, caching strategies, service redundancy
   - **Contingency**: Offline mode, cached voice lines, local TTS

2. **User Privacy Concerns**
   - **Risk**: Data privacy issues with third-party integrations
   - **Mitigation**: Minimal data collection, transparent privacy policy
   - **Contingency**: Data anonymization, user consent management

### **Timeline Risks**
- **Scope Creep**: Strict requirement management, change control process
- **Resource Constraints**: Flexible sprint planning, prioritization matrix
- **Technical Debt**: Regular refactoring sessions, code review process

## 📋 Acceptance Criteria

### **Phase 1 Completion Criteria**
- [ ] App builds successfully with all dependencies
- [ ] Navigation flows work between all screens
- [ ] Theme system fully implemented
- [ ] Basic UI components functional
- [ ] Hilt dependency injection working

### **Phase 2 Completion Criteria**
- [ ] All third-party integrations functional
- [ ] Data synchronization working
- [ ] AI services responding correctly
- [ ] Database operations complete
- [ ] User authentication flows working

### **Phase 3 Completion Criteria**
- [ ] GPS tracking accurate and reliable
- [ ] Voice coaching system functional
- [ ] Analytics dashboard complete
- [ ] Performance metrics within targets
- [ ] UI/UX polished and accessible

### **Phase 4 Completion Criteria**
- [ ] All tests passing (unit, integration, UI)
- [ ] Performance benchmarks met
- [ ] Security audit passed
- [ ] App store ready
- [ ] Documentation complete

## 🔄 Post-Launch Considerations

### **Immediate Post-Launch (Week 13-14)**
- Monitor crash reports and user feedback
- Performance optimization based on real usage
- Bug fixes and stability improvements
- User onboarding optimization

### **Short-term Enhancements (Month 2-3)**
- Additional coach personalities
- Advanced analytics features
- Social sharing capabilities
- Wearable device integration

### **Long-term Roadmap (Month 4+)**
- Multi-platform music support (Apple Music, YouTube Music)
- Community features and challenges
- Advanced health metrics integration
- International localization

## 📞 Stakeholder Communication

### **Weekly Status Reports**
- Development progress against milestones
- Technical challenges and solutions
- Performance metrics updates
- Risk assessment and mitigation status

### **Sprint Reviews**
- Feature demonstrations
- User feedback incorporation
- Technical debt assessment
- Next sprint planning

---

## 🎯 Conclusion

This PRD provides a comprehensive roadmap for rebuilding RunningCoachV2 from scratch, addressing all identified technical issues and incorporating lessons learned from previous development efforts. The focus on modern Android development practices, proper version management, and robust architecture will ensure a successful, maintainable, and scalable application.

**Key Success Factors:**
1. **Strict Version Management**: Prevent build and compatibility issues
2. **Comprehensive Testing**: Ensure quality and reliability
3. **User-Centric Design**: Focus on seamless user experience
4. **Performance First**: Optimize for battery, memory, and responsiveness
5. **Iterative Development**: Regular feedback and improvement cycles

**Next Steps:**
1. Review and approve this PRD
2. Set up development environment with specified versions
3. Begin Phase 1 implementation
4. Establish regular review cadence
5. Monitor progress against defined metrics

## 📚 Project Documentation Guide

This PRD is part of a comprehensive documentation suite. Here's how to navigate the project documentation:

### **Core Planning Documents**
- **[PRD-RunningCoachV2-Fresh-Start.md](PRD-RunningCoachV2-Fresh-Start.md)** (This Document) - Complete product requirements and technical specifications
- **[implementation-plan.md](implementation-plan.md)** - Detailed development roadmap with phases, sprints, and technical tasks
- **[requirements.md](requirements.md)** - Comprehensive functional and technical requirements specification
- **[current-status.md](current-status.md)** - Real-time project status, achievements, and next phase readiness

### **Architecture & Design**
- **[architecture.md](architecture.md)** - System architecture, technical design patterns, and component relationships
- **[database-design.md](database-design.md)** - Complete database schema, entities, and data management strategy
- **[field-mapping.md](field-mapping.md)** - Data models, field specifications, and type definitions

### **Development Guidelines**
- **[common-issues-solutions.md](common-issues-solutions.md)** - Troubleshooting guide, build issues, and proven solutions
- **[testing-strategy.md](testing-strategy.md)** - Testing approach, quality assurance, and validation procedures
- **[CHANGELOG.md](CHANGELOG.md)** - Development progress tracking and version history

### **Integration Guides**
- **[api-reference.md](api-reference.md)** - API specifications and third-party service integrations
- **[api-keys-summary.md](api-keys-summary.md)** - API key configuration and service setup status
- **[google-maps-setup-guide.md](google-maps-setup-guide.md)** - Google Maps API integration instructions
- **[spotify-setup-guide.md](spotify-setup-guide.md)** - Spotify API integration and OAuth setup
- **[google-services-setup-guide.md](google-services-setup-guide.md)** - Google services configuration

### **Feature Documentation**
- **[sync-strategy.md](sync-strategy.md)** - Data synchronization and cloud integration strategy
- **[voice-system.md](voice-system.md)** - Text-to-speech and audio coaching system specifications
- **[wireframe-reference.md](wireframe-reference.md)** - UI/UX reference from React wireframe implementation
- **[wishlist-features.md](wishlist-features.md)** - Future enhancements and advanced feature roadmap

### **Development Tasks**
- **[todays_tasks.md](todays_tasks.md)** - Current development session status and completed tasks
- **[image-handling.md](image-handling.md)** - UI assets and visual design implementation guidelines

### **How to Use This Documentation**

#### **For Project Managers:**
1. Start with this **PRD** for complete project overview
2. Review **[current-status.md](current-status.md)** for real-time progress
3. Check **[implementation-plan.md](implementation-plan.md)** for timeline and milestones
4. Monitor **[CHANGELOG.md](CHANGELOG.md)** for development updates

#### **For Developers:**
1. Read **[implementation-plan.md](implementation-plan.md)** for technical roadmap
2. Study **[architecture.md](architecture.md)** for system design
3. Reference **[common-issues-solutions.md](common-issues-solutions.md)** for troubleshooting
4. Follow **[todays_tasks.md](todays_tasks.md)** for current development priorities

#### **For Integration Work:**
1. Review **[api-reference.md](api-reference.md)** for service specifications
2. Follow setup guides for each service (Google Maps, Spotify, etc.)
3. Check **[api-keys-summary.md](api-keys-summary.md)** for configuration status
4. Reference **[sync-strategy.md](sync-strategy.md)** for data handling

#### **For UI/UX Implementation:**
1. Study **[wireframe-reference.md](wireframe-reference.md)** for design patterns
2. Review **[field-mapping.md](field-mapping.md)** for data structures
3. Check **[image-handling.md](image-handling.md)** for asset guidelines
4. Reference **[voice-system.md](voice-system.md)** for audio features

### **Documentation Maintenance**
- All documents are kept in sync with development progress
- **[current-status.md](current-status.md)** is updated after each development session
- **[CHANGELOG.md](CHANGELOG.md)** tracks all significant changes and achievements
- Integration guides are updated when API configurations change

---

*This document serves as the single source of truth for the RunningCoachV2 rebuild project and should be referenced throughout the development lifecycle.*
