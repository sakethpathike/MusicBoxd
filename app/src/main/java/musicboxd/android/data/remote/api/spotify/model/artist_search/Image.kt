package musicboxd.android.data.remote.api.spotify.model.artist_search

import kotlinx.serialization.Serializable

@Serializable
data class Image(
    val height: Int,
    val url: String,
    val width: Int
)