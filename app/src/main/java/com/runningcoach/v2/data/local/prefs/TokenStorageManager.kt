package com.runningcoach.v2.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.runningcoach.v2.data.service.SpotifyTokenResponse

interface TokenStorageManager {
    fun saveTokens(tokens: SpotifyTokenResponse)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getExpiresIn(): Long
    fun getTokenType(): String?
    fun clearTokens()
}

class TokenStorageManagerImpl(private val context: Context) : TokenStorageManager {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "spotify_auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveTokens(tokens: SpotifyTokenResponse) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            putLong(KEY_EXPIRES_IN, System.currentTimeMillis() + (tokens.expiresIn * 1000)) // Store expiry as timestamp
            putString(KEY_TOKEN_TYPE, tokens.tokenType)
            apply()
        }
    }

    override fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    override fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    override fun getExpiresIn(): Long {
        return sharedPreferences.getLong(KEY_EXPIRES_IN, 0L)
    }

    override fun getTokenType(): String? {
        return sharedPreferences.getString(KEY_TOKEN_TYPE, null)
    }

    override fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_TOKEN_TYPE = "token_type"
    }
}
