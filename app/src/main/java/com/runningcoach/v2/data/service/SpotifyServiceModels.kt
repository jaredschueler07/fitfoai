package com.runningcoach.v2.data.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null
)

@Serializable
data class UserProfileData(
    @SerialName("display_name") val displayName: String,
    val id: String,
    val uri: String,
    val href: String,
    val external_urls: Map<String, String>,
    val followers: Followers,
    val images: List<SpotifyImage>
)

@Serializable
data class Followers(
    val href: String?,
    val total: Int
)

@Serializable
data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

@Serializable
data class TopItemsResponse<T>(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<T>
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val uri: String,
    val href: String,
    val external_urls: Map<String, String>,
    val genres: List<String>? = null,
    val images: List<SpotifyImage>? = null,
    val followers: Followers? = null,
    val popularity: Int? = null
)

@Serializable
data class Track(
    val id: String,
    val name: String,
    val uri: String,
    val href: String,
    val artists: List<ArtistSimplified>,
    val album: AlbumSimplified,
    val duration_ms: Long,
    val explicit: Boolean,
    val external_urls: Map<String, String>,
    val preview_url: String? = null
)

@Serializable
data class ArtistSimplified(
    val id: String,
    val name: String,
    val uri: String,
    val href: String,
    val external_urls: Map<String, String>
)

@Serializable
data class AlbumSimplified(
    val id: String,
    val name: String,
    val uri: String,
    val href: String,
    val album_type: String,
    val artists: List<ArtistSimplified>,
    val external_urls: Map<String, String>,
    val images: List<SpotifyImage>,
    val release_date: String,
    val release_date_precision: String,
    val total_tracks: Int
)
