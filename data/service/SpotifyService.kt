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

    private val tokenEndpoint = "https://accounts.spotify.com/api/token"
    private val apiBaseUrl = "https://api.spotify.com/v1"

    suspend fun exchangeCodeForTokens(
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String
    ): SpotifyTokenResponse {
        val response: HttpResponse = client.post(tokenEndpoint) {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(FormDataContent(parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", clientId)
                append("code_verifier", codeVerifier)
            }))
        }
        return response.body()
    }

    suspend fun refreshToken(
        refreshToken: String,
        clientId: String
    ): SpotifyTokenResponse {
        val response: HttpResponse = client.post(tokenEndpoint) {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(FormDataContent(parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", clientId)
            }))
        }
        return response.body()
    }

    suspend fun getUserProfile(accessToken: String): UserProfileData {
        val response: HttpResponse = client.get("$apiBaseUrl/me") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return response.body()
    }

    suspend fun getUserTopItems(accessToken: String, type: String = "artists", limit: Int = 20, offset: Int = 0): TopItemsResponse<Artist> { // Default to artists
        val response: HttpResponse = client.get("$apiBaseUrl/me/top/$type") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            url {
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
            }
        }
        return response.body()
    }

    suspend fun getTrackAudioFeatures(accessToken: String, trackIds: List<String>): AudioFeaturesResponse {
        val response: HttpResponse = client.get("$apiBaseUrl/audio-features") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            url {
                parameters.append("ids", trackIds.joinToString(","))
            }
        }
        return response.body()
    }
}