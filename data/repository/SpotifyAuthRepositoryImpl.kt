package com.runningcoach.v2.data.repository

import com.runningcoach.v2.data.service.SpotifyService
import com.runningcoach.v2.data.local.prefs.TokenStorageManager // Assuming this class exists
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import com.runningcoach.v2.data.service.SpotifyTokenResponse // Import the data class from SpotifyService

class SpotifyAuthRepositoryImpl(
    private val spotifyService: SpotifyService,
    private val tokenStorageManager: TokenStorageManager // Assuming this is your secure storage manager
) : SpotifyAuthRepository {

    // Hardcoded for now, move to a more secure location
    private val clientId = "YOUR_SPOTIFY_CLIENT_ID"
    private val redirectUri = "YOUR_REDIRECT_URI"
    private val authEndpoint = "https://accounts.spotify.com/authorize"
    private val tokenEndpoint = "https://accounts.spotify.com/api/token"

    override fun getAuthorizationUrl(scopes: List<String>, redirectUri: String): String {
        val codeVerifier = generateRandomString(128)
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // Save the code verifier for later token exchange
        tokenStorageManager.saveCodeVerifier(codeVerifier)

        val scopeString = scopes.joinToString(" ")

        return "$authEndpoint?" +
                "response_type=code" +
                "&client_id=$clientId" +
                "&scope=$scopeString" +
                "&redirect_uri=$redirectUri" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256"
    }

    override suspend fun exchangeCodeForTokens(
        authorizationCode: String,
        redirectUri: String,
        codeVerifier: String
    ): SpotifyTokens {
        val tokenResponse: SpotifyTokenResponse = spotifyService.exchangeCodeForTokens(
            code = authorizationCode,
            redirectUri = redirectUri,
            clientId = clientId,
            codeVerifier = codeVerifier
        )
        return SpotifyTokens(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresIn = tokenResponse.expiresIn
        )
    }

    override suspend fun refreshToken(refreshToken: String): SpotifyTokens {
        val tokenResponse: SpotifyTokenResponse = spotifyService.refreshToken(
            refreshToken = refreshToken,
            clientId = clientId
        )
        return SpotifyTokens(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken ?: refreshToken, // Use new refresh token if provided, otherwise keep old
            expiresIn = tokenResponse.expiresIn
        )
    }

    override fun saveTokens(tokens: SpotifyTokens) {
        tokenStorageManager.saveTokens(tokens)
    }

    override fun getTokens(): SpotifyTokens? {
        return tokenStorageManager.getTokens()
    }

    override fun clearTokens() {
        tokenStorageManager.clearTokens()
        tokenStorageManager.clearCodeVerifier()
    }
}

// Assuming these data classes exist in data/service/model/
// data class SpotifyTokenResponse(
//     val accessToken: String,
//     val tokenType: String,
//     val expiresIn: Long,
//     val refreshToken: String?,
//     val scope: String
// )

// Assuming this interface exists in data/local/prefs/
// interface TokenStorageManager {
//     fun saveTokens(tokens: SpotifyTokens)
//     fun getTokens(): SpotifyTokens?
//     fun clearTokens()
//     fun saveCodeVerifier(codeVerifier: String)
//     fun getCodeVerifier(): String?
//     fun clearCodeVerifier()
// }