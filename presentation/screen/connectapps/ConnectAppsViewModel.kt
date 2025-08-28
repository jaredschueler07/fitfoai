package com.runningcoach.v2.presentation.screen.connectapps

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.domain.usecase.AuthenticateSpotifyUseCase
import com.runningcoach.v2.data.repository.SpotifyAuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ConnectAppsViewModel(
    private val authenticateSpotifyUseCase: AuthenticateSpotifyUseCase,
    private val spotifyAuthRepository: SpotifyAuthRepository
) : ViewModel() {

    private val _navigateToBrowser = Channel<Pair<String, String>>() // Pair: authUrl, codeVerifier
    val navigateToBrowser = _navigateToBrowser.receiveAsFlow()

    fun onConnectSpotifyClick() {
        viewModelScope.launch {
            try {
                val (authUrl, codeVerifier) = authenticateSpotifyUseCase.execute()
                Log.d("ConnectAppsViewModel", "Spotify Auth URL: $authUrl")
                Log.d("ConnectAppsViewModel", "PKCE Code Verifier: $codeVerifier")
                _navigateToBrowser.send(Pair(authUrl, codeVerifier))
            } catch (e: Exception) {
                Log.e("ConnectAppsViewModel", "Error during Spotify authentication initiation: ${e.message}", e)
                // TODO: Handle error and inform the user via UI state
            }
        }
    }

    fun handleSpotifyAuthRedirect(authorizationCode: String, codeVerifier: String, redirectUri: String) {
        viewModelScope.launch {
            try {
                val tokenResponse = spotifyAuthRepository.exchangeCodeForTokens(
                    authorizationCode = authorizationCode,
                    redirectUri = redirectUri,
                    codeVerifier = codeVerifier
                )
                Log.d("ConnectAppsViewModel", "Spotify token exchange successful! Access Token: ${tokenResponse.accessToken}")
                // TODO: Update UI state to reflect successful connection, navigate away if needed
            } catch (e: Exception) {
                Log.e("ConnectAppsViewModel", "Error exchanging Spotify code for tokens: ${e.message}", e)
                // TODO: Handle error and inform the user
            }
        }
    }
}