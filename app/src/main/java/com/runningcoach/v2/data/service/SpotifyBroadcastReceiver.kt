package com.runningcoach.v2.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for Spotify app activity detection
 *
 * Listens for "com.spotify.music.active" broadcasts to detect when Spotify is active
 * and coordinate with our music integration features.
 *
 * Reference: https://developer.spotify.com/documentation/android/tutorials/broadcast-receivers
 */
class SpotifyBroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.spotify.music.active" -> {
                // Spotify app became active - a new track started playing
                scope.launch {
                    handleSpotifyActive(context)
                }
            }
        }
    }

    /**
     * Handle Spotify becoming active (new track started)
     */
    private suspend fun handleSpotifyActive(context: Context) {
        try {
            // Get the Spotify service instance (would need dependency injection in production)
            val spotifyService = SpotifyService(context, io.ktor.client.HttpClient())

            if (spotifyService.isConnected.value) {
                // Refresh current track information
                val currentTrack = spotifyService.getCurrentTrack().getOrNull()
                val playbackState = spotifyService.getPlaybackState().getOrNull()

                // Log for debugging (replace with proper logging in production)
                println("Spotify broadcast received - Track: ${currentTrack?.name ?: "Unknown"}")
                println("Playback state: ${playbackState?.isPlaying ?: false}")

                // Could trigger BPM analysis or other music-aware features here
                // For example: analyze BPM matching, update UI, trigger coaching coordination
            }

        } catch (e: Exception) {
            // Log error but don't crash the broadcast receiver
            println("Error handling Spotify broadcast: ${e.message}")
        }
    }
}
