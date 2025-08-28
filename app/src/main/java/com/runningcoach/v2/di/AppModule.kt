package com.runningcoach.v2.di

import android.content.Context
import androidx.room.Room
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.repository.RunSessionRepositoryImpl
import com.runningcoach.v2.data.service.*
import com.runningcoach.v2.domain.repository.RunSessionRepository
import com.runningcoach.v2.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection container.
 * Provides all dependencies for the application without Hilt.
 * [TECH-DEBT] Replace with Hilt once KSP compatibility issues are resolved.
 */
class AppContainer(private val context: Context) {

    private val database: FITFOAIDatabase by lazy {
        Room.databaseBuilder(
            context,
            FITFOAIDatabase::class.java,
            "fitfoai_database"
        ).build()
    }

    private val runSessionDao: RunSessionDao by lazy {
        database.runSessionDao()
    }

    private val locationService: LocationService by lazy {
        LocationService(context)
    }
    
    private val sessionRecoveryManager: SessionRecoveryManager by lazy {
        SessionRecoveryManager(context)
    }
    
    // Permission Management
    val permissionManager: PermissionManager by lazy {
        PermissionManager(context)
    }
    
    // Audio Management
    private val audioFocusManager: AudioFocusManager by lazy {
        AudioFocusManager(context)
    }
    
    // ElevenLabs Service
    private val elevenLabsService: ElevenLabsService by lazy {
        ElevenLabsService()
    }
    
    // Fitness Coach Agent
    private val fitnessCoachAgent: FitnessCoachAgent by lazy {
        FitnessCoachAgent(geminiService, database)
    }
    
    // Gemini Service
    private val geminiService: GeminiService by lazy {
        GeminiService()
    }
    
    // Voice Coaching Manager
    val voiceCoachingManager: VoiceCoachingManager by lazy {
        VoiceCoachingManager(context, database, elevenLabsService, fitnessCoachAgent)
    }
    
    // Background Location Service (dependency for location tracking)
    val backgroundLocationService: BackgroundLocationService by lazy {
        BackgroundLocationService()
    }

    private val applicationScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    val runSessionRepository: RunSessionRepository by lazy {
        RunSessionRepositoryImpl(runSessionDao, locationService, sessionRecoveryManager, applicationScope)
    }

    val startRunSessionUseCase: StartRunSessionUseCase by lazy {
        StartRunSessionUseCase(runSessionRepository)
    }

    val trackRunSessionUseCase: TrackRunSessionUseCase by lazy {
        TrackRunSessionUseCase(runSessionRepository)
    }

    val endRunSessionUseCase: EndRunSessionUseCase by lazy {
        EndRunSessionUseCase(runSessionRepository)
    }

    val getRunSessionsUseCase: GetRunSessionsUseCase by lazy {
        GetRunSessionsUseCase(runSessionRepository)
    }
    
    // Additional services for comprehensive app functionality
    val apiConnectionManager: APIConnectionManager by lazy {
        APIConnectionManager(context, database)
    }
    
    val googleFitService: GoogleFitService by lazy {
        GoogleFitService(context)
    }
    
    val spotifyService: SpotifyService by lazy {
        SpotifyService()
    }
    
    val runSessionManager: RunSessionManager by lazy {
        RunSessionManager(context, database, locationService, voiceCoachingManager)
    }
}