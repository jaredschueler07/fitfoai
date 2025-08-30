package com.runningcoach.v2.data.service

import android.content.Context
import android.content.Intent
import com.spotify.android.appremote.api.*
import com.spotify.android.appremote.api.error.*
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.*
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.runningcoach.v2.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Official Spotify Android SDK implementation
 *
 * This service properly implements the Spotify Android SDK using:
 * - Spotify Auth library for OAuth 2.0 + PKCE
 * - Spotify App Remote for playback control and metadata
 * - Proper error handling and connection management
 *
 * Reference: https://developer.spotify.com/documentation/android
 */
class SpotifySdkService(private val context: Context) {

    companion object {
        private const val CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
        private const val REDIRECT_URI = BuildConfig.SPOTIFY_REDIRECT_URI
        private const val REQUEST_CODE_AUTH = 1337

        // Spotify scopes
        private val SCOPES = arrayOf(
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "playlist-read-private",
            "playlist-modify-public",
            "playlist-modify-private",
            "user-read-recently-played",
            "user-top-read",
            "user-read-private"
        )
    }

    // Error handling - using UnifiedErrorHandler
    private val unifiedErrorHandler = UnifiedErrorHandler.getInstance()
    
    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Not connected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _lastError = MutableStateFlow<UnifiedErrorHandler.UnifiedError?>(null)
    val lastError: StateFlow<UnifiedErrorHandler.UnifiedError?> = _lastError.asStateFlow()

    // Spotify App Remote instance
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Current track and playback state
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState?>(null)
    val playbackState: StateFlow<PlaybackState?> = _playbackState.asStateFlow()

    // Subscriptions for real-time updates
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var playerContextSubscription: Subscription<PlayerContext>? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // System status exposure
    val systemStatus = unifiedErrorHandler.systemStatus
    val errorHistory = unifiedErrorHandler.errorHistory
    
    init {
        updateSpotifySystemStatus()
    }

    /**
     * Initialize OAuth flow with Spotify Auth SDK
     */
    fun initiateAuth(): Intent {
        _connectionStatus.value = "Authenticating..."

        val request = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.CODE,
            REDIRECT_URI
        ).apply {
            setScopes(SCOPES)
            setShowDialog(true)
        }.build()

        return AuthorizationClient.createLoginActivityIntent(context, request)
    }

    /**
     * Handle OAuth callback from Spotify Auth SDK
     */
    fun handleAuthResponse(requestCode: Int, resultCode: Int, data: Intent?): Result<String> {
        if (requestCode == REQUEST_CODE_AUTH) {
            val response = AuthorizationClient.getResponse(resultCode, data)

            return when (response.type) {
                AuthorizationResponse.Type.CODE -> {
                    // Successfully got authorization code
                    _connectionStatus.value = "Authorization successful"
                    connectToSpotifyAppRemote(response.code)
                    Result.success("Successfully authorized")
                }
                AuthorizationResponse.Type.ERROR,
                AuthorizationResponse.Type.EMPTY -> {
                    val spotifyError = SpotifyErrorHandler.handleAuthError(response)
                    val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
                    _connectionStatus.value = unifiedError.userMessage
                    _lastError.value = unifiedError
                    updateSpotifySystemStatus()
                    Result.failure(spotifyError)
                }
                else -> {
                    val spotifyError = SpotifyErrorHandler.SpotifyError.UnknownError("Unexpected authorization response")
                    val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
                    _connectionStatus.value = unifiedError.userMessage
                    _lastError.value = unifiedError
                    updateSpotifySystemStatus()
                    Result.failure(spotifyError)
                }
            }
        }

        val spotifyError = SpotifyErrorHandler.SpotifyError.AuthorizationError("Invalid request code")
        val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
        _lastError.value = unifiedError
        updateSpotifySystemStatus()
        return Result.failure(spotifyError)
    }

    /**
     * Connect to Spotify App Remote
     */
    private fun connectToSpotifyAppRemote(authCode: String) {
        _connectionStatus.value = "Connecting to Spotify..."

        // Check if Spotify app is installed first
        if (!isSpotifyInstalled()) {
            val spotifyError = SpotifyErrorHandler.SpotifyError.SpotifyAppNotInstalledError()
            val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
            _connectionStatus.value = unifiedError.userMessage
            _lastError.value = unifiedError
            updateSpotifySystemStatus()
            return
        }

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                _isConnected.value = true
                _connectionStatus.value = "Connected to Spotify"
                _lastError.value = null // Clear previous errors on successful connection
                updateSpotifySystemStatus()
                setupSubscriptions()
            }

            override fun onFailure(error: Throwable) {
                val spotifyError = SpotifyErrorHandler.handleConnectionError(error)
                val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
                _connectionStatus.value = unifiedError.userMessage
                _lastError.value = unifiedError
                _isConnected.value = false
                updateSpotifySystemStatus()

                // Log technical details for debugging
                println("Spotify connection failed: ${unifiedError.message}")
                error.printStackTrace()
            }
        })
    }

    /**
     * Setup real-time subscriptions for player state and context
     */
    private fun setupSubscriptions() {
        spotifyAppRemote?.let { appRemote ->

            // Subscribe to player state changes
            playerStateSubscription = appRemote.playerApi.subscribeToPlayerState().apply {
                setEventCallback { playerState ->
                    scope.launch {
                        _currentTrack.value = playerState.track
                        _playbackState.value = PlaybackState(
                            isPlaying = playerState.isPaused.not(),
                            progressMs = playerState.playbackPosition,
                            durationMs = playerState.track.duration,
                            shuffleEnabled = playerState.playbackOptions.isShuffling,
                            repeatMode = playerState.playbackOptions.repeatMode.name,
                            device = playerState.playbackRestrictions?.let { restrictions ->
                                Device(
                                    id = "current_device",
                                    name = "Spotify Device",
                                    type = "smartphone",
                                    isActive = true,
                                    volumePercent = 100 // Would need to get actual volume
                                )
                            }
                        )
                    }
                }.setErrorCallback { error ->
                    println("Player state subscription error: ${error.message}")
                }
            }

            // Subscribe to player context changes
            playerContextSubscription = appRemote.playerApi.subscribeToPlayerContext().apply {
                setEventCallback { playerContext ->
                    // Handle context changes (playlist, album, etc.)
                    println("Player context changed: ${playerContext.uri}")
                }.setErrorCallback { error ->
                    println("Player context subscription error: ${error.message}")
                }
            }
        }
    }

    /**
     * Control playback using Spotify App Remote
     */
    suspend fun togglePlayback(): Result<String> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.let { playerApi ->
                val result = if (playbackState.value?.isPlaying == true) {
                    playerApi.pause()
                } else {
                    playerApi.resume()
                }

                when (result.result) {
                    CallResult.Result.SUCCESS -> {
                        val action = if (playbackState.value?.isPlaying == true) "paused" else "resumed"
                        Result.success("Playback $action")
                    }
                    CallResult.Result.ERROR -> {
                        val spotifyError = SpotifyErrorHandler.handleApiResultError("Toggle playback", result.error)
                        val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
                        _lastError.value = unifiedError
                        Result.failure(spotifyError)
                    }
                    else -> {
                        val spotifyError = SpotifyErrorHandler.SpotifyError.PlaybackError("Playback control failed with unknown result")
                        val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
                        _lastError.value = unifiedError
                        Result.failure(spotifyError)
                    }
                }
            } ?: run {
                val spotifyError = SpotifyErrorHandler.SpotifyError.ConnectionError("Spotify not connected")
                val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
                _lastError.value = unifiedError
                Result.failure(spotifyError)
            }
        } catch (e: Exception) {
            val spotifyError = SpotifyErrorHandler.handleGenericError("Toggle playback", e)
            val unifiedError = unifiedErrorHandler.handleSpotifyError(spotifyError)
            _lastError.value = unifiedError
            Result.failure(spotifyError)
        }
    }

    suspend fun skipToNext(): Result<String> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.skipNext()?.let { result ->
                when (result.result) {
                    CallResult.Result.SUCCESS -> Result.success("Skipped to next track")
                    else -> Result.failure(Exception("Skip next failed"))
                }
            } ?: Result.failure(Exception("Spotify not connected"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun skipToPrevious(): Result<String> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.skipPrevious()?.let { result ->
                when (result.result) {
                    CallResult.Result.SUCCESS -> Result.success("Skipped to previous track")
                    else -> Result.failure(Exception("Skip previous failed"))
                }
            } ?: Result.failure(Exception("Spotify not connected"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setVolume(volumePercent: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.setVolume(volumePercent.toFloat())?.let { result ->
                when (result.result) {
                    CallResult.Result.SUCCESS -> Result.success("Volume set to $volumePercent%")
                    else -> Result.failure(Exception("Volume control failed"))
                }
            } ?: Result.failure(Exception("Spotify not connected"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search for content using Spotify App Remote
     */
    suspend fun search(query: String, type: String = "track"): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.search(query, type)?.let { result ->
                when (result.result) {
                    CallResult.Result.SUCCESS -> {
                        val tracks = result.items.map { item ->
                            // Convert Spotify SDK Track to our Track model
                            Track(
                                id = item.uri.split(":").last(),
                                name = item.name,
                                artist = item.artist.name,
                                album = item.album.name,
                                durationMs = item.duration,
                                imageUrl = item.imageUri?.raw,
                                uri = item.uri,
                                popularity = 0, // Not available in search results
                                explicit = false // Not available in search results
                            )
                        }
                        Result.success(tracks)
                    }
                    else -> Result.failure(Exception("Search failed"))
                }
            } ?: Result.failure(Exception("Spotify not connected"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queue a track for playback
     */
    suspend fun queueTrack(trackUri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.queue(trackUri)?.let { result ->
                when (result.result) {
                    CallResult.Result.SUCCESS -> Result.success("Track queued")
                    else -> Result.failure(Exception("Queue failed"))
                }
            } ?: Result.failure(Exception("Spotify not connected"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Play a specific track, album, or playlist
     */
    suspend fun playContent(uri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            spotifyAppRemote?.playerApi?.play(uri)?.let { result ->
                when (result.result) {
                    CallResult.Result.SUCCESS -> Result.success("Playing content")
                    else -> Result.failure(Exception("Play failed"))
                }
            } ?: Result.failure(Exception("Spotify not connected"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect from Spotify App Remote
     */
    fun disconnect() {
        playerStateSubscription?.cancel()
        playerContextSubscription?.cancel()
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null

        _isConnected.value = false
        _connectionStatus.value = "Disconnected"
        _currentTrack.value = null
        _playbackState.value = null
        _lastError.value = null
        updateSpotifySystemStatus()
    }

    /**
     * Check if Spotify app is installed
     */
    fun isSpotifyInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.spotify.music", 0) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        disconnect()
    }

    // Data classes for compatibility with existing code
    data class Track(
        val id: String,
        val name: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val imageUrl: String? = null,
        val previewUrl: String? = null,
        val uri: String,
        val popularity: Int = 0,
        val explicit: Boolean = false
    )

    data class PlaybackState(
        val isPlaying: Boolean,
        val progressMs: Long,
        val durationMs: Long,
        val shuffleEnabled: Boolean,
        val repeatMode: String,
        val device: Device? = null
    )

    data class Device(
        val id: String,
        val name: String,
        val type: String,
        val isActive: Boolean,
        val volumePercent: Int
    )

    private fun updateSpotifySystemStatus() {
        val isAvailable = isSpotifyInstalled()
        val isConnected = _isConnected.value

        unifiedErrorHandler.updateSystemStatus { status ->
            status.copy(
                spotifyAvailable = isAvailable,
                spotifyConnected = isConnected
            )
        }
    }
}
