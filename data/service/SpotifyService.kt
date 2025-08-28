package com.runningcoach.v2.data.service

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.util.Base64
import java.net.URLEncoder

class SpotifyService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        // Add other configurations like default request headers if needed
    }

    suspend fun exchangeCodeForTokens(
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String
    ): String {
        // Placeholder for the API call to exchange authorization code for tokens
        // This function will make a POST request to the Spotify token endpoint
        // and return the response (likely a JSON string for now).
        // The actual parsing and handling of tokens will be in the repository.
        return "Placeholder for token exchange response"
    }

    // Placeholder for other Spotify API call functions (e.g., fetching user profile, audio features)

    fun generateAuthorizationUrl(): Pair<String, String> {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // TODO: Replace with secure retrieval of Client ID and Redirect URI
        val clientId = "YOUR_CLIENT_ID"
        val redirectUri = "YOUR_REDIRECT_URI" // This must match the one registered in Spotify Dashboard
        val scopes = "user-top-read playlist-modify-public playlist-modify-private user-modify-playback-state" // Add necessary scopes

        val authUrl = "https://accounts.spotify.com/authorize?" +
                "response_type=code" +
                "&client_id=$clientId" +
                "&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}" +
                "&scope=${URLEncoder.encode(scopes, "UTF-8")}" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256"

        return Pair(authUrl, codeVerifier)
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}