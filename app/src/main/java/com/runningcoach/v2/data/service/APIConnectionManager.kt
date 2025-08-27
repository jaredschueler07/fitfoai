package com.runningcoach.v2.data.service

import android.content.Context
import android.content.Intent
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.repository.GoogleFitRepository
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class APIConnectionManager(private val context: Context) {
    
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }
    
    private val _googleFitConnected = MutableStateFlow(false)
    val googleFitConnected: StateFlow<Boolean> = _googleFitConnected.asStateFlow()
    
    private val _spotifyConnected = MutableStateFlow(false)
    val spotifyConnected: StateFlow<Boolean> = _spotifyConnected.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()
    
    // Database and repository setup
    private val database by lazy { FITFOAIDatabase.getDatabase(context) }
    private val googleFitRepository by lazy { GoogleFitRepository(context, database) }
    private val spotifyService by lazy { SpotifyService(context, httpClient) }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun connectGoogleFit(): Intent {
        _connectionStatus.value = "Connecting to Google Fit..."
        return googleFitRepository.connectGoogleFit()
    }
    
    fun connectSpotify(): Intent {
        _connectionStatus.value = "Connecting to Spotify..."
        return spotifyService.initiateConnection()
    }
    
    fun handleGoogleFitActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle the activity result through the repository
        scope.launch {
            try {
                val isConnected = googleFitRepository.isGoogleFitConnected()
                _googleFitConnected.value = isConnected
                
                if (isConnected) {
                    _connectionStatus.value = "Google Fit connected successfully"
                    // Sync today's fitness data
                    googleFitRepository.syncTodaysFitnessData()
                } else {
                    _connectionStatus.value = "Google Fit connection failed"
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Google Fit connection failed: ${e.message}"
            }
        }
    }
    
    suspend fun handleSpotifyCallback(authCode: String): Result<String> {
        val result = spotifyService.handleAuthCallback(authCode)
        if (result.isSuccess) {
            _spotifyConnected.value = true
            _connectionStatus.value = "Spotify connected successfully"
        } else {
            _connectionStatus.value = "Spotify connection failed"
        }
        return result
    }
    
    fun testGoogleFitConnection(): Boolean {
        scope.launch {
            try {
                val isConnected = googleFitRepository.isGoogleFitConnected()
                _googleFitConnected.value = isConnected
                if (isConnected) {
                    // Try to sync data to test the connection
                    googleFitRepository.syncTodaysFitnessData()
                }
            } catch (e: Exception) {
                _googleFitConnected.value = false
            }
        }
        return _googleFitConnected.value
    }
    
    fun testSpotifyConnection(): Boolean {
        val isConnected = spotifyService.testConnection()
        _spotifyConnected.value = isConnected
        return isConnected
    }
    
    fun disconnectGoogleFit() {
        scope.launch {
            googleFitRepository.disconnect()
            _googleFitConnected.value = false
            _connectionStatus.value = "Google Fit disconnected"
        }
    }
    
    fun disconnectSpotify() {
        spotifyService.disconnect()
        _spotifyConnected.value = false
        _connectionStatus.value = "Spotify disconnected"
    }
    
    fun clearStatus() {
        _connectionStatus.value = null
    }
    
    // Get the repositories for other parts of the app to use
    fun provideGoogleFitRepository(): GoogleFitRepository = googleFitRepository
    fun provideSpotifyService(): SpotifyService = spotifyService
    
    fun close() {
        httpClient.close()
    }
}
