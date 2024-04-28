package musicboxd.android.data.remote.api.lastfm.model.searchAlbums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    @SerialName("#text")
    val imgURL: String,
    val size: String
)