package com.isaev.musicswipe.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// RecommendationsResponse

@Serializable
data class RecommendationsResponse(
    @SerialName("tracks") val tracks: List<Track>
)

@Serializable
data class Track(
    @SerialName("album") val album: Album,
    @SerialName("artists") val artists: List<ArtistX>,
    @SerialName("available_markets") val availableMarkets: List<String>,
    @SerialName("disc_number") val discNumber: Int,
    @SerialName("duration_ms") val durationMs: Int,
    @SerialName("explicit") val explicit: Boolean,
    @SerialName("external_ids") val externalIds: ExternalIds,
    @SerialName("external_urls") val externalUrls: ExternalUrlsXXX,
    @SerialName("href") val href: String,
    @SerialName("id") val id: String,
    @SerialName("is_local") val isLocal: Boolean,
    @SerialName("name") val name: String,
    @SerialName("popularity") val popularity: Int,
    @SerialName("preview_url") val previewUrl: String?,
    @SerialName("track_number") val trackNumber: Int,
    @SerialName("type") val type: String,
    @SerialName("uri") val uri: String
)

@Serializable
data class Album(
    @SerialName("album_type") val albumType: String,
    @SerialName("artists") val artists: List<Artist>,
    @SerialName("available_markets") val availableMarkets: List<String>,
    @SerialName("external_urls") val externalUrls: ExternalUrlsX,
    @SerialName("href") val href: String,
    @SerialName("id") val id: String,
    @SerialName("images") val images: List<Image>,
    @SerialName("name") val name: String,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("release_date_precision") val releaseDatePrecision: String,
    @SerialName("total_tracks") val totalTracks: Int,
    @SerialName("type") val type: String,
    @SerialName("uri") val uri: String
)

@Serializable
data class ArtistX(
    @SerialName("external_urls") val externalUrls: ExternalUrlsXX,
    @SerialName("href") val href: String,
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("uri") val uri: String
)

@Serializable
data class ExternalIds(
    @SerialName("isrc") val isrc: String
)

@Serializable
data class ExternalUrlsXXX(
    @SerialName("spotify") val spotify: String
)

@Serializable
data class Artist(
    @SerialName("external_urls") val externalUrls: ExternalUrls,
    @SerialName("href") val href: String,
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("uri") val uri: String
)

@Serializable
data class ExternalUrlsX(
    @SerialName("spotify") val spotify: String
)

@Serializable
data class Image(
    @SerialName("height") val height: Int?,
    @SerialName("url") val url: String,
    @SerialName("width") val width: Int?
)

@Serializable
data class ExternalUrls(
    @SerialName("spotify") val spotify: String
)

@Serializable
data class ExternalUrlsXX(
    @SerialName("spotify") val spotify: String
)

// ArtistResponse

@Serializable
data class ArtistResponse(
    @SerialName("external_urls") val externalUrls: ExternalUrls,
    @SerialName("followers") val followers: Followers,
    @SerialName("genres") val genres: List<String>,
    @SerialName("href") val href: String,
    @SerialName("id") val id: String,
    @SerialName("images") val images: List<Image>,
    @SerialName("name") val name: String,
    @SerialName("popularity") val popularity: Int,
    @SerialName("type") val type: String,
    @SerialName("uri") val uri: String
)

@Serializable
data class Followers(
    @SerialName("href") val href: String?,
    @SerialName("total") val total: Int
)

// TopTracksResponse

@Serializable
data class TopTracksResponse(
    @SerialName("items") val items: List<Track>,
    @SerialName("total") val total: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("offset") val offset: Int,
    @SerialName("href") val href: String,
    @SerialName("previous") val previous: String?,
    @SerialName("next") val next: String?
)

// User

@Serializable
data class User(
    @SerialName("country") val country: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("email") val email: String? = null,
    @SerialName("explicit_content") val explicitContent: ExplicitContent,
    @SerialName("external_urls") val externalUrls: ExternalUrls,
    @SerialName("followers") val followers: Followers,
    @SerialName("href") val href: String,
    @SerialName("id") val id: String,
    @SerialName("images") val images: List<Image>,
    @SerialName("product") val product: String,
    @SerialName("type") val type: String,
    @SerialName("uri") val uri: String
)

@Serializable
data class ExplicitContent(
    @SerialName("filter_enabled") val filterEnabled: Boolean,
    @SerialName("filter_locked") val filterLocked: Boolean
)

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String?
)
