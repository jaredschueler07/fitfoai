package com.runningcoach.v2.data.repository

import com.runningcoach.v2.data.service.SpotifyService
import com.runningcoach.v2.data.local.prefs.TokenStorageManager
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import com.runningcoach.v2.data.service.SpotifyTokenResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SpotifyAuthRepositoryImpl(
    private val spotifyService: SpotifyService,
    private val tokenStorageManager: TokenStorageManager,
    private val clientId: String,
    private val redirectUri: String
) : SpotifyAuthRepository {

    private val authEndpoint = "https://accounts.spotify.com/authorize"

    private var currentCodeVerifier: String? = null
    private val refreshAccessTokenMutex = Mutex()

    override fun getAuthorizationUrl(scopes: List<String>, redirectUri: String): Pair<String, String> {
        val codeVerifier = generateRandomString(128)
        val codeChallenge = generateCodeChallenge(codeVerifier)
        currentCodeVerifier = codeVerifier // Store for later use in token exchange

        val scopeString = scopes.joinToString(" ")

        val url = "$authEndpoint?" +
                "response_type=code" +
                "&client_id=$clientId" +
                "&scope=$scopeString" +
                "&redirect_uri=$redirectUri" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256"
        return Pair(url, codeVerifier)
    }

    override suspend fun exchangeCodeForTokens(
        authorizationCode: String,
        redirectUri: String,
        codeVerifier: String
    ): SpotifyTokenResponse {
        return try {
            val tokenResponse: SpotifyTokenResponse = spotifyService.exchangeCodeForTokens(
                code = authorizationCode,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = codeVerifier
            )
            tokenStorageManager.saveTokens(tokenResponse)
            tokenResponse
        } catch (e: Exception) {
            // Log the error and rethrow or wrap in a custom exception
            throw e
        }
    }

    override suspend fun refreshToken(): SpotifyTokenResponse {
        return refreshAccessTokenMutex.withLock {
            val refreshToken = tokenStorageManager.getRefreshToken()
                ?: throw IllegalStateException("No refresh token available.")

            try {
                val tokenResponse: SpotifyTokenResponse = spotifyService.refreshToken(
                    refreshToken = refreshToken,
                    clientId = clientId
                )
                tokenStorageManager.saveTokens(tokenResponse)
                tokenResponse
            } catch (e: Exception) {
                // Log the error, clear tokens if refresh fails to prevent infinite loops, and rethrow
                tokenStorageManager.clearTokens()
                throw e
            }
        }
    }

    override suspend fun getValidAccessToken(): String? {
        val accessToken = tokenStorageManager.getAccessToken()
        val expiresIn = tokenStorageManager.getExpiresIn()

        if (accessToken == null || expiresIn == 0L) {
            return null // No token stored
        }

        val currentTime = System.currentTimeMillis()
        // Refresh token if it expires within the next 60 seconds (or is already expired)
        if (expiresIn < currentTime + (60 * 1000)) {
            return try {
                refreshToken().accessToken
            } catch (e: Exception) {
                // Handle refresh failure, e.g., re-authenticate
                null
            }
        }
        return accessToken
    }

    override fun clearTokens() {
        tokenStorageManager.clearTokens()
        currentCodeVerifier = null // Clear the stored code verifier
    }

    // Utility functions for PKCE
    private fun generateRandomString(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val digest = sha256.digest(codeVerifier.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
