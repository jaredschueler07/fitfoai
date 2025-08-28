package com.runningcoach.v2.data.repository

interface SpotifyAuthRepository {
    /**
     * Initiates the Spotify authorization flow by generating the authorization URL.
     * @param scopes A list of required Spotify scopes.
     * @param redirectUri The redirect URI configured in the Spotify Developer Dashboard.
     * @return The authorization URL to open in a browser or custom tab.
     */
    fun getAuthorizationUrl(scopes: List<String>, redirectUri: String): String

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
    ): SpotifyTokens

    /**
     * Refreshes an expired access token using the refresh token.
     * @param refreshToken The stored refresh token.
     * @return A data class containing the new access token (and potentially a new refresh token) and expiry time.
     * @throws Exception if the token refresh fails.
     */
    suspend fun refreshToken(refreshToken: String): SpotifyTokens

    /**
     * Securely saves the Spotify tokens.
     * @param tokens The SpotifyTokens data class to save.
     */
    fun saveTokens(tokens: SpotifyTokens)

    /**
     * Retrieves the securely stored Spotify tokens.
     * @return The stored SpotifyTokens data class, or null if no tokens are stored.
     */
    fun getTokens(): SpotifyTokens?

    /**
     * Clears the securely stored Spotify tokens.
     */
    fun clearTokens()
}

data class SpotifyTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // Expiry time in seconds from when it was issued
    val issuedAt: Long = System.currentTimeMillis() // Timestamp when the token was issued
)