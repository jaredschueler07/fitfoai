package com.runningcoach.v2.data.repository

import com.runningcoach.v2.data.service.SpotifyTokenResponse

interface SpotifyAuthRepository {
    /**
     * Initiates the Spotify authorization flow by generating the authorization URL and a code verifier for PKCE.
     * @param scopes A list of required Spotify scopes.
     * @param redirectUri The redirect URI configured in the Spotify Developer Dashboard.
     * @return A Pair containing the authorization URL (String) and the generated PKCE code verifier (String).
     */
    fun getAuthorizationUrl(scopes: List<String>, redirectUri: String): Pair<String, String>

    /**
     * Exchanges the authorization code received from Spotify for access and refresh tokens.
     * @param authorizationCode The authorization code received after user consent.
     * @param redirectUri The redirect URI used in the authorization request.
     * @param codeVerifier The PKCE code verifier string used in the authorization request.
     * @return A data class containing the access token, refresh token, and expiry time.
     * @throws Exception if the token exchange fails.
     */
    suspend fun exchangeCodeForTokens(
        authorizationCode: String,
        redirectUri: String,
        codeVerifier: String
    ): SpotifyTokenResponse

    /**
     * Refreshes an expired access token using the refresh token.
     * @return A data class containing the new access token and expiry time.
     * @throws Exception if the token refresh fails.
     */
    suspend fun refreshToken(): SpotifyTokenResponse

    /**
     * Retrieves a valid access token. If the current token is expired, it attempts to refresh it.
     * @return A valid access token, or null if no valid token can be obtained.
     */
    suspend fun getValidAccessToken(): String?

    /**
     * Clears the securely stored Spotify tokens.
     */
    fun clearTokens()
}
