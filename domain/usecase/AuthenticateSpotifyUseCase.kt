package com.runningcoach.v2.domain.usecase

import com.runningcoach.v2.data.repository.SpotifyAuthRepository

class AuthenticateSpotifyUseCase(
    private val spotifyAuthRepository: SpotifyAuthRepository,
    private val redirectUri: String
) {
    private val scopes = listOf("user-top-read", "playlist-modify-public", "playlist-modify-private", "user-modify-playback-state") // Define required scopes

    fun execute(): Pair<String, String> {
        return spotifyAuthRepository.getAuthorizationUrl(scopes, redirectUri)
    }
}