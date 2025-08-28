package com.runningcoach.v2.data.service

import android.content.Context
import android.media.MediaPlayer
import com.runningcoach.v2.BuildConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import io.ktor.client.plugins.*
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ElevenLabsService(
    private val httpClient: HttpClient,
    private val context: Context
) {
    
    private val baseUrl = "https://api.elevenlabs.io/v1"
    private val apiKey = BuildConfig.ELEVENLABS_API_KEY
    
    // Pre-defined voice IDs and personalities for the 4 coach types
    private val coachVoices = mapOf(
        "bennett" to CoachPersonality(
            voiceId = "pNInz6obpgDQGcFmaJgB", // Professional male voice
            name = "Bennett",
            description = "Professional, encouraging, data-driven",
            voiceSettings = VoiceSettings(stability = 0.85f, similarity_boost = 0.80f, style = 0.1f)
        ),
        "mariana" to CoachPersonality(
            voiceId = "EXAVITQu4vr4xnSDxMaL", // Energetic female voice
            name = "Mariana",
            description = "Energetic, motivational, upbeat",
            voiceSettings = VoiceSettings(stability = 0.70f, similarity_boost = 0.90f, style = 0.4f)
        ),
        "becs" to CoachPersonality(
            voiceId = "oWAxZDx7w5VEj9dCyTzz", // Calm female voice
            name = "Becs",
            description = "Calm, supportive, mindful",
            voiceSettings = VoiceSettings(stability = 0.90f, similarity_boost = 0.75f, style = 0.0f)
        ),
        "goggins" to CoachPersonality(
            voiceId = "VR6AewLTigWG4xSOukaG", // Intense male voice
            name = "Goggins",
            description = "Intense, challenging, no-excuses",
            voiceSettings = VoiceSettings(stability = 0.65f, similarity_boost = 0.95f, style = 0.6f)
        ),
        "ai_coach" to CoachPersonality(
            voiceId = "pNInz6obpgDQGcFmaJgB", // Default to Bennett
            name = "AI Coach",
            description = "Default AI coach",
            voiceSettings = VoiceSettings(stability = 0.75f, similarity_boost = 0.85f, style = 0.2f)
        )
    )
    
    @Serializable
    data class TTSRequest(
        val text: String,
        val model_id: String = "eleven_monolingual_v1",
        val voice_settings: VoiceSettings = VoiceSettings()
    )
    
    @Serializable
    data class VoiceSettings(
        val stability: Float = 0.75f,
        val similarity_boost: Float = 0.85f,
        val style: Float = 0.0f,
        val use_speaker_boost: Boolean = true
    )
    
    data class CoachPersonality(
        val voiceId: String,
        val name: String,
        val description: String,
        val voiceSettings: VoiceSettings
    )
    
    private var currentMediaPlayer: MediaPlayer? = null
    
    suspend fun generateSpeech(
        text: String,
        coachId: String = "ai_coach",
        playImmediately: Boolean = true,
        priority: AudioPriority = AudioPriority.NORMAL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val coachPersonality = coachVoices[coachId] ?: coachVoices["ai_coach"]!!
            
            // Personalize text based on coach personality
            val personalizedText = personalizeTextForCoach(text, coachId)
            
            val request = TTSRequest(
                text = personalizedText,
                voice_settings = coachPersonality.voiceSettings
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/text-to-speech/${coachPersonality.voiceId}") {
                header("xi-api-key", apiKey)
                header("Content-Type", "application/json")
                setBody(request)
                timeout {
                    requestTimeoutMillis = 10000 // 10s timeout for latency requirements
                    connectTimeoutMillis = 5000   // 5s connection timeout
                }
            }
            
            if (response.status.isSuccess()) {
                val audioBytes = response.readBytes()
                val latency = System.currentTimeMillis() - startTime
                val audioFile = saveAudioToFile(audioBytes, "${coachId}_speech_${System.currentTimeMillis()}.mp3")
                
                // Log latency for performance monitoring
                if (latency > 200) {
                    println("[VOICE-COACHING] High latency detected: ${latency}ms for $coachId")
                }
                
                if (playImmediately) {
                    playAudio(audioFile, priority)
                }
                
                Result.success(audioFile)
            } else {
                // Enhanced error handling with retry logic
                val errorMsg = "TTS generation failed: ${response.status} - ${response.bodyAsText()}"
                println("[VOICE-COACHING] API Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateCoachingAudio(
        coachingText: String,
        coachId: String = "ai_coach",
        urgency: CoachingUrgency = CoachingUrgency.NORMAL,
        priority: AudioPriority = AudioPriority.NORMAL,
        cacheKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val coachPersonality = coachVoices[coachId] ?: coachVoices["ai_coach"]!!
            
            // Check cache first if cache key provided
            cacheKey?.let { key ->
                getCachedVoiceLine(key, coachId)?.let { cachedFile ->
                    return@withContext Result.success(cachedFile)
                }
            }
            
            // Personalize text based on coach personality and urgency
            val personalizedText = personalizeTextForCoach(coachingText, coachId, urgency)
            
            // Apply urgency modifiers to voice settings
            val urgencyModifiedSettings = applyUrgencyToVoiceSettings(coachPersonality.voiceSettings, urgency)
            
            val request = TTSRequest(
                text = personalizedText,
                voice_settings = urgencyModifiedSettings
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/text-to-speech/${coachPersonality.voiceId}") {
                header("xi-api-key", apiKey)
                header("Content-Type", "application/json")
                setBody(request)
                timeout {
                    requestTimeoutMillis = 10000 // 10s timeout for latency requirements
                    connectTimeoutMillis = 5000   // 5s connection timeout
                }
            }
            
            if (response.status.isSuccess()) {
                val audioBytes = response.readBytes()
                val latency = System.currentTimeMillis() - startTime
                val fileName = "${coachId}_${urgency.name.lowercase()}_${System.currentTimeMillis()}.mp3"
                val audioFile = saveAudioToFile(audioBytes, fileName)
                
                // Cache the audio file if cache key provided
                cacheKey?.let { key ->
                    cacheVoiceLine(key, coachId, audioFile, personalizedText)
                }
                
                // Log latency for performance monitoring
                println("[VOICE-COACHING] Generated audio for $coachId in ${latency}ms")
                if (latency > 200) {
                    println("[VOICE-COACHING] WARNING: Latency ${latency}ms exceeds 200ms target")
                }
                
                Result.success(audioFile)
            } else {
                val errorMsg = "Coaching audio generation failed: ${response.status} - ${response.bodyAsText()}"
                println("[VOICE-COACHING] API Error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveAudioToFile(audioBytes: ByteArray, filename: String): String = withContext(Dispatchers.IO) {
        val audioDir = File(context.cacheDir, "coach_audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        
        val audioFile = File(audioDir, filename)
        FileOutputStream(audioFile).use { output ->
            output.write(audioBytes)
        }
        
        audioFile.absolutePath
    }
    
    suspend fun playAudio(
        audioFilePath: String, 
        priority: AudioPriority = AudioPriority.NORMAL
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Handle audio priority - stop current audio only if new audio has higher priority
            if (currentMediaPlayer?.isPlaying == true) {
                if (priority == AudioPriority.URGENT) {
                    stopCurrentAudio()
                } else {
                    // Queue the audio instead of playing immediately
                    return@withContext Result.failure(Exception("Audio already playing, use urgent priority to override"))
                }
            }
            
            currentMediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepareAsync()
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                    println("[VOICE-COACHING] Started playback: ${audioFilePath.substringAfterLast("/")}")
                }
                setOnCompletionListener { mediaPlayer ->
                    mediaPlayer.release()
                    currentMediaPlayer = null
                    println("[VOICE-COACHING] Playback completed")
                }
                setOnErrorListener { mediaPlayer, what, extra ->
                    println("[VOICE-COACHING] Playback error: what=$what, extra=$extra")
                    mediaPlayer.release()
                    currentMediaPlayer = null
                    false
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("[VOICE-COACHING] Playback error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun stopCurrentAudio() {
        currentMediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            currentMediaPlayer = null
        }
    }
    
    fun isPlaying(): Boolean {
        return currentMediaPlayer?.isPlaying == true
    }
    
    suspend fun preloadCoachingPhrases(coachId: String = "ai_coach"): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        val commonPhrases = getCoachPersonalityPhrases(coachId)
        val audioFiles = mutableMapOf<String, String>()
        
        try {
            println("[VOICE-COACHING] Preloading ${commonPhrases.size} phrases for $coachId")
            
            commonPhrases.forEachIndexed { index, phrase ->
                val cacheKey = "preload_${coachId}_${index}"
                val result = generateCoachingAudio(
                    coachingText = phrase,
                    coachId = coachId,
                    cacheKey = cacheKey,
                    urgency = CoachingUrgency.NORMAL
                )
                
                result.getOrNull()?.let { audioFile ->
                    audioFiles[phrase] = audioFile
                    println("[VOICE-COACHING] Cached: $phrase")
                }
            }
            
            println("[VOICE-COACHING] Preloaded ${audioFiles.size}/${commonPhrases.size} phrases for $coachId")
            Result.success(audioFiles)
        } catch (e: Exception) {
            println("[VOICE-COACHING] Preload error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun clearAudioCache() {
        val audioDir = File(context.cacheDir, "coach_audio")
        if (audioDir.exists()) {
            audioDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }
    
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (apiKey.isBlank()) {
                Result.failure(Exception("API key not configured"))
            } else {
                // Test with a simple API call to get voice list
                val response: HttpResponse = httpClient.get("https://api.elevenlabs.io/v1/voices") {
                    headers {
                        append("xi-api-key", apiKey)
                    }
                }
                
                if (response.status.isSuccess()) {
                    Result.success("Connection successful - API key valid")
                } else {
                    Result.failure(Exception("API call failed with status: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Enhanced coaching urgency system
    enum class CoachingUrgency {
        CALM,       // Gentle encouragement (Becs style)
        NORMAL,     // Standard coaching (Bennett style)
        ENERGETIC,  // High energy motivation (Mariana style)
        URGENT      // Critical pace/form corrections (Goggins style)
    }
    
    enum class AudioPriority {
        LOW,        // Can be queued or skipped
        NORMAL,     // Standard priority
        HIGH,       // Important coaching
        URGENT      // Critical feedback, interrupts current audio
    }
    
    // Helper functions for coach personality system
    private fun personalizeTextForCoach(
        text: String, 
        coachId: String, 
        urgency: CoachingUrgency = CoachingUrgency.NORMAL
    ): String {
        val personality = coachVoices[coachId] ?: return text
        
        return when (coachId) {
            "bennett" -> addBennettPersonality(text, urgency)
            "mariana" -> addMarianaPersonality(text, urgency)
            "becs" -> addBecsPersonality(text, urgency)
            "goggins" -> addGogginsPersonality(text, urgency)
            else -> text
        }
    }
    
    private fun addBennettPersonality(text: String, urgency: CoachingUrgency): String {
        val prefix = when (urgency) {
            CoachingUrgency.CALM -> "Let's analyze your data: "
            CoachingUrgency.NORMAL -> "Based on your metrics, "
            CoachingUrgency.ENERGETIC -> "Your numbers look strong! "
            CoachingUrgency.URGENT -> "Data shows immediate adjustment needed: "
        }
        return prefix + text
    }
    
    private fun addMarianaPersonality(text: String, urgency: CoachingUrgency): String {
        val prefix = when (urgency) {
            CoachingUrgency.CALM -> "Hey there! "
            CoachingUrgency.NORMAL -> "You're doing great! "
            CoachingUrgency.ENERGETIC -> "YES! Let's go! "
            CoachingUrgency.URGENT -> "Time to power up! "
        }
        return prefix + text + when (urgency) {
            CoachingUrgency.ENERGETIC, CoachingUrgency.URGENT -> " You've got this!"
            else -> ""
        }
    }
    
    private fun addBecsPersonality(text: String, urgency: CoachingUrgency): String {
        val prefix = when (urgency) {
            CoachingUrgency.CALM -> "Take a deep breath. "
            CoachingUrgency.NORMAL -> "Listen to your body. "
            CoachingUrgency.ENERGETIC -> "Feel your strength. "
            CoachingUrgency.URGENT -> "Stay centered and focus. "
        }
        return prefix + text + " Remember to stay present."
    }
    
    private fun addGogginsPersonality(text: String, urgency: CoachingUrgency): String {
        val prefix = when (urgency) {
            CoachingUrgency.CALM -> "Stay hard. "
            CoachingUrgency.NORMAL -> "No excuses! "
            CoachingUrgency.ENERGETIC -> "WHO'S GONNA CARRY THE BOATS?! "
            CoachingUrgency.URGENT -> "STOP BEING SOFT! "
        }
        return prefix + text + " Stay hard!"
    }
    
    private fun getCoachPersonalityPhrases(coachId: String): List<String> {
        return when (coachId) {
            "bennett" -> listOf(
                "Your pace data looks solid",
                "Metrics indicate you're in the optimal zone", 
                "Performance analysis shows good consistency",
                "Your form efficiency is improving",
                "Data-driven improvement in progress"
            )
            "mariana" -> listOf(
                "You're absolutely crushing it!",
                "This energy is AMAZING!",
                "Feel that power flowing through you!",
                "Your strength is incredible today!",
                "Keep that beautiful rhythm going!"
            )
            "becs" -> listOf(
                "Feel your breath, feel your body",
                "You're moving with beautiful awareness",
                "Trust in your body's wisdom", 
                "Each step is meditation in motion",
                "You're exactly where you need to be"
            )
            "goggins" -> listOf(
                "This is where champions are made!",
                "Pain is weakness leaving the body!",
                "You're tougher than your excuses!",
                "Embrace the suck and keep moving!",
                "Stay hard! No giving up!"
            )
            else -> listOf(
                "Great pace! Keep it up!",
                "You're doing amazing! Stay strong!",
                "Focus on your breathing",
                "Maintain your rhythm",
                "You've got this!"
            )
        }
    }
    
    private fun applyUrgencyToVoiceSettings(
        baseSettings: VoiceSettings, 
        urgency: CoachingUrgency
    ): VoiceSettings {
        return when (urgency) {
            CoachingUrgency.CALM -> baseSettings.copy(
                stability = (baseSettings.stability + 0.1f).coerceAtMost(1.0f),
                style = (baseSettings.style - 0.1f).coerceAtLeast(0.0f)
            )
            CoachingUrgency.NORMAL -> baseSettings
            CoachingUrgency.ENERGETIC -> baseSettings.copy(
                style = (baseSettings.style + 0.2f).coerceAtMost(1.0f),
                stability = (baseSettings.stability - 0.1f).coerceAtLeast(0.0f)
            )
            CoachingUrgency.URGENT -> baseSettings.copy(
                style = (baseSettings.style + 0.3f).coerceAtMost(1.0f),
                stability = (baseSettings.stability - 0.2f).coerceAtLeast(0.0f)
            )
        }
    }
    
    // Voice cache management methods
    private var voiceCache = mutableMapOf<String, String>()
    
    private fun getCachedVoiceLine(cacheKey: String, coachId: String): String? {
        val fullKey = "${coachId}_$cacheKey"
        return voiceCache[fullKey]?.let { filePath ->
            if (File(filePath).exists()) filePath else {
                voiceCache.remove(fullKey)
                null
            }
        }
    }
    
    private fun cacheVoiceLine(cacheKey: String, coachId: String, audioFilePath: String, text: String) {
        val fullKey = "${coachId}_$cacheKey"
        voiceCache[fullKey] = audioFilePath
        println("[VOICE-COACHING] Cached voice line: $fullKey -> $text")
    }
    
    fun getCoachPersonalities(): Map<String, CoachPersonality> = coachVoices
    
    suspend fun testVoiceGeneration(coachId: String): Result<String> {
        val testPhrase = getCoachPersonalityPhrases(coachId).first()
        return generateCoachingAudio(
            coachingText = testPhrase,
            coachId = coachId,
            urgency = CoachingUrgency.NORMAL
        )
    }
    
    fun getVoiceCacheStatus(): Map<String, Int> {
        return coachVoices.keys.associateWith { coachId ->
            voiceCache.keys.count { it.startsWith("${coachId}_") }
        }
    }
}
