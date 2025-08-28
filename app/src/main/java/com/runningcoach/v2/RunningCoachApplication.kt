package com.runningcoach.v2

import android.app.Application
import com.runningcoach.v2.di.AppContainer

// @HiltAndroidApp - Temporarily disabled due to KSP compatibility issue
class RunningCoachApplication : Application() {
    
    // Manual dependency injection container
    // [TECH-DEBT] Replace with Hilt once KSP compatibility issues are resolved
    lateinit var appContainer: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
