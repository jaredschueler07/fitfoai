package com.runningcoach.v2.presentation.screen.connectapps

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.domain.usecase.AuthenticateSpotifyUseCase
import kotlinx.coroutines.launch

class ConnectAppsViewModel(
    private val authenticateSpotifyUseCase: AuthenticateSpotifyUseCase
) : ViewModel() {

    fun onConnectSpotifyClick() {
        viewModelScope.launch {
            try {
                val (authUrl, codeVerifier) = authenticateSpotifyUseCase.execute()
                Log.d("ConnectAppsViewModel", "Spotify Auth URL: $authUrl")
                Log.d("ConnectAppsViewModel", "PKCE Code Verifier: $codeVerifier")
                // TODO: Handle opening the URL in a browser/custom tab
            } catch (e: Exception) {
                Log.e("ConnectAppsViewModel", "Error during Spotify authentication initiation: ${e.message}", e)
                // TODO: Handle error and inform the user
            }
        }
    }
}