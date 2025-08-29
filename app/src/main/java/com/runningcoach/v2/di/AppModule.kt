package com.runningcoach.v2.di

import android.content.Context
import androidx.room.Room
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.dao.RunSessionDao
import com.runningcoach.v2.data.repository.RunSessionRepositoryImpl
import com.runningcoach.v2.data.service.*
import com.runningcoach.v2.BuildConfig
import com.runningcoach.v2.data.service.context.ContextPipeline
import com.runningcoach.v2.data.service.context.KnowledgeBaseContextSource
import com.runningcoach.v2.data.service.context.UserDataContextSource
import com.runningcoach.v2.domain.repository.RunSessionRepository
import com.runningcoach.v2.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Manual dependency injection container.
 * Provides all dependencies for the application without Hilt.
 * [TECH-DEBT] Replace with Hilt once KSP compatibility issues are resolved.
 */
class AppContainer(private val context: Context) {

    // HttpClient for network services
    private val httpClient: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    private val database: FITFOAIDatabase by lazy {
        FITFOAIDatabase.getDatabase(context)
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
    
    // [ARCH-CHANGE] PermissionManager removed from AppContainer
    // Create PermissionManager at Activity/Screen level to avoid casting issues
    
    // Audio Management
    private val audioFocusManager: AudioFocusManager by lazy {
        AudioFocusManager(context)
    }
    
    // ElevenLabs Service
    private val elevenLabsService: ElevenLabsService by lazy {
        ElevenLabsService(httpClient, context)
    }
    
    // Gemini Service (existing)
    private val geminiService: GeminiService by lazy {
        GeminiService(httpClient)
    }
    
    // LLM provider selection (separate for chat vs voice)
    private val llmChatService: LLMService by lazy {
        when (BuildConfig.AI_PROVIDER.uppercase()) {
            "GPT" -> OpenAIService(httpClient)
            else -> GeminiLLMAdapter(geminiService)
        }
    }

    // Voice should use Gemini by default (cost/control), independent of chat
    private val llmVoiceService: LLMService by lazy {
        GeminiLLMAdapter(geminiService)
    }
    
    // Fitness Coach Agents
    // - Chat agent: GPT (or provider configured)
    // - Voice agent: Gemini-backed, for coaching line generation when needed
    private val chatContextProvider: ChatContextProvider by lazy { ChatContextProvider(database) }
    private val userDataContextSource by lazy { UserDataContextSource(chatContextProvider) }
    private val kbContextSource by lazy { KnowledgeBaseContextSource(context) }
    private val contextPipeline by lazy { ContextPipeline(listOf(userDataContextSource, kbContextSource)) }
    val aiChatAgent: FitnessCoachAgent by lazy {
        FitnessCoachAgent(context, llmChatService, elevenLabsService, database, chatContextProvider, contextPipeline)
    }
    private val aiVoiceAgent: FitnessCoachAgent by lazy {
        FitnessCoachAgent(context, llmVoiceService, elevenLabsService, database, chatContextProvider, contextPipeline)
    }
    
    // Voice Coaching Manager
    val voiceCoachingManager: VoiceCoachingManager by lazy {
        VoiceCoachingManager(context, database, elevenLabsService, aiVoiceAgent)
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
    
    val generateTrainingPlanUseCase: com.runningcoach.v2.domain.usecase.GenerateTrainingPlanUseCase by lazy {
        com.runningcoach.v2.domain.usecase.GenerateTrainingPlanUseCase(database, llmChatService)
    }
    
    // Additional services for comprehensive app functionality
    val apiConnectionManager: APIConnectionManager by lazy {
        APIConnectionManager(context)
    }
    
    val googleFitService: GoogleFitService by lazy {
        GoogleFitService(context)
    }
    
    val spotifyService: SpotifyService by lazy {
        SpotifyService(context, httpClient)
    }
    
    val runSessionManager: RunSessionManager by lazy {
        RunSessionManager(context, locationService, database, voiceCoachingManager)
    }

    // Cleanup method for proper resource management
    fun cleanup() {
        httpClient.close()
    }
}
