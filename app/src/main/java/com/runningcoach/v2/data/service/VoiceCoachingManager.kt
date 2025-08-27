package com.runningcoach.v2.data.service

import android.content.Context
import com.runningcoach.v2.domain.model.RunMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs

class VoiceCoachingManager(
    private val context: Context,
    private val fitnessCoachAgent: FitnessCoachAgent
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isVoiceCoachingEnabled = MutableStateFlow(true)
    val isVoiceCoachingEnabled: StateFlow<Boolean> = _isVoiceCoachingEnabled.asStateFlow()
    
    private val _currentCoachingPhase = MutableStateFlow(CoachingPhase.WARMUP)
    val currentCoachingPhase: StateFlow<CoachingPhase> = _currentCoachingPhase.asStateFlow()
    
    private val _lastCoachingTime = MutableStateFlow(0L)
    private val _runStartTime = MutableStateFlow(0L)
    
    // Coaching intervals (in milliseconds)
    private val coachingIntervals = mapOf(
        CoachingPhase.WARMUP to 60_000L,      // Every 1 minute during warmup
        CoachingPhase.MAIN_WORKOUT to 120_000L, // Every 2 minutes during main workout
        CoachingPhase.COOLDOWN to 90_000L     // Every 1.5 minutes during cooldown
    )
    
    private var coachingJob: Job? = null
    
    fun startVoiceCoaching(
        runMetrics: Flow<RunMetrics>,
        targetPace: String? = null,
        targetDistance: String? = null
    ) {
        if (!_isVoiceCoachingEnabled.value) return
        
        _runStartTime.value = System.currentTimeMillis()
        _lastCoachingTime.value = System.currentTimeMillis()
        
        // Start with warmup phase
        _currentCoachingPhase.value = CoachingPhase.WARMUP
        
        // Provide initial coaching
        scope.launch {
            val welcomeMessage = fitnessCoachAgent.getQuickCoaching(
                FitnessCoachAgent.CoachingScenario.START_RUN
            )
            fitnessCoachAgent.provideRunCoaching(
                RunMetrics(), // Empty metrics for start
                targetPace,
                targetDistance,
                includeVoice = true
            )
        }
        
        // Start monitoring run metrics for coaching triggers
        coachingJob = scope.launch {
            runMetrics.collect { metrics ->
                processRunMetrics(metrics, targetPace, targetDistance)
            }
        }
    }
    
    fun stopVoiceCoaching() {
        coachingJob?.cancel()
        
        // Provide completion coaching
        if (_isVoiceCoachingEnabled.value) {
            scope.launch {
                val completionMessage = fitnessCoachAgent.getQuickCoaching(
                    FitnessCoachAgent.CoachingScenario.COMPLETED_RUN
                )
                fitnessCoachAgent.provideRunCoaching(
                    RunMetrics(), // Empty metrics for completion
                    includeVoice = true
                )
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
                processRunMetrics(metrics)
            }
        }
    }
    
    private suspend fun processRunMetrics(
        metrics: RunMetrics,
        targetPace: String? = null,
        targetDistance: String? = null
    ) {
        val currentTime = System.currentTimeMillis()
        val runDuration = currentTime - _runStartTime.value
        
        // Update coaching phase based on run duration and distance
        updateCoachingPhase(runDuration, metrics.distance)
        
        // Check if it's time for interval-based coaching
        val timeSinceLastCoaching = currentTime - _lastCoachingTime.value
        val currentPhase = _currentCoachingPhase.value
        val intervalForPhase = coachingIntervals[currentPhase] ?: 120_000L
        
        if (timeSinceLastCoaching >= intervalForPhase) {
            provideIntervalCoaching(metrics, targetPace, targetDistance)
            _lastCoachingTime.value = currentTime
        }
        
        // Check for trigger-based coaching
        checkCoachingTriggers(metrics, targetPace)
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
            isPlaying = fitnessCoachAgent.isPlayingAudio()
        )
    }
    
    fun cleanup() {
        coachingJob?.cancel()
        fitnessCoachAgent.stopCurrentAudio()
        scope.cancel()
    }
    
    enum class CoachingPhase {
        WARMUP,
        MAIN_WORKOUT,
        COOLDOWN
    }
    
    data class CoachingStatus(
        val isEnabled: Boolean,
        val currentPhase: CoachingPhase,
        val isPlaying: Boolean
    )
}
