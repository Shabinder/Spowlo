package com.bobbyesp.spowlo.features.spotify_api.model

import com.adamratzman.spotify.models.ReleaseDate

data class SpotifyData(
    val artworkUrl: String = "",
    val name: String = "",
    val artists: List<String> = emptyList(),
    val releaseDate: ReleaseDate? = null,
    val playlistSize : Int? = 0,
    val type: SpotifyDataType = SpotifyDataType.TRACK
)

enum class SpotifyDataType {
    TRACK,
    ALBUM,
    PLAYLIST,
    ARTIST
}