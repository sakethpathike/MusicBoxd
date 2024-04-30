package musicboxd.android.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import musicboxd.android.data.remote.api.lastfm.LastFMAPIRepo
import musicboxd.android.data.remote.api.musicbrainz.MusicBrainzAPIRepo
import musicboxd.android.data.remote.api.spotify.SpotifyAPIRepo
import musicboxd.android.data.remote.api.spotify.model.artist.Item
import musicboxd.android.data.remote.api.spotify.model.token.SpotifyToken
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val lastFMAPIRepo: LastFMAPIRepo,
    private val musicBrainzAPIRepo: MusicBrainzAPIRepo,
    private val spotifyAPIRepo: SpotifyAPIRepo
) :
    ViewModel() {
    private val _searchArtistsResult = MutableStateFlow(emptyList<Item>())
    val searchArtistsResult = _searchArtistsResult.asStateFlow()

    private var spotifyToken: SpotifyToken? = SpotifyToken(
        accessToken = "",
        accessTokenExpirationTimestampMs = 0L,
        clientId = "",
        isAnonymous = false
    )

    init {
        viewModelScope.launch {
            spotifyToken = withContext(Dispatchers.Default) {
                try {
                    OkHttpClient().newCall(
                        Request.Builder().get()
                            .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                            .build()
                    ).execute().body?.string()
                } catch (_: Exception) {
                    ""
                }?.let {
                    try {
                        Json.decodeFromString<SpotifyToken>(it)
                    } catch (_: Exception) {
                        SpotifyToken(
                            accessToken = "",
                            accessTokenExpirationTimestampMs = 0,
                            clientId = "",
                            isAnonymous = false
                        )
                    }
                }
            }
            Log.d("10MinMail", spotifyToken.toString())
        }
    }

    private val _searchTracksResult = MutableStateFlow(
        emptyList<musicboxd.android.data.remote.api.spotify.model.track.Item>()
    )
    val searchTracksResult = _searchTracksResult.asStateFlow()

    private val _searchAlbumsResult =
        MutableStateFlow(emptyList<musicboxd.android.data.remote.api.spotify.model.album.Item>())
    val searchAlbumsResult = _searchAlbumsResult.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    fun onUiEvent(searchScreenUiEvent: SearchScreenUiEvent) {
        when (searchScreenUiEvent) {
            is SearchScreenUiEvent.OnQueryChange -> {
                viewModelScope.launch {
                    _searchQuery.emit(searchScreenUiEvent.query)
                    _searchQuery.debounce(150L).collectLatest { query ->
                        if (query.isNotEmpty() && spotifyToken != null) {
                            awaitAll(async {
                                _searchTracksResult.emit(
                                    spotifyAPIRepo.searchTracks(
                                        query,
                                        "10",
                                        spotifyToken!!.accessToken
                                    ).tracks.items
                                )
                            }, async {
                                _searchAlbumsResult.emit(
                                    spotifyAPIRepo.searchAlbums(
                                        query,
                                        "10",
                                        spotifyToken!!.accessToken
                                    ).albums.items
                                )
                            }, async {
                                _searchArtistsResult.emit(
                                    spotifyAPIRepo.searchArtists(
                                        query,
                                        "10",
                                        spotifyToken!!.accessToken
                                    ).artists.items.sortedByDescending {
                                        it.popularity
                                    }
                                )
                            })
                        }
                    }
                }
            }
        }
    }
}