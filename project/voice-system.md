# 🎵 RunningCoach App - Voice System & Audio Management

## 📋 Overview

This document outlines the comprehensive voice system for the RunningCoach app, including ElevenLabs TTS integration, voice line database management, audio triggers, and the personal fitness agent implementation. It addresses your specific requirements for live running feedback and smart voice line triggers.

## 🎤 Voice System Architecture

### System Overview
```
┌─────────────────────────────────────────────────────────────┐
│                    Voice System                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ ElevenLabs  │ │ Voice Line  │ │ Audio       │          │
│  │ TTS Engine  │ │ Database    │ │ Manager     │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Smart Triggers                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ ML Vertex   │ │ Context     │ │ Trigger     │          │
│  │ AI Engine   │ │ Analyzer    │ │ Engine      │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Personal Fitness Agent                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │ Custom GPT  │ │ Context     │ │ Voice       │          │
│  │ Assistant   │ │ Manager     │ │ Integration │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## 🎵 ElevenLabs TTS Integration

### Primary TTS Service
```kotlin
class ElevenLabsTTSManager(
    private val apiKey: String,
    private val httpClient: HttpClient,
    private val audioManager: AudioManager,
    private val voiceLineDatabase: VoiceLineDatabase
) {
    
    private val ttsQueue = TTSQueue()
    private val voiceCache = mutableMapOf<String, Voice>()
    
    suspend fun synthesizeAndPlay(
        text: String,
        voiceId: String,
        priority: AudioPriority = AudioPriority.NORMAL
    ): AudioResult {
        return try {
            // Check voice line database first
            val cachedVoiceLine = voiceLineDatabase.getVoiceLine(text, voiceId)
            if (cachedVoiceLine != null) {
                return playCachedVoiceLine(cachedVoiceLine, priority)
            }
            
            // Generate new audio via ElevenLabs
            val audioData = synthesizeSpeech(text, voiceId)
            
            // Cache the voice line
            voiceLineDatabase.cacheVoiceLine(text, voiceId, audioData)
            
            // Play the audio
            playAudio(audioData, priority)
            
            AudioResult.Success
        } catch (e: Exception) {
            AudioResult.Error(e.message ?: "TTS failed")
        }
    }
    
    private suspend fun synthesizeSpeech(text: String, voiceId: String): AudioData {
        val request = TTSRequest(
            text = text,
            voiceId = voiceId,
            settings = getOptimalSettings(voiceId)
        )
        
        val response = httpClient.post("https://api.elevenlabs.io/v1/text-to-speech/$voiceId") {
            headers {
                append("xi-api-key", apiKey)
                append("Content-Type", "application/json")
            }
            setBody(request)
        }
        
        if (!response.status.isSuccess()) {
            throw TTSException("ElevenLabs API error: ${response.status}")
        }
        
        val responseBody = response.body<ByteArray>()
        return AudioData(
            audioBytes = responseBody,
            format = AudioFormat.MP3,
            duration = calculateDuration(responseBody),
            sampleRate = 22050
        )
    }
    
    private fun getOptimalSettings(voiceId: String): TTSSettings {
        return when (voiceId) {
            "coach_bennett" -> TTSSettings(
                stability = 0.7f,
                similarityBoost = 0.8f,
                style = 0.3f,
                useSpeakerBoost = true
            )
            "coach_mariana" -> TTSSettings(
                stability = 0.8f,
                similarityBoost = 0.75f,
                style = 0.2f,
                useSpeakerBoost = true
            )
            "coach_becs" -> TTSSettings(
                stability = 0.6f,
                similarityBoost = 0.85f,
                style = 0.4f,
                useSpeakerBoost = true
            )
            "coach_goggins" -> TTSSettings(
                stability = 0.5f,
                similarityBoost = 0.9f,
                style = 0.5f,
                useSpeakerBoost = true
            )
            else -> TTSSettings()
        }
    }
    
    suspend fun getAvailableVoices(): List<Voice> {
        if (voiceCache.isNotEmpty()) {
            return voiceCache.values.toList()
        }
        
        val response = httpClient.get("https://api.elevenlabs.io/v1/voices") {
            headers {
                append("xi-api-key", apiKey)
            }
        }
        
        val voices = Json.decodeFromString<List<ElevenLabsVoice>>(response.bodyAsText())
        val mappedVoices = voices.map { it.toVoice() }
        
        voiceCache.putAll(mappedVoices.associateBy { it.id })
        return mappedVoices
    }
}

data class TTSRequest(
    val text: String,
    val voiceId: String,
    val settings: TTSSettings
)

data class TTSSettings(
    val stability: Float = 0.5f,
    val similarityBoost: Float = 0.75f,
    val style: Float = 0.0f,
    val useSpeakerBoost: Boolean = true
)

enum class AudioPriority {
    CRITICAL, HIGH, NORMAL, LOW
}

sealed class AudioResult {
    object Success : AudioResult()
    data class Error(val message: String) : AudioResult()
}
```

### Voice Line Database
```kotlin
@Entity(tableName = "voice_lines")
data class VoiceLine(
    @PrimaryKey val id: String,
    val text: String,
    val voiceId: String,
    val audioData: ByteArray,
    val duration: Long,
    val category: VoiceLineCategory,
    val context: String,
    val usageCount: Int = 0,
    val lastUsed: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class VoiceLineCategory {
    PACE_GUIDANCE,
    MOTIVATION,
    MILESTONE_CELEBRATION,
    SAFETY_WARNING,
    RECOVERY_ADVICE,
    PERSONAL_FITNESS_AGENT,
    GENERAL_COACHING
}

@Dao
interface VoiceLineDao {
    @Query("SELECT * FROM voice_lines WHERE text = :text AND voiceId = :voiceId LIMIT 1")
    suspend fun getVoiceLine(text: String, voiceId: String): VoiceLine?
    
    @Query("SELECT * FROM voice_lines WHERE category = :category AND voiceId = :voiceId")
    suspend fun getVoiceLinesByCategory(category: VoiceLineCategory, voiceId: String): List<VoiceLine>
    
    @Query("SELECT * FROM voice_lines WHERE context LIKE :contextPattern AND voiceId = :voiceId")
    suspend fun getVoiceLinesByContext(contextPattern: String, voiceId: String): List<VoiceLine>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceLine(voiceLine: VoiceLine)
    
    @Update
    suspend fun updateVoiceLine(voiceLine: VoiceLine)
    
    @Query("UPDATE voice_lines SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: String, timestamp: Long)
    
    @Query("DELETE FROM voice_lines WHERE lastUsed < :timestamp")
    suspend fun deleteOldVoiceLines(timestamp: Long)
}

class VoiceLineDatabase(
    private val voiceLineDao: VoiceLineDao,
    private val fileManager: AudioFileManager
) {
    
    suspend fun cacheVoiceLine(
        text: String,
        voiceId: String,
        audioData: AudioData,
        category: VoiceLineCategory = VoiceLineCategory.GENERAL_COACHING,
        context: String = ""
    ) {
        val voiceLine = VoiceLine(
            id = generateVoiceLineId(text, voiceId),
            text = text,
            voiceId = voiceId,
            audioData = audioData.audioBytes,
            duration = audioData.duration.inWholeMilliseconds,
            category = category,
            context = context
        )
        
        voiceLineDao.insertVoiceLine(voiceLine)
    }
    
    suspend fun getVoiceLine(text: String, voiceId: String): VoiceLine? {
        return voiceLineDao.getVoiceLine(text, voiceId)
    }
    
    suspend fun getVoiceLinesByCategory(
        category: VoiceLineCategory,
        voiceId: String
    ): List<VoiceLine> {
        return voiceLineDao.getVoiceLinesByCategory(category, voiceId)
    }
    
    suspend fun getContextualVoiceLines(
        context: String,
        voiceId: String
    ): List<VoiceLine> {
        val contextPattern = "%$context%"
        return voiceLineDao.getVoiceLinesByContext(contextPattern, voiceId)
    }
    
    suspend fun incrementUsage(voiceLineId: String) {
        voiceLineDao.incrementUsage(voiceLineId, System.currentTimeMillis())
    }
    
    suspend fun cleanupOldVoiceLines(olderThanDays: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        voiceLineDao.deleteOldVoiceLines(cutoffTime)
    }
    
    private fun generateVoiceLineId(text: String, voiceId: String): String {
        val hash = text.hashCode().toString() + voiceId.hashCode().toString()
        return "voice_line_$hash"
    }
}
```

## 🧠 Smart Voice Line Triggers

### ML Vertex AI Integration
```kotlin
class SmartTriggerEngine(
    private val vertexAIClient: VertexAIClient,
    private val voiceLineDatabase: VoiceLineDatabase,
    private val ttsManager: ElevenLabsTTSManager,
    private val contextAnalyzer: ContextAnalyzer
) {
    
    suspend fun analyzeAndTrigger(
        runContext: RunContext,
        userState: UserState
    ): TriggerResult {
        return try {
            // Analyze current context
            val contextAnalysis = contextAnalyzer.analyzeContext(runContext, userState)
            
            // Get ML prediction for optimal voice line
            val prediction = getMLPrediction(contextAnalysis)
            
            // Find or generate appropriate voice line
            val voiceLine = getOrGenerateVoiceLine(prediction, contextAnalysis)
            
            // Play the voice line
            val audioResult = ttsManager.synthesizeAndPlay(
                text = voiceLine.text,
                voiceId = voiceLine.voiceId,
                priority = prediction.priority
            )
            
            TriggerResult.Success(voiceLine, prediction.confidence)
        } catch (e: Exception) {
            TriggerResult.Error(e.message ?: "Trigger failed")
        }
    }
    
    private suspend fun getMLPrediction(contextAnalysis: ContextAnalysis): MLPrediction {
        val request = MLPredictionRequest(
            context = contextAnalysis,
            modelVersion = "smart_triggers_v1"
        )
        
        val response = vertexAIClient.predict(request)
        return response.toMLPrediction()
    }
    
    private suspend fun getOrGenerateVoiceLine(
        prediction: MLPrediction,
        contextAnalysis: ContextAnalysis
    ): VoiceLine {
        // Try to find existing voice line
        val existingVoiceLines = voiceLineDatabase.getContextualVoiceLines(
            contextAnalysis.contextKey,
            prediction.voiceId
        )
        
        if (existingVoiceLines.isNotEmpty()) {
            // Use existing voice line with highest confidence
            return existingVoiceLines.maxByOrNull { it.usageCount }!!
        }
        
        // Generate new voice line using AI
        val generatedText = generateVoiceLineText(prediction, contextAnalysis)
        
        // Create and cache the voice line
        val audioData = ttsManager.synthesizeSpeech(generatedText, prediction.voiceId)
        val voiceLine = VoiceLine(
            id = generateVoiceLineId(generatedText, prediction.voiceId),
            text = generatedText,
            voiceId = prediction.voiceId,
            audioData = audioData.audioBytes,
            duration = audioData.duration.inWholeMilliseconds,
            category = prediction.category,
            context = contextAnalysis.contextKey
        )
        
        voiceLineDatabase.cacheVoiceLine(generatedText, prediction.voiceId, audioData, prediction.category, contextAnalysis.contextKey)
        return voiceLine
    }
    
    private suspend fun generateVoiceLineText(
        prediction: MLPrediction,
        contextAnalysis: ContextAnalysis
    ): String {
        val prompt = buildVoiceLinePrompt(prediction, contextAnalysis)
        
        val response = vertexAIClient.generateText(prompt)
        return response.text
    }
    
    private fun buildVoiceLinePrompt(
        prediction: MLPrediction,
        contextAnalysis: ContextAnalysis
    ): String {
        return """
        Generate a coaching voice line for a runner with the following context:
        
        Runner State:
        - Current pace: ${contextAnalysis.currentPace} min/km
        - Target pace: ${contextAnalysis.targetPace} min/km
        - Distance covered: ${contextAnalysis.distanceCovered} km
        - Time elapsed: ${contextAnalysis.timeElapsed} minutes
        - Energy level: ${contextAnalysis.energyLevel}
        - Motivation level: ${contextAnalysis.motivationLevel}
        
        Coach Personality: ${prediction.voiceId}
        Category: ${prediction.category}
        Priority: ${prediction.priority}
        
        Generate a ${prediction.category.name.lowercase()} message that is:
        - Under 15 words
        - Motivational and encouraging
        - Specific to the current situation
        - Matches the coach's personality
        
        Voice line:
        """.trimIndent()
    }
}

data class RunContext(
    val currentPace: Pace,
    val targetPace: Pace,
    val distanceCovered: Float,
    val timeElapsed: Duration,
    val heartRate: Int?,
    val elevation: Float,
    val weather: WeatherInfo?
)

data class UserState(
    val energyLevel: EnergyLevel,
    val motivationLevel: MotivationLevel,
    val fatigueLevel: FatigueLevel,
    val performanceLevel: PerformanceLevel,
    val recentCoachingEvents: List<CoachingEvent>
)

data class ContextAnalysis(
    val contextKey: String,
    val currentPace: String,
    val targetPace: String,
    val distanceCovered: String,
    val timeElapsed: String,
    val energyLevel: String,
    val motivationLevel: String,
    val coachingGap: Long, // Time since last coaching event
    val performanceTrend: PerformanceTrend
)

data class MLPrediction(
    val voiceId: String,
    val category: VoiceLineCategory,
    val priority: AudioPriority,
    val confidence: Float,
    val reasoning: String
)

sealed class TriggerResult {
    data class Success(val voiceLine: VoiceLine, val confidence: Float) : TriggerResult()
    data class Error(val message: String) : TriggerResult()
}
```

### Context Analyzer
```kotlin
class ContextAnalyzer {
    
    fun analyzeContext(runContext: RunContext, userState: UserState): ContextAnalysis {
        val contextKey = generateContextKey(runContext, userState)
        val performanceTrend = analyzePerformanceTrend(runContext, userState)
        
        return ContextAnalysis(
            contextKey = contextKey,
            currentPace = formatPace(runContext.currentPace),
            targetPace = formatPace(runContext.targetPace),
            distanceCovered = "${runContext.distanceCovered}km",
            timeElapsed = "${runContext.timeElapsed.toMinutes()}min",
            energyLevel = userState.energyLevel.name,
            motivationLevel = userState.motivationLevel.name,
            coachingGap = calculateCoachingGap(userState.recentCoachingEvents),
            performanceTrend = performanceTrend
        )
    }
    
    private fun generateContextKey(runContext: RunContext, userState: UserState): String {
        val paceDifference = runContext.currentPace.minutesPerUnit - runContext.targetPace.minutesPerUnit
        val paceStatus = when {
            paceDifference < -0.5 -> "ahead_of_pace"
            paceDifference > 0.5 -> "behind_pace"
            else -> "on_pace"
        }
        
        val energyStatus = when (userState.energyLevel) {
            EnergyLevel.HIGH -> "high_energy"
            EnergyLevel.MEDIUM -> "medium_energy"
            EnergyLevel.LOW -> "low_energy"
            EnergyLevel.EXHAUSTED -> "exhausted"
        }
        
        val motivationStatus = when (userState.motivationLevel) {
            MotivationLevel.MOTIVATED -> "motivated"
            MotivationLevel.NEUTRAL -> "neutral"
            MotivationLevel.STRUGGLING -> "struggling"
            MotivationLevel.DISCOURAGED -> "discouraged"
        }
        
        return "${paceStatus}_${energyStatus}_${motivationStatus}"
    }
    
    private fun analyzePerformanceTrend(
        runContext: RunContext,
        userState: UserState
    ): PerformanceTrend {
        val paceVsTarget = runContext.currentPace.minutesPerUnit - runContext.targetPace.minutesPerUnit
        val energyFactor = when (userState.energyLevel) {
            EnergyLevel.HIGH -> 1.0
            EnergyLevel.MEDIUM -> 0.7
            EnergyLevel.LOW -> 0.4
            EnergyLevel.EXHAUSTED -> 0.1
        }
        
        val motivationFactor = when (userState.motivationLevel) {
            MotivationLevel.MOTIVATED -> 1.0
            MotivationLevel.NEUTRAL -> 0.6
            MotivationLevel.STRUGGLING -> 0.3
            MotivationLevel.DISCOURAGED -> 0.1
        }
        
        val overallScore = (energyFactor + motivationFactor) / 2.0
        
        return when {
            paceVsTarget < -0.5 && overallScore > 0.7 -> PerformanceTrend.EXCELING
            paceVsTarget < 0.0 && overallScore > 0.5 -> PerformanceTrend.IMPROVING
            paceVsTarget < 0.5 && overallScore > 0.3 -> PerformanceTrend.STABLE
            else -> PerformanceTrend.STRUGGLING
        }
    }
    
    private fun calculateCoachingGap(recentEvents: List<CoachingEvent>): Long {
        if (recentEvents.isEmpty()) return Long.MAX_VALUE
        
        val lastEvent = recentEvents.maxByOrNull { it.timestamp }
        return System.currentTimeMillis() - lastEvent!!.timestamp
    }
    
    private fun formatPace(pace: Pace): String {
        return "${pace.minutesPerUnit}min/${pace.units.name.lowercase()}"
    }
}

enum class PerformanceTrend {
    EXCELING, IMPROVING, STABLE, STRUGGLING
}
```

## 🤖 Personal Fitness Agent

### Custom GPT Integration
```kotlin
class PersonalFitnessAgent(
    private val openAIClient: OpenAIClient,
    private val voiceLineDatabase: VoiceLineDatabase,
    private val ttsManager: ElevenLabsTTSManager,
    private val userProfileRepository: UserProfileRepository,
    private val runRepository: RunRepository
) {
    
    private val conversationHistory = mutableListOf<ConversationMessage>()
    private val agentContext = buildAgentContext()
    
    suspend fun processUserQuery(
        query: String,
        context: FitnessContext
    ): AgentResponse {
        return try {
            // Build conversation context
            val fullContext = buildConversationContext(context)
            
            // Get AI response
            val aiResponse = getAIResponse(query, fullContext)
            
            // Generate voice response
            val voiceResponse = generateVoiceResponse(aiResponse)
            
            // Update conversation history
            updateConversationHistory(query, aiResponse)
            
            AgentResponse.Success(aiResponse, voiceResponse)
        } catch (e: Exception) {
            AgentResponse.Error(e.message ?: "Agent failed")
        }
    }
    
    private suspend fun getAIResponse(
        query: String,
        context: String
    ): String {
        val messages = buildMessages(query, context)
        
        val request = ChatCompletionRequest(
            model = "gpt-4",
            messages = messages,
            maxTokens = 200,
            temperature = 0.7
        )
        
        val response = openAIClient.createChatCompletion(request)
        return response.choices.firstOrNull()?.message?.content ?: "I'm sorry, I couldn't process that."
    }
    
    private suspend fun generateVoiceResponse(text: String): AudioData {
        // Use the user's preferred coach voice
        val userProfile = userProfileRepository.getUserProfile()
        val voiceId = userProfile?.preferredCoach?.voiceId ?: "coach_bennett"
        
        return ttsManager.synthesizeSpeech(text, voiceId)
    }
    
    private fun buildConversationContext(context: FitnessContext): String {
        return """
        You are a personal fitness agent for a runner. Here's the current context:
        
        User Profile:
        - Experience Level: ${context.userProfile.experienceLevel}
        - Preferred Units: ${context.userProfile.preferredUnits}
        - Current Goal: ${context.currentGoal}
        
        Recent Activity:
        - Last Run: ${context.lastRun?.let { "${it.distance}km in ${it.duration.toMinutes()}min" } ?: "No recent runs"}
        - Weekly Distance: ${context.weeklyDistance}km
        - Training Load: ${context.trainingLoad}
        
        Current Context:
        - Time of Day: ${context.timeOfDay}
        - Weather: ${context.weather}
        - User Query: ${context.userQuery}
        
        Provide helpful, encouraging, and personalized fitness advice. Keep responses concise and actionable.
        """.trimIndent()
    }
    
    private fun buildMessages(query: String, context: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        
        // System message
        messages.add(ChatMessage(
            role = "system",
            content = agentContext
        ))
        
        // Context message
        messages.add(ChatMessage(
            role = "system",
            content = context
        ))
        
        // Conversation history (last 10 messages)
        conversationHistory.takeLast(10).forEach { message ->
            messages.add(ChatMessage(
                role = if (message.isUser) "user" else "assistant",
                content = message.content
            ))
        }
        
        // Current query
        messages.add(ChatMessage(
            role = "user",
            content = query
        ))
        
        return messages
    }
    
    private fun buildAgentContext(): String {
        return """
        You are a personal fitness agent for the RunningCoach app. Your role is to:
        
        1. Provide personalized fitness advice based on the user's profile and goals
        2. Answer questions about running, training, and fitness
        3. Offer motivation and encouragement
        4. Help users understand their performance data
        5. Suggest training adjustments when appropriate
        
        Guidelines:
        - Be encouraging and supportive
        - Provide actionable advice
        - Keep responses concise (under 100 words)
        - Use the user's preferred units (miles/kilometers)
        - Consider the user's experience level
        - Be mindful of safety and injury prevention
        
        You have access to the user's training history, current goals, and performance metrics.
        Use this information to provide personalized, relevant advice.
        """
    }
    
    private fun updateConversationHistory(userQuery: String, aiResponse: String) {
        conversationHistory.add(ConversationMessage(userQuery, true))
        conversationHistory.add(ConversationMessage(aiResponse, false))
        
        // Keep only last 20 messages to manage memory
        if (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
            conversationHistory.removeAt(0)
        }
    }
}

data class FitnessContext(
    val userProfile: UserProfile,
    val currentGoal: RunningGoal?,
    val lastRun: Run?,
    val weeklyDistance: Float,
    val trainingLoad: TrainingLoad?,
    val timeOfDay: String,
    val weather: WeatherInfo?,
    val userQuery: String
)

data class ConversationMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AgentResponse {
    data class Success(val textResponse: String, val audioResponse: AudioData) : AgentResponse()
    data class Error(val message: String) : AgentResponse()
}
```

## 🎵 Audio Management

### Audio Queue and Priority System
```kotlin
class AudioManager(
    private val context: Context,
    private val audioFocusManager: AudioFocusManager
) {
    
    private val audioQueue = PriorityQueue<AudioRequest>()
    private val mediaPlayer = MediaPlayer()
    private var isPlaying = false
    
    suspend fun playAudio(audioData: AudioData, priority: AudioPriority) {
        val audioRequest = AudioRequest(audioData, priority, System.currentTimeMillis())
        
        when (priority) {
            AudioPriority.CRITICAL -> {
                // Interrupt current audio and play immediately
                stopCurrentAudio()
                playAudioImmediately(audioRequest)
            }
            AudioPriority.HIGH -> {
                // Add to front of queue
                audioQueue.offer(audioRequest)
                if (!isPlaying) {
                    playNextInQueue()
                }
            }
            AudioPriority.NORMAL -> {
                // Add to queue
                audioQueue.offer(audioRequest)
                if (!isPlaying) {
                    playNextInQueue()
                }
            }
            AudioPriority.LOW -> {
                // Add to end of queue
                audioQueue.offer(audioRequest)
                if (!isPlaying) {
                    playNextInQueue()
                }
            }
        }
    }
    
    private suspend fun playAudioImmediately(audioRequest: AudioRequest) {
        try {
            // Request audio focus
            val focusResult = audioFocusManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                    .build()
            )
            
            if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Play audio
                mediaPlayer.reset()
                mediaPlayer.setDataSource(ByteArrayInputStream(audioRequest.audioData.audioBytes))
                mediaPlayer.prepare()
                mediaPlayer.start()
                
                isPlaying = true
                
                // Wait for completion
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    playNextInQueue()
                }
            }
        } catch (e: Exception) {
            // Handle error
            isPlaying = false
            playNextInQueue()
        }
    }
    
    private suspend fun playNextInQueue() {
        if (audioQueue.isEmpty() || isPlaying) return
        
        val nextRequest = audioQueue.poll()
        playAudioImmediately(nextRequest)
    }
    
    private fun stopCurrentAudio() {
        if (isPlaying) {
            mediaPlayer.stop()
            isPlaying = false
        }
    }
}

data class AudioRequest(
    val audioData: AudioData,
    val priority: AudioPriority,
    val timestamp: Long
) : Comparable<AudioRequest> {
    
    override fun compareTo(other: AudioRequest): Int {
        // Higher priority first, then by timestamp (FIFO for same priority)
        val priorityComparison = other.priority.ordinal.compareTo(this.priority.ordinal)
        return if (priorityComparison != 0) priorityComparison else timestamp.compareTo(other.timestamp)
    }
}
```

## 📊 Voice Analytics

### Voice Line Performance Tracking
```kotlin
@Entity(tableName = "voice_line_analytics")
data class VoiceLineAnalytics(
    @PrimaryKey val id: String,
    val voiceLineId: String,
    val userId: String,
    val playedAt: Long,
    val context: String,
    val userResponse: UserResponse?,
    val effectiveness: Float?,
    val completionRate: Float
)

enum class UserResponse {
    POSITIVE, NEUTRAL, NEGATIVE, IGNORED
}

class VoiceAnalyticsManager(
    private val analyticsDao: VoiceLineAnalyticsDao,
    private val voiceLineDatabase: VoiceLineDatabase
) {
    
    suspend fun trackVoiceLinePlayed(
        voiceLineId: String,
        userId: String,
        context: String
    ) {
        val analytics = VoiceLineAnalytics(
            id = generateAnalyticsId(),
            voiceLineId = voiceLineId,
            userId = userId,
            playedAt = System.currentTimeMillis(),
            context = context,
            userResponse = null,
            effectiveness = null,
            completionRate = 1.0f
        )
        
        analyticsDao.insertAnalytics(analytics)
    }
    
    suspend fun trackUserResponse(
        voiceLineId: String,
        userId: String,
        response: UserResponse
    ) {
        analyticsDao.updateUserResponse(voiceLineId, userId, response)
    }
    
    suspend fun calculateEffectiveness(voiceLineId: String): Float {
        val analytics = analyticsDao.getAnalyticsForVoiceLine(voiceLineId)
        
        if (analytics.isEmpty()) return 0.0f
        
        val positiveResponses = analytics.count { it.userResponse == UserResponse.POSITIVE }
        val totalResponses = analytics.count { it.userResponse != null }
        
        return if (totalResponses > 0) {
            positiveResponses.toFloat() / totalResponses
        } else {
            0.0f
        }
    }
    
    suspend fun getTopPerformingVoiceLines(
        category: VoiceLineCategory,
        limit: Int = 10
    ): List<VoiceLinePerformance> {
        return analyticsDao.getTopPerformingVoiceLines(category.name, limit)
    }
    
    suspend fun optimizeVoiceLineDatabase() {
        // Remove low-performing voice lines
        val lowPerformingVoiceLines = analyticsDao.getLowPerformingVoiceLines(0.3f)
        lowPerformingVoiceLines.forEach { analytics ->
            voiceLineDatabase.deleteVoiceLine(analytics.voiceLineId)
        }
        
        // Update effectiveness scores
        val allVoiceLines = analyticsDao.getAllVoiceLines()
        allVoiceLines.forEach { analytics ->
            val effectiveness = calculateEffectiveness(analytics.voiceLineId)
            analyticsDao.updateEffectiveness(analytics.voiceLineId, effectiveness)
        }
    }
}

data class VoiceLinePerformance(
    val voiceLineId: String,
    val text: String,
    val category: VoiceLineCategory,
    val usageCount: Int,
    val effectiveness: Float,
    val averageCompletionRate: Float
)
```

## 🔧 Configuration

### Voice System Configuration
```kotlin
object VoiceSystemConfig {
    // TTS Settings
    const val DEFAULT_VOICE_ID = "coach_bennett"
    const val MAX_VOICE_LINE_LENGTH = 100
    const val MIN_COACHING_INTERVAL = 30000L // 30 seconds
    
    // Audio Settings
    const val AUDIO_FADE_DURATION = 500L // milliseconds
    const val MAX_QUEUE_SIZE = 50
    const val AUDIO_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    
    // ML Settings
    const val ML_PREDICTION_THRESHOLD = 0.7f
    const val MAX_VOICE_LINES_PER_CONTEXT = 5
    
    // Database Settings
    const val VOICE_LINE_CACHE_DURATION = 30 * 24 * 60 * 60 * 1000L // 30 days
    const val MAX_VOICE_LINES_PER_USER = 1000
}
```

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Next Review**: February 2025
