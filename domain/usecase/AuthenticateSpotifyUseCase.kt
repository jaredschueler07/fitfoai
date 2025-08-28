package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.data.repository.SpotifyAuthRepository
import javax.inject.Inject

class AuthenticateSpotifyUseCase @Inject constructor(
    private val spotifyAuthRepository: SpotifyAuthRepository
) {
    private val scopes = listOf("user-top-read", "playlist-modify-public", "playlist-modify-private", "user-modify-playback-state") // Define required scopes
    private val redirectUri = "YOUR_REDIRECT_URI" // Replace with your configured redirect URI

    fun execute(): Pair<String, String> {
        // This function will orchestrate the Spotify authentication flow
        // It will likely call functions in SpotifyAuthRepository
        // For now, it's just a placeholder.
        return spotifyAuthRepository.getAuthorizationUrl(scopes, redirectUri)
    }
}