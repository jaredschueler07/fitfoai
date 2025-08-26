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
    
    // Pre-defined voice IDs (you can get these from ElevenLabs dashboard)
    private val coachVoices = mapOf(
        "bennett" to "pNInz6obpgDQGcFmaJgB", // Professional male voice
        "sarah" to "EXAVITQu4vr4xnSDxMaL", // Energetic female voice  
        "mike" to "VR6AewLTigWG4xSOukaG", // Experienced male voice
        "emma" to "oWAxZDx7w5VEj9dCyTzz", // Motivational female voice
        "ai_coach" to "pNInz6obpgDQGcFmaJgB" // Default AI coach voice
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
    
    private var currentMediaPlayer: MediaPlayer? = null
    
    suspend fun generateSpeech(
        text: String,
        coachId: String = "ai_coach",
        playImmediately: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val voiceId = coachVoices[coachId] ?: coachVoices["ai_coach"]!!
            
            val request = TTSRequest(
                text = text,
                voice_settings = VoiceSettings(
                    stability = 0.8f,
                    similarity_boost = 0.9f,
                    style = 0.2f, // Slight style for coaching energy
                    use_speaker_boost = true
                )
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/text-to-speech/$voiceId") {
                header("xi-api-key", apiKey)
                header("Content-Type", "application/json")
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val audioBytes = response.readBytes()
                val audioFile = saveAudioToFile(audioBytes, "coach_speech_${System.currentTimeMillis()}.mp3")
                
                if (playImmediately) {
                    playAudio(audioFile)
                }
                
                Result.success(audioFile)
            } else {
                Result.failure(Exception("TTS generation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateCoachingAudio(
        coachingText: String,
        coachId: String = "ai_coach",
        urgency: CoachingUrgency = CoachingUrgency.NORMAL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val voiceSettings = when (urgency) {
                CoachingUrgency.CALM -> VoiceSettings(
                    stability = 0.9f,
                    similarity_boost = 0.8f,
                    style = 0.0f
                )
                CoachingUrgency.NORMAL -> VoiceSettings(
                    stability = 0.8f,
                    similarity_boost = 0.85f,
                    style = 0.2f
                )
                CoachingUrgency.ENERGETIC -> VoiceSettings(
                    stability = 0.7f,
                    similarity_boost = 0.9f,
                    style = 0.4f
                )
                CoachingUrgency.URGENT -> VoiceSettings(
                    stability = 0.6f,
                    similarity_boost = 0.95f,
                    style = 0.6f
                )
            }
            
            val voiceId = coachVoices[coachId] ?: coachVoices["ai_coach"]!!
            
            val request = TTSRequest(
                text = coachingText,
                voice_settings = voiceSettings
            )
            
            val response: HttpResponse = httpClient.post("$baseUrl/text-to-speech/$voiceId") {
                header("xi-api-key", apiKey)
                header("Content-Type", "application/json")
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val audioBytes = response.readBytes()
                val audioFile = saveAudioToFile(audioBytes, "coaching_${urgency.name.lowercase()}_${System.currentTimeMillis()}.mp3")
                
                Result.success(audioFile)
            } else {
                Result.failure(Exception("Coaching audio generation failed: ${response.status}"))
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
    
    suspend fun playAudio(audioFilePath: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Stop any currently playing audio
            stopCurrentAudio()
            
            currentMediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepareAsync()
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                }
                setOnCompletionListener { mediaPlayer ->
                    mediaPlayer.release()
                    currentMediaPlayer = null
                }
                setOnErrorListener { mediaPlayer, what, extra ->
                    mediaPlayer.release()
                    currentMediaPlayer = null
                    false
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
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
        val commonPhrases = listOf(
            "Great pace! Keep it up!",
            "You're doing amazing! Stay strong!",
            "Focus on your breathing",
            "Maintain your rhythm",
            "You've got this!",
            "Excellent form!",
            "Push through, you're almost there!",
            "Relax your shoulders",
            "Find your flow",
            "Outstanding effort!"
        )
        
        val audioFiles = mutableMapOf<String, String>()
        
        try {
            commonPhrases.forEach { phrase ->
                val result = generateSpeech(phrase, coachId, playImmediately = false)
                result.getOrNull()?.let { audioFile ->
                    audioFiles[phrase] = audioFile
                }
            }
            Result.success(audioFiles)
        } catch (e: Exception) {
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
    
    enum class CoachingUrgency {
        CALM,       // Gentle encouragement
        NORMAL,     // Standard coaching
        ENERGETIC,  // High energy motivation
        URGENT      // Critical pace/form corrections
    }
}
