package musicboxd.android.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import musicboxd.android.data.remote.api.spotify.model.album.ExternalUrlsX
import musicboxd.android.data.remote.api.spotify.model.tracklist.Artist
import musicboxd.android.data.remote.api.spotify.model.tracklist.Item
import musicboxd.android.ui.common.AlbumxTrackHorizontalPreview
import musicboxd.android.ui.common.ArtistHorizontalPreview
import musicboxd.android.ui.details.DetailsViewModel
import musicboxd.android.ui.details.album.AlbumDetailScreenState
import musicboxd.android.ui.navigation.NavigationRoutes

@OptIn(ExperimentalFoundationApi::class, ExperimentalPagerApi::class)
@Composable
fun SearchContent(
    searchScreenViewModel: SearchScreenViewModel,
    navController: NavController,
    detailsViewModel: DetailsViewModel,
    inSearchScreen: Boolean = true,
    onSelectingAnItem: (String) -> Unit = {}
) {
    val searchArtistsResult =
        searchScreenViewModel.searchArtistsResult.collectAsStateWithLifecycle()
    val searchTracksResult = searchScreenViewModel.searchTracksResult.collectAsStateWithLifecycle()
    val searchAlbumsResult = searchScreenViewModel.searchAlbumsResult.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val tabsList = if (inSearchScreen) {
        listOf("Artists", "Albums", "Tracks")
    } else {
        listOf("Albums", "Tracks")
    }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabsList.forEachIndexed { index, item ->
                Tab(selected = pagerState.currentPage == index, onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(15.dp),
                        color = if (pagerState.currentPage == index) TabRowDefaults.contentColor else MaterialTheme.colorScheme.onSurface.copy(
                            0.70f
                        )
                    )
                }
            }
        }
        HorizontalPager(count = 3, state = pagerState) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when {
                    it == 0 && inSearchScreen -> itemsIndexed(
                        searchArtistsResult.value,
                        key = { index, it ->
                            it.id
                        }) { index, it ->
                        ArtistHorizontalPreview(
                            onClick = {
                                detailsViewModel.artistInfo.value = it
                                detailsViewModel.loadArtistInfo(it.id, it.name)
                                navController.navigate(NavigationRoutes.ARTIST_DETAILS.name)
                            },
                            artistImgUrl = if (it.images.isNotEmpty()) it.images.last().url else "",
                            artistName = it.name
                        )
                    }

                    it == 0 || (inSearchScreen && it == 1) -> itemsIndexed(
                        searchAlbumsResult.value,
                        key = { index, it ->
                            it.id
                        }) { index, it ->
                        AlbumxTrackHorizontalPreview(
                            onClick = {
                                detailsViewModel.albumScreenState = AlbumDetailScreenState(
                                    covertArtImgUrl = flowOf(),
                                    albumImgUrl = it.images.first().url,
                                    albumTitle = it.name,
                                    artists = it.artists,
                                    albumWiki = flowOf(),
                                    releaseDate = it.release_date,
                                    trackList = flowOf(),
                                    itemType = it.album_type.capitalize(),
                                    itemUri = it.uri
                                )
                                if (inSearchScreen) {
                                    detailsViewModel.loadAlbumInfo(
                                        albumID = it.id,
                                        albumName = it.name,
                                        artistID = it.artists.map { it.id }.random(),
                                        artistName = it.artists.first().name,
                                        loadCanvases = inSearchScreen
                                    )
                                    navController.navigate(NavigationRoutes.ALBUM_DETAILS.name)
                                    return@AlbumxTrackHorizontalPreview
                                }
                                onSelectingAnItem("Album")
                            },
                            isExplicit = false,
                            itemType = it.album_type.capitalize(),
                            albumImgUrl = if (it.images.isNotEmpty()) it.images.last().url else "",
                            albumTitle = it.name,
                            artistName = it.artists.joinToString { it.name }
                        )
                    }

                    else -> itemsIndexed(
                        searchTracksResult.value,
                        key = { index, it ->
                            it.id
                        }) { index, it ->
                        AlbumxTrackHorizontalPreview(
                            onClick = {
                                detailsViewModel.albumScreenState = AlbumDetailScreenState(
                                    covertArtImgUrl = flowOf(),
                                    albumImgUrl = it.album.images.first().url,
                                    albumTitle = it.name,
                                    artists = it.artists.map {
                                        musicboxd.android.data.remote.api.spotify.model.album.Artist(
                                            href = it.href,
                                            id = it.id,
                                            name = it.name,
                                            type = it.type,
                                            uri = it.uri,
                                            external_urls = ExternalUrlsX(spotify = it.external_urls.spotify),
                                        )
                                    },
                                    albumWiki = flowOf(),
                                    releaseDate = it.album.release_date,
                                    trackList = flowOf(
                                        listOf(
                                            Item(
                                                artists = it.artists.map {
                                                    Artist(
                                                        id = it.id,
                                                        name = it.name,
                                                        uri = it.uri
                                                    )
                                                },
                                                explicit = false,
                                                id = it.id,
                                                name = it.name,
                                                preview_url = it.preview_url,
                                                track_number = 1,
                                                type = it.type,
                                                uri = it.uri
                                            )
                                        )
                                    ),
                                    itemType = "Track",
                                    itemUri = it.uri
                                )
                                detailsViewModel.loadArtistMetaData(it.artists.first().id)
                                if (inSearchScreen) {
                                    detailsViewModel.loadExternalLinks(isTrack = true, it.id, "")
                                    detailsViewModel.loadCanvases()
                                    navController.navigate(NavigationRoutes.ALBUM_DETAILS.name)
                                    return@AlbumxTrackHorizontalPreview
                                }
                                onSelectingAnItem("Track")
                            },
                            isExplicit = it.explicit,
                            itemType = it.type.capitalize(),
                            albumImgUrl = if (it.album.images.isNotEmpty()) it.album.images.last().url else "",
                            albumTitle = it.name,
                            artistName = it.artists.joinToString { it.name }
                        )
                    }
                }
            }
        }
    }
}