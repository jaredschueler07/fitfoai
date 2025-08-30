package com.runningcoach.v2.data.repository

import com.runningcoach.v2.data.local.prefs.TokenStorageManager
import com.runningcoach.v2.data.service.SpotifyService
import com.runningcoach.v2.data.service.SpotifyTokenResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SpotifyAuthRepositoryImplTest {

    private lateinit var spotifyService: SpotifyService
    private lateinit var tokenStorageManager: TokenStorageManager
    private lateinit var spotifyAuthRepository: SpotifyAuthRepositoryImpl

    private val clientId = "test_client_id"
    private val redirectUri = "test_redirect_uri"
    private val authCode = "test_auth_code"
    private val codeVerifier = "test_code_verifier"

    @Before
    fun setup() {
        spotifyService = mockk()
        tokenStorageManager = mockk(relaxUnitFun = true)
        spotifyAuthRepository = SpotifyAuthRepositoryImpl(spotifyService, tokenStorageManager, clientId, redirectUri)

        // Mock common behavior for tokenStorageManager
        every { tokenStorageManager.getAccessToken() } returns "mock_access_token"
        every { tokenStorageManager.getRefreshToken() } returns "mock_refresh_token"
        every { tokenStorageManager.getExpiresIn() } returns System.currentTimeMillis() + (3600 * 1000) // 1 hour from now
        every { tokenStorageManager.getTokenType() } returns "Bearer"
    }

    @Test
    fun `getAuthorizationUrl generates correct URL and code verifier`() {
        val scopes = listOf("user-read-private", "user-read-email")
        val (url, verifier) = spotifyAuthRepository.getAuthorizationUrl(scopes, redirectUri)

        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=$clientId"))
        assertTrue(url.contains("scope=user-read-private%20user-read-email"))
        assertTrue(url.contains("redirect_uri=$redirectUri"))
        assertTrue(url.contains("code_challenge="))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertFalse(verifier.isBlank())
    }

    @Test
    fun `exchangeCodeForTokens calls service and saves tokens on success`() = runBlocking {
        val mockTokenResponse = SpotifyTokenResponse(
            accessToken = "new_access",
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "new_refresh",
            scope = ""
        )

        coEvery { spotifyService.exchangeCodeForTokens(any(), any(), any(), any()) } returns mockTokenResponse

        val result = spotifyAuthRepository.exchangeCodeForTokens(authCode, redirectUri, codeVerifier)

        assertEquals(mockTokenResponse, result)
        coVerify(exactly = 1) { spotifyService.exchangeCodeForTokens(authCode, redirectUri, clientId, codeVerifier) }
        verify(exactly = 1) { tokenStorageManager.saveTokens(mockTokenResponse) }
    }

    @Test(expected = Exception::class)
    fun `exchangeCodeForTokens rethrows exception on service error`() = runBlocking {
        coEvery { spotifyService.exchangeCodeForTokens(any(), any(), any(), any()) } throws Exception("API Error")

        spotifyAuthRepository.exchangeCodeForTokens(authCode, redirectUri, codeVerifier)

        coVerify(exactly = 1) { spotifyService.exchangeCodeForTokens(authCode, redirectUri, clientId, codeVerifier) }
        verify(exactly = 0) { tokenStorageManager.saveTokens(any()) }
    }

    @Test
    fun `refreshToken calls service and saves new tokens on success`() = runBlocking {
        val oldRefreshToken = "mock_refresh_token"
        val mockNewTokenResponse = SpotifyTokenResponse(
            accessToken = "refreshed_access",
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "new_refreshed_token",
            scope = ""
        )

        every { tokenStorageManager.getRefreshToken() } returns oldRefreshToken
        coEvery { spotifyService.refreshToken(any(), any()) } returns mockNewTokenResponse

        val result = spotifyAuthRepository.refreshToken()

        assertEquals(mockNewTokenResponse, result)
        coVerify(exactly = 1) { spotifyService.refreshToken(oldRefreshToken, clientId) }
        verify(exactly = 1) { tokenStorageManager.saveTokens(mockNewTokenResponse) }
    }

    @Test(expected = IllegalStateException::class)
    fun `refreshToken throws exception if no refresh token is available`() = runBlocking {
        every { tokenStorageManager.getRefreshToken() } returns null

        spotifyAuthRepository.refreshToken()

        coVerify(exactly = 0) { spotifyService.refreshToken(any(), any()) }
        verify(exactly = 0) { tokenStorageManager.saveTokens(any()) }
    }

    @Test(expected = Exception::class)
    fun `refreshToken clears tokens and rethrows exception on service error`() = runBlocking {
        val oldRefreshToken = "mock_refresh_token"
        every { tokenStorageManager.getRefreshToken() } returns oldRefreshToken
        coEvery { spotifyService.refreshToken(any(), any()) } throws Exception("Refresh API Error")

        spotifyAuthRepository.refreshToken()

        coVerify(exactly = 1) { spotifyService.refreshToken(oldRefreshToken, clientId) }
        verify(exactly = 1) { tokenStorageManager.clearTokens() }
        verify(exactly = 0) { tokenStorageManager.saveTokens(any()) }
    }

    @Test
    fun `getValidAccessToken returns existing token if not expired`() = runBlocking {
        val accessToken = "valid_access_token"
        val expiresIn = System.currentTimeMillis() + (3600 * 1000) // 1 hour from now

        every { tokenStorageManager.getAccessToken() } returns accessToken
        every { tokenStorageManager.getExpiresIn() } returns expiresIn

        val result = spotifyAuthRepository.getValidAccessToken()

        assertEquals(accessToken, result)
        coVerify(exactly = 0) { spotifyService.refreshToken(any(), any()) }
    }

    @Test
    fun `getValidAccessToken refreshes token if expired`() = runBlocking {
        val expiredAccessToken = "expired_access_token"
        val expiredIn = System.currentTimeMillis() - 1000 // 1 second ago
        val newAccessToken = "new_valid_access_token"

        every { tokenStorageManager.getAccessToken() } returns expiredAccessToken
        every { tokenStorageManager.getExpiresIn() } returns expiredIn
        every { tokenStorageManager.getRefreshToken() } returns "mock_refresh_token"
        coEvery { spotifyService.refreshToken(any(), any()) } returns SpotifyTokenResponse(
            accessToken = newAccessToken,
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "new_refreshed_token",
            scope = ""
        )

        val result = spotifyAuthRepository.getValidAccessToken()

        assertEquals(newAccessToken, result)
        coVerify(exactly = 1) { spotifyService.refreshToken(any(), any()) }
        verify(exactly = 1) { tokenStorageManager.saveTokens(any()) }
    }

    @Test
    fun `getValidAccessToken returns null if no token is stored`() = runBlocking {
        every { tokenStorageManager.getAccessToken() } returns null
        every { tokenStorageManager.getExpiresIn() } returns 0L

        val result = spotifyAuthRepository.getValidAccessToken()

        assertNull(result)
        coVerify(exactly = 0) { spotifyService.refreshToken(any(), any()) }
    }

    @Test
    fun `getValidAccessToken returns null if refresh fails`() = runBlocking {
        val expiredAccessToken = "expired_access_token"
        val expiredIn = System.currentTimeMillis() - 1000 // 1 second ago

        every { tokenStorageManager.getAccessToken() } returns expiredAccessToken
        every { tokenStorageManager.getExpiresIn() } returns expiredIn
        every { tokenStorageManager.getRefreshToken() } returns "mock_refresh_token"
        coEvery { spotifyService.refreshToken(any(), any()) } throws Exception("Refresh failed")

        val result = spotifyAuthRepository.getValidAccessToken()

        assertNull(result)
        coVerify(exactly = 1) { spotifyService.refreshToken(any(), any()) }
        verify(exactly = 1) { tokenStorageManager.clearTokens() }
    }

    @Test
    fun `clearTokens clears storage`() {
        spotifyAuthRepository.clearTokens()
        verify(exactly = 1) { tokenStorageManager.clearTokens() }
    }
}