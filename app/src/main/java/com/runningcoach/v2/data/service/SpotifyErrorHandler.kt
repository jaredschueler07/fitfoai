package com.runningcoach.v2.data.service

import com.spotify.android.appremote.api.error.*
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Comprehensive error handling for Spotify SDK operations
 *
 * Provides centralized error handling for:
 * - Network connectivity issues
 * - Authentication failures
 * - API rate limiting
 * - Spotify app connectivity problems
 * - User permission issues
 */
class SpotifyErrorHandler {

    sealed class SpotifyError(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause) {

        // Authentication errors
        class AuthenticationError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)
        class AuthorizationError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)
        class TokenExpiredError(message: String = "Access token has expired") : SpotifyError(message)

        // Connection errors
        class ConnectionError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)
        class SpotifyAppNotInstalledError(message: String = "Spotify app is not installed") : SpotifyError(message)
        class RemoteDisconnectedError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)

        // Playback errors
        class PlaybackError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)
        class ContentNotAvailableError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)

        // API errors
        class ApiError(message: String, val statusCode: Int? = null, cause: Throwable? = null) : SpotifyError(message, cause)
        class RateLimitError(message: String, val retryAfter: Long? = null) : SpotifyError(message)
        class NetworkError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)

        // User errors
        class UserCancelledError(message: String = "User cancelled the operation") : SpotifyError(message)
        class PermissionDeniedError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)

        // Generic fallback
        class UnknownError(message: String, cause: Throwable? = null) : SpotifyError(message, cause)
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            handleCoroutineError(throwable)
        }

        /**
         * Handle Spotify SDK connection errors
         */
        fun handleConnectionError(error: Throwable): SpotifyError {
            return when (error) {
                is SpotifyConnectionTerminatedException -> {
                    SpotifyError.RemoteDisconnectedError(
                        "Spotify connection terminated. User may have closed the app.",
                        error
                    )
                }
                is SpotifyDisconnectedException -> {
                    SpotifyError.ConnectionError(
                        "Lost connection to Spotify. Check network and try reconnecting.",
                        error
                    )
                }
                is CouldNotFindSpotifyApp -> {
                    SpotifyError.SpotifyAppNotInstalledError(
                        "Spotify app is not installed. Please install Spotify to use this feature."
                    )
                }
                else -> {
                    SpotifyError.ConnectionError(
                        "Failed to connect to Spotify: ${error.message}",
                        error
                    )
                }
            }
        }

        /**
         * Handle Spotify Auth SDK errors
         */
        fun handleAuthError(response: AuthorizationResponse): SpotifyError {
            return when (response.type) {
                AuthorizationResponse.Type.ERROR -> {
                    when (response.error) {
                        "access_denied" -> SpotifyError.UserCancelledError("User denied access to Spotify")
                        "invalid_scope" -> SpotifyError.PermissionDeniedError("Invalid permissions requested")
                        "invalid_request" -> SpotifyError.AuthorizationError("Invalid authorization request")
                        else -> SpotifyError.AuthorizationError("Authorization failed: ${response.error}")
                    }
                }
                AuthorizationResponse.Type.EMPTY -> {
                    SpotifyError.UserCancelledError("Authorization was cancelled")
                }
                else -> {
                    SpotifyError.UnknownError("Unexpected authorization response")
                }
            }
        }

        /**
         * Handle API call result errors
         */
        fun handleApiResultError(operation: String, result: com.spotify.protocol.types.ErrorDetails?): SpotifyError {
            return result?.let { error ->
                when (error.errorCode) {
                    "RESTRICTED_CONTENT" -> SpotifyError.ContentNotAvailableError("$operation failed: Content not available in your region")
                    "PREMIUM_REQUIRED" -> SpotifyError.PermissionDeniedError("$operation failed: Spotify Premium required")
                    "RATE_LIMITED" -> SpotifyError.RateLimitError("$operation failed: Too many requests", error.retryAfterMs?.toLong())
                    "NETWORK_ERROR" -> SpotifyError.NetworkError("$operation failed: Network connection error")
                    else -> SpotifyError.ApiError("$operation failed: ${error.message}", null, Exception(error.message))
                }
            } ?: SpotifyError.UnknownError("$operation failed with unknown error")
        }

        /**
         * Handle generic exceptions and convert to SpotifyError
         */
        fun handleGenericError(operation: String, error: Throwable): SpotifyError {
            return when (error) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException -> {
                    SpotifyError.NetworkError("$operation failed: Network connection error", error)
                }
                is java.io.IOException -> {
                    SpotifyError.NetworkError("$operation failed: I/O error", error)
                }
                is IllegalArgumentException -> {
                    SpotifyError.ApiError("$operation failed: Invalid parameters", null, error)
                }
                is SecurityException -> {
                    SpotifyError.PermissionDeniedError("$operation failed: Permission denied", error)
                }
                else -> {
                    SpotifyError.UnknownError("$operation failed: ${error.message}", error)
                }
            }
        }

        /**
         * Handle coroutine errors
         */
        private fun handleCoroutineError(error: Throwable) {
            when (val spotifyError = handleGenericError("Coroutine operation", error)) {
                is SpotifyError.NetworkError -> {
                    // Log network error but don't crash
                    println("Spotify network error: ${spotifyError.message}")
                }
                is SpotifyError.ConnectionError -> {
                    // Log connection error
                    println("Spotify connection error: ${spotifyError.message}")
                }
                else -> {
                    // Log other errors
                    println("Spotify error: ${spotifyError.message}")
                }
            }
        }

        /**
         * Get user-friendly error message
         */
        fun getUserFriendlyMessage(error: SpotifyError): String {
            return when (error) {
                is SpotifyError.SpotifyAppNotInstalledError -> "Please install the Spotify app to use this feature"
                is SpotifyError.UserCancelledError -> "Operation cancelled"
                is SpotifyError.NetworkError -> "Network connection error. Please check your internet connection"
                is SpotifyError.ConnectionError -> "Unable to connect to Spotify. Please try again"
                is SpotifyError.AuthorizationError -> "Failed to authorize with Spotify. Please try again"
                is SpotifyError.PermissionDeniedError -> "Permission denied. Please check your Spotify account settings"
                is SpotifyError.ContentNotAvailableError -> "This content is not available in your region"
                is SpotifyError.RateLimitError -> "Too many requests. Please wait a moment and try again"
                is SpotifyError.TokenExpiredError -> "Session expired. Please reconnect to Spotify"
                else -> "An error occurred. Please try again"
            }
        }

        /**
         * Check if error is recoverable
         */
        fun isRecoverable(error: SpotifyError): Boolean {
            return when (error) {
                is SpotifyError.NetworkError,
                is SpotifyError.ConnectionError,
                is SpotifyError.RateLimitError,
                is SpotifyError.TokenExpiredError -> true
                else -> false
            }
        }

        /**
         * Get recovery suggestion for error
         */
        fun getRecoverySuggestion(error: SpotifyError): String {
            return when (error) {
                is SpotifyError.NetworkError -> "Check your internet connection and try again"
                is SpotifyError.ConnectionError -> "Reconnect to Spotify by tapping the connect button"
                is SpotifyError.RateLimitError -> "Wait a few minutes before trying again"
                is SpotifyError.TokenExpiredError -> "Tap the connect button to refresh your session"
                is SpotifyError.SpotifyAppNotInstalledError -> "Install the Spotify app from the Play Store"
                is SpotifyError.PermissionDeniedError -> "Check your Spotify app permissions and account settings"
                else -> "Try the operation again"
            }
        }
    }
}
