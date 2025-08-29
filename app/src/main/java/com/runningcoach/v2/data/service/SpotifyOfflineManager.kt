package com.runningcoach.v2.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Manages offline playback detection and handling for Spotify integration
 *
 * Provides offline-aware functionality including:
 * - Network connectivity monitoring
 * - Offline queue management
 * - Graceful degradation when offline
 * - Resume functionality when back online
 */
class SpotifyOfflineManager(
    private val context: Context,
    private val spotifyService: SpotifySdkService
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Offline state management
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unknown)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    // Offline operation queue
    private val offlineQueue = mutableListOf<OfflineOperation>()
    private val _queuedOperations = MutableStateFlow(0)
    val queuedOperations: StateFlow<Int> = _queuedOperations.asStateFlow()

    enum class NetworkStatus {
        Online, Offline, Unknown
    }

    sealed class OfflineOperation {
        data class PlaybackControl(val action: PlaybackAction) : OfflineOperation()
        data class QueueTrack(val trackUri: String) : OfflineOperation()
        data class Search(val query: String) : OfflineOperation()
        data class PlayContent(val uri: String) : OfflineOperation()
    }

    enum class PlaybackAction {
        PLAY, PAUSE, SKIP_NEXT, SKIP_PREVIOUS, TOGGLE
    }

    init {
        // Start network monitoring
        scope.launch {
            monitorNetworkConnectivity()
        }

        // Start offline queue processor
        scope.launch {
            processOfflineQueue()
        }
    }

    /**
     * Check current network connectivity status
     */
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * Monitor network connectivity changes
     */
    private suspend fun monitorNetworkConnectivity() {
        while (true) {
            try {
                val isOnline = isNetworkAvailable()
                val newStatus = if (isOnline) NetworkStatus.Online else NetworkStatus.Offline

                if (_networkStatus.value != newStatus) {
                    _networkStatus.value = newStatus

                    when (newStatus) {
                        NetworkStatus.Online -> {
                            _isOfflineMode.value = false
                            handleNetworkRestored()
                        }
                        NetworkStatus.Offline -> {
                            _isOfflineMode.value = true
                            handleNetworkLost()
                        }
                        NetworkStatus.Unknown -> {
                            // Keep current offline mode status
                        }
                    }
                }

                delay(5000) // Check every 5 seconds
            } catch (e: Exception) {
                println("Error monitoring network: ${e.message}")
                _networkStatus.value = NetworkStatus.Unknown
                delay(10000) // Wait longer on error
            }
        }
    }

    /**
     * Handle when network connection is restored
     */
    private suspend fun handleNetworkRestored() {
        println("Network restored - processing offline queue")

        // Process queued operations
        processQueuedOperations()

        // Try to reconnect to Spotify if needed
        if (!spotifyService.isConnected.value) {
            // Note: Would need to implement reconnection logic here
            // For now, just log the event
            println("Network restored - Spotify reconnection may be needed")
        }
    }

    /**
     * Handle when network connection is lost
     */
    private suspend fun handleNetworkLost() {
        println("Network lost - entering offline mode")

        // Notify user about offline mode
        // This would typically trigger a UI notification
        println("Spotify features limited due to offline mode")
    }

    /**
     * Queue operation for offline execution
     */
    private fun queueOperation(operation: OfflineOperation) {
        offlineQueue.add(operation)
        _queuedOperations.value = offlineQueue.size
        println("Queued offline operation: ${operation::class.simpleName}")
    }

    /**
     * Process offline queue when network is restored
     */
    private suspend fun processOfflineQueue() {
        while (true) {
            if (_networkStatus.value == NetworkStatus.Online && offlineQueue.isNotEmpty()) {
                processQueuedOperations()
            }
            delay(10000) // Check every 10 seconds
        }
    }

    /**
     * Process all queued operations
     */
    private suspend fun processQueuedOperations() {
        val operationsToProcess = offlineQueue.toList()
        offlineQueue.clear()
        _queuedOperations.value = 0

        for (operation in operationsToProcess) {
            try {
                when (operation) {
                    is OfflineOperation.PlaybackControl -> {
                        processPlaybackOperation(operation.action)
                    }
                    is OfflineOperation.QueueTrack -> {
                        spotifyService.queueTrack(operation.trackUri)
                    }
                    is OfflineOperation.Search -> {
                        spotifyService.search(operation.query)
                    }
                    is OfflineOperation.PlayContent -> {
                        spotifyService.playContent(operation.uri)
                    }
                }
                println("Processed queued operation: ${operation::class.simpleName}")
            } catch (e: Exception) {
                println("Failed to process queued operation: ${e.message}")
                // Re-queue failed operations
                offlineQueue.add(operation)
                _queuedOperations.value = offlineQueue.size
            }
        }
    }

    /**
     * Process playback operation with offline handling
     */
    private suspend fun processPlaybackOperation(action: PlaybackAction) {
        if (_isOfflineMode.value) {
            // In offline mode, show appropriate message
            println("Playback control queued for when online")
            queueOperation(OfflineOperation.PlaybackControl(action))
            return
        }

        when (action) {
            PlaybackAction.TOGGLE -> spotifyService.togglePlayback()
            PlaybackAction.PLAY -> spotifyService.togglePlayback() // Assuming it's paused
            PlaybackAction.PAUSE -> spotifyService.togglePlayback() // Assuming it's playing
            PlaybackAction.SKIP_NEXT -> spotifyService.skipToNext()
            PlaybackAction.SKIP_PREVIOUS -> spotifyService.skipToPrevious()
        }
    }

    /**
     * Offline-aware playback control methods
     */
    suspend fun togglePlaybackOfflineAware(): Result<String> {
        if (_isOfflineMode.value) {
            queueOperation(OfflineOperation.PlaybackControl(PlaybackAction.TOGGLE))
            return Result.success("Playback command queued for when online")
        }
        return spotifyService.togglePlayback()
    }

    suspend fun skipToNextOfflineAware(): Result<String> {
        if (_isOfflineMode.value) {
            queueOperation(OfflineOperation.PlaybackControl(PlaybackAction.SKIP_NEXT))
            return Result.success("Skip command queued for when online")
        }
        return spotifyService.skipToNext()
    }

    suspend fun skipToPreviousOfflineAware(): Result<String> {
        if (_isOfflineMode.value) {
            queueOperation(OfflineOperation.PlaybackControl(PlaybackAction.SKIP_PREVIOUS))
            return Result.success("Previous command queued for when online")
        }
        return spotifyService.skipToPrevious()
    }

    suspend fun queueTrackOfflineAware(trackUri: String): Result<String> {
        if (_isOfflineMode.value) {
            queueOperation(OfflineOperation.QueueTrack(trackUri))
            return Result.success("Track queued for when online")
        }
        return spotifyService.queueTrack(trackUri)
    }

    suspend fun searchOfflineAware(query: String): Result<List<SpotifySdkService.Track>> {
        if (_isOfflineMode.value) {
            queueOperation(OfflineOperation.Search(query))
            return Result.success(emptyList()) // Return empty list, operation queued
        }
        return spotifyService.search(query)
    }

    suspend fun playContentOfflineAware(uri: String): Result<String> {
        if (_isOfflineMode.value) {
            queueOperation(OfflineOperation.PlayContent(uri))
            return Result.success("Content queued for when online")
        }
        return spotifyService.playContent(uri)
    }

    /**
     * Get offline status information
     */
    fun getOfflineStatus(): OfflineStatus {
        return OfflineStatus(
            isOfflineMode = _isOfflineMode.value,
            networkStatus = _networkStatus.value,
            queuedOperationsCount = _queuedOperations.value,
            isSpotifyConnected = spotifyService.isConnected.value,
            connectionStatus = spotifyService.connectionStatus.value
        )
    }

    /**
     * Clear offline queue
     */
    fun clearOfflineQueue() {
        offlineQueue.clear()
        _queuedOperations.value = 0
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        clearOfflineQueue()
    }

    /**
     * Data class for offline status
     */
    data class OfflineStatus(
        val isOfflineMode: Boolean,
        val networkStatus: NetworkStatus,
        val queuedOperationsCount: Int,
        val isSpotifyConnected: Boolean,
        val connectionStatus: String
    )
}
