package com.runningcoach.v2.data.service

import android.content.Context
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.AIConversationEntity
import com.runningcoach.v2.data.local.entity.UserEntity
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FitnessCoachAgent(
    private val context: Context,
    private val llmService: LLMService,
    private val elevenLabsService: ElevenLabsService,
    private val database: FITFOAIDatabase
) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val conversationDao = database.aiConversationDao()
    private val userDao = database.userDao()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()
    
    private var currentUserId: Long = 1L
    private var currentCoachId: String = "ai_coach"
    
    fun setCurrentUser(userId: Long) {
        currentUserId = userId
    }
    
    fun setCoachVoice(coachId: String) {
        currentCoachId = coachId
    }
    
    suspend fun sendMessage(
        message: String,
        includeVoiceResponse: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _isProcessing.value = true
            
            // Save user message to database
            val userMessage = AIConversationEntity(
                userId = currentUserId,
                message = message,
                isFromUser = true,
                messageType = "TEXT"
            )
            conversationDao.insertMessage(userMessage)
            
            // Get user context for better responses
            val userContext = getUserFitnessContext()
            
            // Generate AI response
            val aiResponseResult = llmService.generateFitnessAdvice(message, userContext)
            
            if (aiResponseResult.isSuccess) {
                val aiResponse = aiResponseResult.getOrThrow()
                
                // Save AI response to database
                val aiMessage = AIConversationEntity(
                    userId = currentUserId,
                    message = aiResponse,
                    isFromUser = false,
                    messageType = if (includeVoiceResponse) "VOICE" else "TEXT"
                )
                conversationDao.insertMessage(aiMessage)
                
                _lastResponse.value = aiResponse
                
                // Generate voice response if requested
                if (includeVoiceResponse) {
                    generateVoiceResponse(aiResponse)
                }
                
                Result.success(aiResponse)
            } else {
                val errorMessage = "I'm having trouble connecting right now. Please try again in a moment."
                _lastResponse.value = errorMessage
                Result.failure(aiResponseResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isProcessing.value = false
        }
    }
    
    suspend fun generateTrainingPlan(
        goals: String,
        fitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _isProcessing.value = true
            
            val planResult = llmService.generateTrainingPlan(goals, fitnessLevel, targetRace, timeframe)
            
            if (planResult.isSuccess) {
                val plan = planResult.getOrThrow()
                
                // Save the generated plan as a conversation
                val planMessage = AIConversationEntity(
                    userId = currentUserId,
                    message = "Generated training plan: $plan",
                    isFromUser = false,
                    messageType = "TRAINING_PLAN"
                )
                conversationDao.insertMessage(planMessage)
                
                _lastResponse.value = plan
                Result.success(plan)
            } else {
                Result.failure(planResult.exceptionOrNull() ?: Exception("Plan generation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isProcessing.value = false
        }
    }
    
    suspend fun provideRunCoaching(
        currentMetrics: RunMetrics,
        targetPace: String? = null,
        targetDistance: String? = null,
        includeVoice: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val metricsContext = RunMetricsContext(
                distance = currentMetrics.distance / 1000f, // Convert to km
                duration = currentMetrics.getFormattedDuration(),
                currentPace = currentMetrics.getFormattedPace(),
                averagePace = currentMetrics.getFormattedPace(),
                heartRate = currentMetrics.currentHeartRate
            )
            
            val goals = UserGoals(
                targetPace = targetPace,
                targetDistance = targetDistance
            )
            
            val coachingResult = llmService.generateRunCoaching(metricsContext, goals)
            
            if (coachingResult.isSuccess) {
                val coaching = coachingResult.getOrThrow()
                
                // Save coaching tip
                val coachingMessage = AIConversationEntity(
                    userId = currentUserId,
                    message = coaching,
                    isFromUser = false,
                    messageType = "COACHING_TIP"
                )
                conversationDao.insertMessage(coachingMessage)
                
                // Generate voice coaching if requested
                if (includeVoice) {
                    val urgency = determineCoachingUrgency(currentMetrics, targetPace)
                    generateVoiceCoaching(coaching, urgency)
                }
                
                Result.success(coaching)
            } else {
                Result.failure(coachingResult.exceptionOrNull() ?: Exception("Coaching generation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun generateVoiceResponse(text: String) {
        scope.launch {
            try {
                elevenLabsService.generateSpeech(
                    text = text,
                    coachId = currentCoachId,
                    playImmediately = true
                )
            } catch (e: Exception) {
                // Log error but don't fail the main response
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun generateVoiceCoaching(
        coachingText: String,
        urgency: ElevenLabsService.CoachingUrgency
    ) {
        scope.launch {
            try {
                val audioResult = elevenLabsService.generateCoachingAudio(
                    coachingText = coachingText,
                    coachId = currentCoachId,
                    urgency = urgency
                )
                
                audioResult.getOrNull()?.let { audioFile ->
                    elevenLabsService.playAudio(audioFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun determineCoachingUrgency(
        metrics: RunMetrics,
        targetPace: String?
    ): ElevenLabsService.CoachingUrgency {
        // Simple logic to determine urgency based on performance
        return when {
            metrics.currentPace > 0 && targetPace != null -> {
                // Compare current pace to target (simplified)
                if (metrics.currentPace > 8.0f) ElevenLabsService.CoachingUrgency.URGENT
                else if (metrics.currentPace > 6.0f) ElevenLabsService.CoachingUrgency.ENERGETIC
                else ElevenLabsService.CoachingUrgency.NORMAL
            }
            metrics.duration > 1800000 -> ElevenLabsService.CoachingUrgency.ENERGETIC // 30+ minutes
            else -> ElevenLabsService.CoachingUrgency.NORMAL
        }
    }
    
    private suspend fun getUserFitnessContext(): UserFitnessContext {
        val user = userDao.getCurrentUser().firstOrNull()
        
        return UserFitnessContext(
            experienceLevel = user?.experienceLevel ?: "Beginner",
            goals = user?.runningGoals ?: listOf("General fitness"),
            age = user?.age ?: 30,
            recentActivity = "Regular running", // This could be calculated from recent runs
            trainingHistory = "Building base fitness" // This could be derived from user data
        )
    }
    
    fun getConversationHistory() = conversationDao.getConversationHistory(currentUserId)
    
    suspend fun clearConversationHistory() {
        conversationDao.clearConversationHistory(currentUserId)
    }
    
    fun stopCurrentAudio() {
        elevenLabsService.stopCurrentAudio()
    }
    
    fun isPlayingAudio(): Boolean {
        return elevenLabsService.isPlaying()
    }
    
    suspend fun preloadCommonPhrases(): Result<Map<String, String>> {
        return elevenLabsService.preloadCoachingPhrases(currentCoachId)
    }
    
    // Predefined coaching responses for common scenarios
    suspend fun getQuickCoaching(scenario: CoachingScenario): String {
        return when (scenario) {
            CoachingScenario.START_RUN -> "Great! Let's begin your run. Remember to start easy and find your rhythm."
            CoachingScenario.HALFWAY_POINT -> "You're halfway there! How are you feeling? Maintain your pace and stay focused."
            CoachingScenario.FINAL_STRETCH -> "You're in the final stretch! Push through and finish strong!"
            CoachingScenario.COMPLETED_RUN -> "Excellent work! You've completed your run. Take a moment to cool down and celebrate your achievement."
            CoachingScenario.STRUGGLING -> "I can see you're working hard. It's okay to slow down if needed. Listen to your body."
            CoachingScenario.EXCELLENT_PACE -> "Outstanding pace! You're running beautifully. Keep this rhythm going!"
        }
    }
    
    enum class CoachingScenario {
        START_RUN,
        HALFWAY_POINT,
        FINAL_STRETCH,
        COMPLETED_RUN,
        STRUGGLING,
        EXCELLENT_PACE
    }
}
