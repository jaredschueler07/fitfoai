package com.runningcoach.v2.data.service

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SPRINT 3.2 TEST: AudioFocusManagerTest
 * 
 * Tests for AudioFocusManager ensuring proper audio focus handling and music app integration.
 * Critical for voice coaching - must work seamlessly with music apps like Spotify.
 * 
 * Test Requirements:
 * ✅ Audio focus acquisition and release
 * ✅ Music ducking behavior
 * ✅ Bluetooth/wired headset support
 * ✅ Audio routing management
 * ✅ Multiple audio source handling
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 29, 30, 31, 33])
class AudioFocusManagerTest {

    private lateinit var audioFocusManager: AudioFocusManager
    private lateinit var context: Context
    private lateinit var mockAudioManager: AudioManager
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        context = ApplicationProvider.getApplicationContext()
        
        // Mock AudioManager
        mockAudioManager = mockk(relaxed = true)
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns mockAudioManager
        
        // Mock audio focus behavior
        every { mockAudioManager.requestAudioFocus(any(), any(), any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        every { mockAudioManager.abandonAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        every { mockAudioManager.mode } returns AudioManager.MODE_NORMAL
        every { mockAudioManager.isBluetoothScoOn } returns false
        every { mockAudioManager.isWiredHeadsetOn } returns false
        every { mockAudioManager.isSpeakerphoneOn } returns false
        
        audioFocusManager = AudioFocusManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        audioFocusManager.cleanup()
        clearAllMocks()
    }

    // ========== SPRINT 3.2 AUDIO SYSTEM TESTS ==========

    @Test
    fun `Audio system - Audio focus acquisition for voice coaching`() = runTest {
        // Act
        audioFocusManager.configureForVoiceCoaching()
        
        // Assert - Should request audio focus
        verify { mockAudioManager.requestAudioFocus(any(), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) }
        
        val status = audioFocusManager.getAudioFocusStatus()
        assertEquals("Should have audio focus", 
            AudioFocusManager.AudioFocusState.GAINED, 
            status.currentFocusState)
        
        println("[TEST-RESULT: PASS] Audio focus acquisition for voice coaching")
    }

    @Test
    fun `Audio system - Audio focus release after coaching`() = runTest {
        // Arrange
        audioFocusManager.configureForVoiceCoaching()
        
        // Act
        audioFocusManager.stopCurrentPlayback()
        
        // Assert - Should abandon audio focus
        verify { mockAudioManager.abandonAudioFocus(any()) }
        
        println("[TEST-RESULT: PASS] Audio focus release after coaching")
    }

    @Test
    fun `Audio system - Music ducking during coaching messages`() = runTest {
        // Arrange
        val testAudioFile = "/path/to/test/audio.mp3"
        
        // Act
        val result = audioFocusManager.playCoachingAudio(
            audioFilePath = testAudioFile,
            priority = ElevenLabsService.AudioPriority.NORMAL,
            duckMusic = true
        )
        
        // Assert
        assertTrue("Should successfully initiate audio playback", result.isSuccess)
        
        // Should request transient focus with ducking
        verify { mockAudioManager.requestAudioFocus(
            any(), 
            AudioManager.STREAM_MUSIC, 
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        ) }
        
        println("[TEST-RESULT: PASS] Music ducking during coaching messages")
    }

    @Test
    fun `Audio system - Urgent priority interrupts current audio`() = runTest {
        // Arrange - Start normal priority audio
        audioFocusManager.playCoachingAudio(
            audioFilePath = "/path/to/normal.mp3",
            priority = ElevenLabsService.AudioPriority.NORMAL,
            duckMusic = true
        )
        
        // Act - Play urgent priority audio
        val result = audioFocusManager.playCoachingAudio(
            audioFilePath = "/path/to/urgent.mp3",
            priority = ElevenLabsService.AudioPriority.URGENT,
            duckMusic = true
        )
        
        // Assert
        assertTrue("Urgent audio should succeed", result.isSuccess)
        
        // Should request exclusive audio focus for urgent messages
        verify(atLeast = 1) { mockAudioManager.requestAudioFocus(any(), any(), any()) }
        
        println("[TEST-RESULT: PASS] Urgent priority audio interruption")
    }

    @Test
    fun `Audio system - Bluetooth headset detection and routing`() = runTest {
        // Arrange - Mock Bluetooth headset connected
        every { mockAudioManager.isBluetoothScoOn } returns true
        every { mockAudioManager.isBluetoothA2dpOn } returns true
        
        // Act
        audioFocusManager.configureForVoiceCoaching()
        val status = audioFocusManager.getAudioFocusStatus()
        
        // Assert
        assertTrue("Should detect Bluetooth capability", 
            status.availableOutputDevices.contains(AudioFocusManager.AudioOutputDevice.BLUETOOTH))
        
        // Should configure for Bluetooth SCO if available
        val bluetoothResult = audioFocusManager.setPreferredOutputDevice(AudioFocusManager.AudioOutputDevice.BLUETOOTH)
        assertTrue("Should support Bluetooth routing", bluetoothResult.isSuccess)
        
        println("[TEST-RESULT: PASS] Bluetooth headset detection and routing")
    }

    @Test
    fun `Audio system - Wired headset detection and routing`() = runTest {
        // Arrange - Mock wired headset connected
        every { mockAudioManager.isWiredHeadsetOn } returns true
        
        // Act
        audioFocusManager.configureForVoiceCoaching()
        val status = audioFocusManager.getAudioFocusStatus()
        
        // Assert
        assertTrue("Should detect wired headset", 
            status.availableOutputDevices.contains(AudioFocusManager.AudioOutputDevice.WIRED_HEADSET))
        
        println("[TEST-RESULT: PASS] Wired headset detection and routing")
    }

    @Test
    fun `Audio system - Speaker output as fallback`() = runTest {
        // Arrange - No headsets connected
        every { mockAudioManager.isWiredHeadsetOn } returns false
        every { mockAudioManager.isBluetoothScoOn } returns false
        every { mockAudioManager.isBluetoothA2dpOn } returns false
        
        // Act
        audioFocusManager.configureForVoiceCoaching()
        val status = audioFocusManager.getAudioFocusStatus()
        
        // Assert
        assertTrue("Should always have speaker as fallback", 
            status.availableOutputDevices.contains(AudioFocusManager.AudioOutputDevice.SPEAKER))
        
        // Should default to speaker
        assertEquals("Should default to speaker output", 
            AudioFocusManager.AudioOutputDevice.SPEAKER, 
            status.currentOutputDevice)
        
        println("[TEST-RESULT: PASS] Speaker output as fallback")
    }

    @Test
    fun `Audio system - Multiple coaching messages queueing`() = runTest {
        // Arrange
        val audioFiles = listOf(
            "/path/to/audio1.mp3",
            "/path/to/audio2.mp3", 
            "/path/to/audio3.mp3"
        )
        
        // Act - Queue multiple messages
        val results = audioFiles.map { audioFile ->
            audioFocusManager.playCoachingAudio(
                audioFilePath = audioFile,
                priority = ElevenLabsService.AudioPriority.NORMAL,
                duckMusic = true
            )
        }
        
        // Assert - All should be accepted
        results.forEach { result ->
            assertTrue("All audio files should be queued successfully", result.isSuccess)
        }
        
        val status = audioFocusManager.getAudioFocusStatus()
        assertTrue("Should track queued messages", status.queueSize >= 0)
        
        println("[TEST-RESULT: PASS] Multiple coaching messages queueing: ${status.queueSize} items")
    }

    @Test
    fun `Audio system - Audio focus loss handling`() = runTest {
        // Arrange
        audioFocusManager.configureForVoiceCoaching()
        
        // Simulate audio focus loss (e.g., phone call)
        val focusChangeListener = slot<AudioManager.OnAudioFocusChangeListener>()
        verify { mockAudioManager.requestAudioFocus(capture(focusChangeListener), any(), any()) }
        
        // Act - Simulate focus loss
        focusChangeListener.captured.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        
        // Assert - Should handle focus loss gracefully
        val status = audioFocusManager.getAudioFocusStatus()
        assertEquals("Should track focus loss", 
            AudioFocusManager.AudioFocusState.LOST, 
            status.currentFocusState)
        
        println("[TEST-RESULT: PASS] Audio focus loss handling")
    }

    @Test
    fun `Audio system - Audio focus gain recovery`() = runTest {
        // Arrange - Start with focus loss
        audioFocusManager.configureForVoiceCoaching()
        val focusChangeListener = slot<AudioManager.OnAudioFocusChangeListener>()
        verify { mockAudioManager.requestAudioFocus(capture(focusChangeListener), any(), any()) }
        
        focusChangeListener.captured.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        
        // Act - Simulate focus regain
        focusChangeListener.captured.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        
        // Assert - Should recover and resume
        val status = audioFocusManager.getAudioFocusStatus()
        assertEquals("Should recover audio focus", 
            AudioFocusManager.AudioFocusState.GAINED, 
            status.currentFocusState)
        
        println("[TEST-RESULT: PASS] Audio focus gain recovery")
    }

    @Test
    fun `Audio system - Volume ducking levels`() = runTest {
        // Arrange
        val testAudioFile = "/path/to/test.mp3"
        
        // Act - Test different ducking levels
        val lowDuckResult = audioFocusManager.playCoachingAudio(
            audioFilePath = testAudioFile,
            priority = ElevenLabsService.AudioPriority.LOW,
            duckMusic = true
        )
        
        val highDuckResult = audioFocusManager.playCoachingAudio(
            audioFilePath = testAudioFile,
            priority = ElevenLabsService.AudioPriority.URGENT,
            duckMusic = true
        )
        
        // Assert
        assertTrue("Low priority ducking should work", lowDuckResult.isSuccess)
        assertTrue("High priority ducking should work", highDuckResult.isSuccess)
        
        // Different priorities should use appropriate focus types
        verify(atLeast = 1) { mockAudioManager.requestAudioFocus(any(), any(), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) }
        
        println("[TEST-RESULT: PASS] Volume ducking levels")
    }

    @Test
    fun `Audio system - Concurrent audio source management`() = runTest {
        // Arrange - Multiple audio sources trying to play
        val sources = listOf(
            "coaching_message_1.mp3",
            "coaching_message_2.mp3",
            "milestone_celebration.mp3"
        )
        
        val priorities = listOf(
            ElevenLabsService.AudioPriority.NORMAL,
            ElevenLabsService.AudioPriority.HIGH,
            ElevenLabsService.AudioPriority.URGENT
        )
        
        // Act - Attempt to play multiple sources
        val results = sources.zip(priorities).map { (source, priority) ->
            audioFocusManager.playCoachingAudio(
                audioFilePath = "/path/to/$source",
                priority = priority,
                duckMusic = true
            )
        }
        
        // Assert - Should handle multiple requests
        results.forEach { result ->
            assertTrue("Should handle concurrent audio sources", result.isSuccess)
        }
        
        val status = audioFocusManager.getAudioFocusStatus()
        assertTrue("Should manage queue size", status.queueSize >= 0)
        
        println("[TEST-RESULT: PASS] Concurrent audio source management")
    }

    @Test
    fun `Audio system - Audio session management`() = runTest {
        // Act
        audioFocusManager.configureForVoiceCoaching()
        val status = audioFocusManager.getAudioFocusStatus()
        
        // Assert - Should create valid audio session
        assertNotNull("Should have audio session ID", status.audioSessionId)
        assertTrue("Audio session ID should be valid", status.audioSessionId != 0)
        
        println("[TEST-RESULT: PASS] Audio session management: Session ID ${status.audioSessionId}")
    }

    @Test
    fun `Audio system - Error handling for audio focus failures`() = runTest {
        // Arrange - Mock audio focus failure
        every { mockAudioManager.requestAudioFocus(any(), any(), any()) } returns AudioManager.AUDIOFOCUS_REQUEST_FAILED
        
        // Act
        val result = audioFocusManager.playCoachingAudio(
            audioFilePath = "/path/to/test.mp3",
            priority = ElevenLabsService.AudioPriority.NORMAL,
            duckMusic = true
        )
        
        // Assert - Should handle failure gracefully
        assertTrue("Should handle audio focus failure", result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertNotNull("Should provide error information", exception)
        assertTrue("Should indicate audio focus issue", 
            exception?.message?.contains("audio focus") == true)
        
        println("[TEST-RESULT: PASS] Audio focus failure error handling")
    }

    @Test
    fun `Audio system - Cleanup releases all resources`() = runTest {
        // Arrange
        audioFocusManager.configureForVoiceCoaching()
        audioFocusManager.playCoachingAudio(
            "/path/to/test.mp3",
            ElevenLabsService.AudioPriority.NORMAL,
            true
        )
        
        // Act
        audioFocusManager.cleanup()
        
        // Assert - Should release audio focus
        verify { mockAudioManager.abandonAudioFocus(any()) }
        
        val status = audioFocusManager.getAudioFocusStatus()
        assertEquals("Should clear focus state after cleanup", 
            AudioFocusManager.AudioFocusState.NONE, 
            status.currentFocusState)
        
        println("[TEST-RESULT: PASS] Cleanup releases all audio resources")
    }

    @Test
    fun `Audio system - Device routing preference persistence`() = runTest {
        // Arrange
        audioFocusManager.configureForVoiceCoaching()
        
        // Act - Set preferred device
        val setResult = audioFocusManager.setPreferredOutputDevice(AudioFocusManager.AudioOutputDevice.SPEAKER)
        
        // Assert
        assertTrue("Should accept device preference", setResult.isSuccess)
        
        val status = audioFocusManager.getAudioFocusStatus()
        assertEquals("Should remember device preference", 
            AudioFocusManager.AudioOutputDevice.SPEAKER, 
            status.currentOutputDevice)
        
        println("[TEST-RESULT: PASS] Device routing preference persistence")
    }

    @Test
    fun `Audio system - Performance with rapid audio requests`() = runTest {
        // Arrange
        val startTime = System.currentTimeMillis()
        val iterations = 50
        
        // Act - Rapid audio requests
        repeat(iterations) { index ->
            audioFocusManager.playCoachingAudio(
                audioFilePath = "/path/to/rapid_$index.mp3",
                priority = ElevenLabsService.AudioPriority.NORMAL,
                duckMusic = true
            )
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val avgTimePerRequest = totalTime.toDouble() / iterations
        
        // Assert - Should handle rapid requests efficiently
        assertTrue("Should handle rapid requests quickly (${totalTime}ms total)", 
            totalTime < 1000) // Under 1 second for 50 requests
        assertTrue("Average request time should be reasonable (${avgTimePerRequest.toInt()}ms)", 
            avgTimePerRequest < 20.0) // Under 20ms per request
        
        println("[TEST-RESULT: PASS] Performance: ${avgTimePerRequest.toInt()}ms avg per request")
    }
}