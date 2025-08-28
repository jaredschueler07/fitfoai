package com.runningcoach.v2.data.service

import android.content.Context
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.local.entity.CoachPersonalityEntity
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs

/**
 * [BACKEND-UPDATE] Enhanced VoiceCoachingManager - Complete voice coaching system
 * 
 * Integrates SmartTriggerEngine, VoiceCacheManager, AudioFocusManager, and ElevenLabsService
 * for intelligent, context-aware voice coaching with offline support and music app interaction.
 */
class VoiceCoachingManager(
    private val context: Context,
    private val database: FITFOAIDatabase,
    private val elevenLabsService: ElevenLabsService,
    private val fitnessCoachAgent: FitnessCoachAgent
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Core components
    private val smartTriggerEngine = SmartTriggerEngine()
    private val voiceCacheManager = VoiceCacheManager(context, database, elevenLabsService)
    private val audioFocusManager = AudioFocusManager(context)
    private val coachPersonalityDao = database.coachPersonalityDao()
    
    // State management
    private val _isVoiceCoachingEnabled = MutableStateFlow(true)
    val isVoiceCoachingEnabled: StateFlow<Boolean> = _isVoiceCoachingEnabled.asStateFlow()
    
    private val _currentCoachingPhase = MutableStateFlow(CoachingPhase.WARMUP)
    val currentCoachingPhase: StateFlow<CoachingPhase> = _currentCoachingPhase.asStateFlow()
    
    private val _currentCoach = MutableStateFlow<String?>(null)
    val currentCoach: StateFlow<String?> = _currentCoach.asStateFlow()
    
    private val _lastCoachingTime = MutableStateFlow(0L)
    private val _runStartTime = MutableStateFlow(0L)
    private val _coachingStats = MutableStateFlow(CoachingStats())
    val coachingStats: StateFlow<CoachingStats> = _coachingStats.asStateFlow()
    
    // Coaching intervals (in milliseconds) - now dynamic based on coach personality
    private val baseCoachingIntervals = mapOf(
        CoachingPhase.WARMUP to 60_000L,      // Every 1 minute during warmup
        CoachingPhase.MAIN_WORKOUT to 120_000L, // Every 2 minutes during main workout
        CoachingPhase.COOLDOWN to 90_000L     // Every 1.5 minutes during cooldown
    )
    
    private var coachingJob: Job? = null
    private var isInitialized = false
    
    init {
        // Initialize coaching system
        scope.launch {
            initializeCoachingSystem()
        }
    }
    
    suspend fun startVoiceCoaching(
        runMetrics: Flow<RunMetrics>,
        targetPace: String? = null,
        targetDistance: String? = null,
        targetDistanceMeters: Float? = null
    ) {
        if (!_isVoiceCoachingEnabled.value) return
        
        if (!isInitialized) {
            initializeCoachingSystem()
        }
        
        _runStartTime.value = System.currentTimeMillis()
        _lastCoachingTime.value = System.currentTimeMillis()
        
        // Reset trigger engine for new run
        smartTriggerEngine.resetTriggerState()
        
        // Start with warmup phase
        _currentCoachingPhase.value = CoachingPhase.WARMUP
        
        // Get current coach
        val selectedCoach = coachPersonalityDao.getSelectedCoachId() ?: "bennett"
        _currentCoach.value = selectedCoach
        
        // Configure audio focus for coaching
        audioFocusManager.configureForVoiceCoaching()
        
        // Provide initial coaching
        scope.launch {
            val welcomeMessage = generatePersonalizedWelcome(selectedCoach)
            playCoachingMessage(
                text = welcomeMessage,
                urgency = ElevenLabsService.CoachingUrgency.CALM,
                priority = ElevenLabsService.AudioPriority.NORMAL,
                coachId = selectedCoach
            )
            
            // Update coaching stats
            updateCoachingStats { it.copy(sessionsStarted = it.sessionsStarted + 1) }
        }
        
        // Start monitoring run metrics for intelligent coaching triggers
        coachingJob = scope.launch {
            runMetrics.collect { metrics ->
                processRunMetricsWithSmartTriggers(
                    metrics = metrics,
                    targetPace = targetPace,
                    targetDistance = targetDistanceMeters,
                    coachId = selectedCoach
                )
            }
        }
    }
    
    fun stopVoiceCoaching() {
        coachingJob?.cancel()
        
        // Provide completion coaching
        if (_isVoiceCoachingEnabled.value) {
            scope.launch {
                val selectedCoach = _currentCoach.value ?: "bennett"
                val completionMessage = generatePersonalizedCompletion(selectedCoach)
                
                playCoachingMessage(
                    text = completionMessage,
                    urgency = ElevenLabsService.CoachingUrgency.NORMAL,
                    priority = ElevenLabsService.AudioPriority.HIGH,
                    coachId = selectedCoach
                )
                
                // Update coach usage statistics
                coachPersonalityDao.incrementUseCount(selectedCoach)
                updateCoachingStats { it.copy(sessionsCompleted = it.sessionsCompleted + 1) }
                
                // Clean up audio resources
                delay(3000) // Wait for completion message
                audioFocusManager.stopCurrentPlayback()
            }
        }
    }
    
    fun pauseVoiceCoaching() {
        coachingJob?.cancel()
        fitnessCoachAgent.stopCurrentAudio()
    }
    
    fun resumeVoiceCoaching(runMetrics: Flow<RunMetrics>) {
        if (!_isVoiceCoachingEnabled.value) return
        
        coachingJob = scope.launch {
            runMetrics.collect { metrics ->
                processRunMetricsWithSmartTriggers(
                    metrics = metrics,
                    targetPace = null,
                    targetDistance = null,
                    coachId = _currentCoach.value ?: "bennett"
                )
            }
        }
    }
    
    private suspend fun processRunMetricsWithSmartTriggers(
        metrics: RunMetrics,
        targetPace: String?,
        targetDistance: Float?,
        coachId: String
    ) {
        val currentTime = System.currentTimeMillis()
        val runDuration = currentTime - _runStartTime.value
        
        // Update coaching phase based on run duration and distance
        updateCoachingPhase(runDuration, metrics.distance)
        
        // Get coach personality settings for dynamic intervals
        val coachPersonality = coachPersonalityDao.getCoachPersonality(coachId)
        val adjustedIntervals = getAdjustedCoachingIntervals(coachPersonality)
        
        // Check if it's time for interval-based coaching
        val timeSinceLastCoaching = currentTime - _lastCoachingTime.value
        val currentPhase = _currentCoachingPhase.value
        val intervalForPhase = adjustedIntervals[currentPhase] ?: 120_000L
        
        // Use SmartTriggerEngine to analyze metrics and determine triggers
        val heartRateZones = coachPersonality?.let { calculateHeartRateZones(it) }
        val triggers = smartTriggerEngine.analyzeMetricsForTriggers(
            metrics = metrics,
            targetPace = targetPace,
            targetDistance = targetDistance,
            heartRateZones = heartRateZones
        )
        
        // Process high-priority triggers immediately
        triggers.filter { it.priority == ElevenLabsService.AudioPriority.URGENT }
            .forEach { trigger ->
                processTrigger(trigger, coachId)
                updateCoachingStats { it.copy(urgentTriggersCount = it.urgentTriggersCount + 1) }
            }
        
        // Process normal triggers if interval elapsed
        if (timeSinceLastCoaching >= intervalForPhase) {
            // Process interval-based coaching
            provideIntervalCoaching(metrics, targetPace, targetDistance?.toString(), coachId)
            
            // Process normal and low priority triggers
            triggers.filter { it.priority != ElevenLabsService.AudioPriority.URGENT }
                .take(2) // Limit to 2 additional messages to avoid overwhelming
                .forEach { trigger ->
                    processTrigger(trigger, coachId)
                }
            
            _lastCoachingTime.value = currentTime
            updateCoachingStats { it.copy(totalTriggersProcessed = it.totalTriggersProcessed + triggers.size) }
        }
    }
    
    private fun updateCoachingPhase(runDuration: Long, distance: Float) {
        val newPhase = when {
            runDuration < 300_000L -> CoachingPhase.WARMUP // First 5 minutes
            distance > 0 && distance < 500f -> CoachingPhase.WARMUP // First 500m
            runDuration > 1_800_000L -> CoachingPhase.COOLDOWN // After 30 minutes
            else -> CoachingPhase.MAIN_WORKOUT
        }
        
        if (newPhase != _currentCoachingPhase.value) {
            _currentCoachingPhase.value = newPhase
            
            // Provide phase transition coaching
            scope.launch {
                val phaseMessage = when (newPhase) {
                    CoachingPhase.WARMUP -> "Let's start with a gentle warmup. Focus on finding your rhythm."
                    CoachingPhase.MAIN_WORKOUT -> "Great warmup! Now let's get into your main workout pace."
                    CoachingPhase.COOLDOWN -> "Excellent work! Time to start cooling down. Gradually reduce your pace."
                }
                
                // Generate and play phase-specific coaching
                fitnessCoachAgent.sendMessage(
                    "Provide a brief coaching message for transitioning to ${newPhase.name.lowercase().replace('_', ' ')} phase: $phaseMessage",
                    includeVoiceResponse = true
                )
            }
        }
    }
    
    private suspend fun provideIntervalCoaching(
        metrics: RunMetrics,
        targetPace: String? = null,
        targetDistance: String? = null
    ) {
        val coachingPrompts = when (_currentCoachingPhase.value) {
            CoachingPhase.WARMUP -> listOf(
                "How does your warmup feel? Remember to start easy.",
                "Focus on your breathing and form during warmup.",
                "Great start! Keep building into your target pace gradually."
            )
            CoachingPhase.MAIN_WORKOUT -> listOf(
                "You're in your main workout now. How's your pace feeling?",
                "Check in with your body. Adjust pace if needed.",
                "Maintain steady effort. You're doing great!"
            )
            CoachingPhase.COOLDOWN -> listOf(
                "Time to cool down. Gradually reduce your effort.",
                "Great job on the main workout! Easy pace now.",
                "Focus on recovery breathing as you cool down."
            )
        }
        
        val randomPrompt = coachingPrompts.random()
        
        fitnessCoachAgent.provideRunCoaching(
            metrics,
            targetPace,
            targetDistance,
            includeVoice = true
        )
    }
    
    private suspend fun checkCoachingTriggers(
        metrics: RunMetrics,
        targetPace: String? = null
    ) {
        // Trigger coaching based on performance indicators
        
        // Pace-based triggers
        targetPace?.let { target ->
            val targetPaceFloat = parsePaceString(target)
            if (targetPaceFloat > 0 && metrics.currentPace > 0) {
                val paceDifference = abs(metrics.currentPace - targetPaceFloat)
                
                if (paceDifference > 0.5f) { // More than 30 seconds off pace
                    val coachingMessage = if (metrics.currentPace > targetPaceFloat) {
                        "You're running a bit fast. Try to settle into your target pace."
                    } else {
                        "You can pick up the pace a little to hit your target."
                    }
                    
                    fitnessCoachAgent.sendMessage(
                        coachingMessage,
                        includeVoiceResponse = true
                    )
                }
            }
        }
        
        // Distance milestone triggers
        val distanceKm = metrics.distance / 1000f
        val milestones = listOf(1f, 2f, 3f, 5f, 10f, 15f, 20f)
        
        milestones.forEach { milestone ->
            if (distanceKm >= milestone && distanceKm < milestone + 0.1f) {
                val milestoneMessage = when (milestone) {
                    1f -> "One kilometer down! You're finding your rhythm."
                    2f -> "Two kilometers completed! How are you feeling?"
                    5f -> "Five kilometers! You're hitting your stride."
                    10f -> "Ten kilometers! Outstanding endurance!"
                    else -> "${milestone.toInt()} kilometers! Keep up the excellent work!"
                }
                
                fitnessCoachAgent.sendMessage(
                    milestoneMessage,
                    includeVoiceResponse = true
                )
            }
        }
        
        // Time-based milestones
        val durationMinutes = metrics.duration / 60_000L
        val timeMillestones = listOf(10L, 20L, 30L, 45L, 60L)
        
        timeMillestones.forEach { milestone ->
            if (durationMinutes >= milestone && durationMinutes < milestone + 1) {
                val timeMessage = when (milestone) {
                    10L -> "Ten minutes in! You're doing great."
                    20L -> "Twenty minutes! You're really getting into the zone."
                    30L -> "Half an hour! Excellent endurance."
                    45L -> "Forty-five minutes! You're a running machine!"
                    60L -> "One hour! Incredible dedication!"
                    else -> "$milestone minutes! Keep pushing forward!"
                }
                
                fitnessCoachAgent.sendMessage(
                    timeMessage,
                    includeVoiceResponse = true
                )
            }
        }
    }
    
    private fun parsePaceString(paceString: String): Float {
        // Parse pace string like "5:30 /km" to minutes as float (5.5)
        return try {
            val cleanPace = paceString.replace(" /km", "").replace("/km", "")
            val parts = cleanPace.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toInt()
                val seconds = parts[1].toInt()
                minutes + (seconds / 60f)
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }
    
    fun setVoiceCoachingEnabled(enabled: Boolean) {
        _isVoiceCoachingEnabled.value = enabled
        if (!enabled) {
            fitnessCoachAgent.stopCurrentAudio()
        }
    }
    
    fun provideManualCoaching(scenario: FitnessCoachAgent.CoachingScenario) {
        if (!_isVoiceCoachingEnabled.value) return
        
        scope.launch {
            val message = fitnessCoachAgent.getQuickCoaching(scenario)
            fitnessCoachAgent.sendMessage(message, includeVoiceResponse = true)
        }
    }
    
    fun getCurrentCoachingStatus(): CoachingStatus {
        return CoachingStatus(
            isEnabled = _isVoiceCoachingEnabled.value,
            currentPhase = _currentCoachingPhase.value,
            isPlaying = fitnessCoachAgent.isPlayingAudio(),
            currentCoach = _currentCoach.value,
            queueSize = 0, // TODO: Implement queue tracking
            audioFocusState = AudioFocusManager.AudioFocusState.NONE // TODO: Get from audioFocusManager
        )
    }
    
    fun cleanup() {
        coachingJob?.cancel()
        scope.launch {
            audioFocusManager.stopCurrentPlayback()
        }
        // voiceCacheManager.cleanup() // TODO: Implement cleanup method
        audioFocusManager.cleanup()
        scope.cancel()
    }
    
    // Enhanced methods for the new voice coaching system
    
    private suspend fun initializeCoachingSystem() {
        try {
            // Initialize default coaches if not exist
            val coachCount = coachPersonalityDao.getCoachCount()
            if (coachCount == 0) {
                coachPersonalityDao.initializeDefaultCoaches()
                println("[VOICE-COACHING] Initialized default coach personalities")
            }
            
            // Warm up cache with essential phrases for selected coach
            val selectedCoach = coachPersonalityDao.getSelectedCoachId() ?: "bennett"
            voiceCacheManager.warmUpCache(selectedCoach)
            
            isInitialized = true
            println("[VOICE-COACHING] Coaching system initialized")
        } catch (e: Exception) {
            println("[VOICE-COACHING] Initialization error: ${e.message}")
        }
    }
    
    private suspend fun playCoachingMessage(
        text: String,
        urgency: ElevenLabsService.CoachingUrgency,
        priority: ElevenLabsService.AudioPriority,
        coachId: String
    ) {
        try {
            // Get cached or generate voice line
            val audioFile = voiceCacheManager.getCachedVoiceLine(text, coachId)
            
            if (audioFile != null) {
                // Use AudioFocusManager for proper music app interaction
                audioFocusManager.playCoachingAudio(
                    audioData = audioFile,
                    onComplete = {}
                )
                
                updateCoachingStats { 
                    it.copy(
                        totalMessagesPlayed = it.totalMessagesPlayed + 1,
                        lastMessageTime = System.currentTimeMillis()
                    ) 
                }
            } else {
                println("[VOICE-COACHING] Failed to get voice line: No cached audio available")
                updateCoachingStats { it.copy(errorCount = it.errorCount + 1) }
            }
        } catch (e: Exception) {
            println("[VOICE-COACHING] Error playing coaching message: ${e.message}")
            updateCoachingStats { it.copy(errorCount = it.errorCount + 1) }
        }
    }
    
    private suspend fun processTrigger(trigger: SmartTriggerEngine.CoachingTrigger, coachId: String) {
        try {
            playCoachingMessage(
                text = trigger.message,
                urgency = trigger.urgency,
                priority = trigger.priority,
                coachId = coachId
            )
            
            println("[VOICE-COACHING] Processed trigger: ${trigger.type} - ${trigger.context}")
        } catch (e: Exception) {
            println("[VOICE-COACHING] Error processing trigger: ${e.message}")
        }
    }
    
    private suspend fun provideIntervalCoaching(
        metrics: RunMetrics,
        targetPace: String?,
        targetDistance: String?,
        coachId: String
    ) {
        try {
            val coachingPrompts = getPhaseSpecificPrompts(_currentCoachingPhase.value)
            val selectedPrompt = coachingPrompts.random()
            
            playCoachingMessage(
                text = selectedPrompt,
                urgency = ElevenLabsService.CoachingUrgency.NORMAL,
                priority = ElevenLabsService.AudioPriority.NORMAL,
                coachId = coachId
            )
        } catch (e: Exception) {
            println("[VOICE-COACHING] Error in interval coaching: ${e.message}")
        }
    }
    
    private fun getPhaseSpecificPrompts(phase: CoachingPhase): List<String> {
        return when (phase) {
            CoachingPhase.WARMUP -> listOf(
                "How does your warmup feel?",
                "Focus on your breathing and form during warmup",
                "Great start! Keep building into your target pace gradually"
            )
            CoachingPhase.MAIN_WORKOUT -> listOf(
                "You're in your main workout now. How's your pace feeling?",
                "Check in with your body. Adjust pace if needed",
                "Maintain steady effort. You're doing great!"
            )
            CoachingPhase.COOLDOWN -> listOf(
                "Time to cool down. Gradually reduce your effort",
                "Great job on the main workout! Easy pace now",
                "Focus on recovery breathing as you cool down"
            )
        }
    }
    
    private suspend fun generatePersonalizedWelcome(coachId: String): String {
        return when (coachId) {
            "bennett" -> "Based on your running data, let's execute a strategic workout today."
            "mariana" -> "Hey superstar! Ready to crush this run with amazing energy?"
            "becs" -> "Take a moment to center yourself. Let's mindfully begin this journey."
            "goggins" -> "Time to get after it! No excuses, just pure determination!"
            else -> "Let's begin your run. Remember to start easy and find your rhythm."
        }
    }
    
    private suspend fun generatePersonalizedCompletion(coachId: String): String {
        return when (coachId) {
            "bennett" -> "Excellent performance data achieved. Well-executed workout!"
            "mariana" -> "You absolutely CRUSHED it! That energy was incredible!"
            "becs" -> "Beautiful work. Honor your body's effort and take time to recover."
            "goggins" -> "Outstanding! You stayed hard and got it done. No shortcuts!"
            else -> "Excellent work! You've completed your run. Time to cool down and celebrate."
        }
    }
    
    private fun getAdjustedCoachingIntervals(coachPersonality: CoachPersonalityEntity?): Map<CoachingPhase, Long> {
        val multiplier = coachPersonality?.motivationalFrequency?.let { freq: Int ->
            when (freq) {
                1, 2 -> 0.5f  // Very frequent
                3, 4 -> 0.75f // Frequent
                5 -> 1.0f     // Normal
                6, 7 -> 1.25f // Less frequent
                else -> 1.5f  // Infrequent
            }
        } ?: 1.0f
        
        return baseCoachingIntervals.mapValues { (_, interval) ->
            (interval * multiplier).toLong()
        }
    }
    
    private fun calculateHeartRateZones(coachPersonality: CoachPersonalityEntity): SmartTriggerEngine.HeartRateZones? {
        // This would typically use user's max HR or age-based calculation
        // For now, using age-based estimate (placeholder)
        val estimatedMaxHR = 180 // This should come from user profile
        
        return SmartTriggerEngine.HeartRateZones(
            zone1Max = (estimatedMaxHR * 0.6).toInt(),  // 60% max HR
            zone2Max = (estimatedMaxHR * 0.7).toInt(),  // 70% max HR
            zone3Max = (estimatedMaxHR * 0.8).toInt(),  // 80% max HR
            zone4Max = (estimatedMaxHR * 0.9).toInt(),  // 90% max HR
            zone5Max = estimatedMaxHR                   // 100% max HR
        )
    }
    
    private fun updateCoachingStats(update: (CoachingStats) -> CoachingStats) {
        _coachingStats.value = update(_coachingStats.value)
    }
    
    // Public API for coach selection and configuration
    
    suspend fun selectCoach(coachId: String): Result<Unit> {
        return try {
            coachPersonalityDao.selectNewCoach(coachId)
            _currentCoach.value = coachId
            
            // Warm up cache for new coach
            voiceCacheManager.warmUpCache(coachId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun preloadCoachingPhrases(coachId: String? = null): Result<Int> {
        return try {
            val coach = coachId ?: _currentCoach.value ?: "bennett"
            voiceCacheManager.preloadCoachPhrases(coach, listOf("essential"))
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun testCoachVoice(coachId: String, testPhrase: String = "Hello, ready for a great run!"): Result<Unit> {
        return try {
            playCoachingMessage(
                text = testPhrase,
                urgency = ElevenLabsService.CoachingUrgency.NORMAL,
                priority = ElevenLabsService.AudioPriority.HIGH,
                coachId = coachId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getAudioFocusStatus() = audioFocusManager.getAudioFocusStatus()
    
    suspend fun getCacheStats() = voiceCacheManager.getCacheStats()
    
    fun getTriggerStats() = smartTriggerEngine.getTriggerStats()
    
    suspend fun getAllCoachPersonalities() = coachPersonalityDao.getAllEnabledCoaches()
    
    // Observe coach personalities for UI
    fun observeSelectedCoach() = coachPersonalityDao.observeSelectedCoach()
    
    fun observeEnabledCoaches() = coachPersonalityDao.observeEnabledCoaches()
    
    enum class CoachingPhase {
        WARMUP,
        MAIN_WORKOUT,
        COOLDOWN
    }
    
    data class CoachingStatus(
        val isEnabled: Boolean,
        val currentPhase: CoachingPhase,
        val isPlaying: Boolean,
        val currentCoach: String?,
        val queueSize: Int,
        val audioFocusState: AudioFocusManager.AudioFocusState
    )
    
    data class CoachingStats(
        val sessionsStarted: Int = 0,
        val sessionsCompleted: Int = 0,
        val totalMessagesPlayed: Int = 0,
        val totalTriggersProcessed: Int = 0,
        val urgentTriggersCount: Int = 0,
        val errorCount: Int = 0,
        val lastMessageTime: Long = 0L
    ) {
        val successRate: Float get() = if (totalMessagesPlayed > 0) {
            ((totalMessagesPlayed - errorCount).toFloat() / totalMessagesPlayed) * 100f
        } else 100f
    }
}
