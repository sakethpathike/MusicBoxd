package musicboxd.android.ui.search.model

import musicboxd.android.data.remote.api.lastfm.model.searchArtists.Artist

data class ArtistSearchResults(
    val artists: List<Artist>,
    val artistImages: List<String>
)
