package musicboxd.android.ui.details.artist

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import musicboxd.android.R
import musicboxd.android.data.remote.api.spotify.model.album.Albums
import musicboxd.android.data.remote.api.spotify.model.album.Item
import musicboxd.android.data.remote.api.spotify.model.topTracks.TopTracksDTO
import musicboxd.android.data.remote.api.spotify.model.tracklist.Artist
import musicboxd.android.ui.common.AlbumxTrackHorizontalPreview
import musicboxd.android.ui.common.ArtistCoverArt
import musicboxd.android.ui.common.ArtistCoverArtState
import musicboxd.android.ui.common.CoilImage
import musicboxd.android.ui.common.HorizontalTrackPreview
import musicboxd.android.ui.common.fadedEdges
import musicboxd.android.ui.details.DetailsViewModel
import musicboxd.android.ui.details.album.AlbumDetailScreenState
import musicboxd.android.ui.navigation.NavigationRoutes

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(detailsViewModel: DetailsViewModel, navController: NavController) {
    val topTracks = detailsViewModel.topTracksDTO.collectAsStateWithLifecycle(
        initialValue = TopTracksDTO(
            emptyList()
        )
    )
    val albums = detailsViewModel.artistAlbums.collectAsStateWithLifecycle(
        initialValue = Albums(
            items = listOf(),
            limit = 0,
            offset = 0,
            total = 0
        )
    ).value.items
    val specificArtistFromSpotifyDTO = detailsViewModel.artistInfo
    LaunchedEffect(key1 = albums.size) {
        if (albums.isNotEmpty()) {
            detailsViewModel.loadTrackListOfAnAlbum(albums.first().id)
        }
    }
    val latestReleases = detailsViewModel.albumScreenState.trackList.collectAsStateWithLifecycle(
        initialValue = emptyList()
    ).value
    val isGenresExpanded = rememberSaveable {
        mutableStateOf(false)
    }
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
        }
    }
    val colorScheme = MaterialTheme.colorScheme
    val isAnyTrackIsPlayingState = rememberSaveable {
        mutableStateOf(false)
    }
    val isAnyTrackInLoadingState = rememberSaveable {
        mutableStateOf(false)
    }
    val selectedTrackId = rememberSaveable {
        mutableStateOf("")
    }
    val currentPlayingTrackDurationAsFloat = rememberSaveable {
        mutableFloatStateOf(0f)
    }
    val artistBio = detailsViewModel.artistBio.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val lastFmImage = detailsViewModel.lastFMImage.collectAsStateWithLifecycle()
    val localUriHandler = LocalUriHandler.current
    val bottomModalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDiscographyBtmSheetEnabled = rememberSaveable {
        mutableStateOf(false)
    }
    val isBtmSheetVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val currentTrackID = rememberSaveable {
        mutableStateOf("")
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = isAnyTrackIsPlayingState.value) {
        while (isAnyTrackIsPlayingState.value) {
            currentPlayingTrackDurationAsFloat.floatValue =
                mediaPlayer.currentPosition.toFloat() / 30000f
            delay(500L)
        }
    }
    fun onDiscographyItemClick(it: Item) {
        detailsViewModel.albumScreenState = AlbumDetailScreenState(
            covertArtImgUrl = flowOf(specificArtistFromSpotifyDTO.value.images.first().url),
            albumImgUrl = it.images.first().url,
            albumTitle = it.name,
            artists = it.artists,
            albumWiki = flowOf(),
            releaseDate = it.release_date,
            trackList = flowOf(),
            itemType = it.album_type.capitalize()
        )
        detailsViewModel.loadAlbumInfo(
            albumID = it.id,
            albumName = it.name,
            artistID = it.artists
                .map { it.id }
                .random(),
            artistName = it.artists.first().name,
            loadArtistImg = false
        )
        navController.navigate(NavigationRoutes.ALBUM_DETAILS.name)
    }

    fun onPlayClick(it: musicboxd.android.data.remote.api.spotify.model.tracklist.Item) {
        if (it.id == selectedTrackId.value && (isAnyTrackIsPlayingState.value || isAnyTrackInLoadingState.value)) {
            isAnyTrackInLoadingState.value = false
            isAnyTrackIsPlayingState.value = false
            mediaPlayer.stop()
            mediaPlayer.reset()
            return
        }
        Toast
            .makeText(localContext, "Fetching Audio", Toast.LENGTH_SHORT)
            .show()
        selectedTrackId.value = it.id
        if (latestReleases.isNotEmpty() && latestReleases.first().id == it.id) {
            currentTrackID.value = it.id
        }
        isAnyTrackInLoadingState.value = true
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.setDataSource(it.preview_url)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            it.start()
            isAnyTrackInLoadingState.value = false
            isAnyTrackIsPlayingState.value = true
        }
        mediaPlayer.setOnCompletionListener {
            it.stop()
            it.reset()
            isAnyTrackIsPlayingState.value = false
            isAnyTrackInLoadingState.value = false
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ArtistCoverArt(
                artistCoverArtState =
                ArtistCoverArtState(
                    artistName = specificArtistFromSpotifyDTO.value.name,
                    artistImgUrl = if (specificArtistFromSpotifyDTO.value.images.isNotEmpty()) specificArtistFromSpotifyDTO.value.images.first().url else ""
                )
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(10.dp))
                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    painter = painterResource(id = R.drawable.spotify_logo),
                    contentDescription = ""
                )
                Text(
                    text = "•  " + specificArtistFromSpotifyDTO.value.followers.total.toString()
                        .plus(" Followers"),
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            FilledTonalButton(
                onClick = { },
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Follow",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        if (specificArtistFromSpotifyDTO.value.genres.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isGenresExpanded.value = !isGenresExpanded.value
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalContentColor.current,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                    IconButton(onClick = {
                        isGenresExpanded.value = !isGenresExpanded.value
                    }) {
                        Icon(
                            imageVector = if (!isGenresExpanded.value) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = null
                        )
                    }
                }
                Box(modifier = Modifier.animateContentSize()) {
                    if (isGenresExpanded.value) {
                        FlowRow(
                            modifier = Modifier
                                .padding(start = 10.dp, bottom = 10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            specificArtistFromSpotifyDTO.value.genres.forEach {
                                AssistChip(onClick = {
                                }, label = {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = LocalContentColor.current,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }
        item {
            Divider(
                modifier = Modifier.padding(start = 15.dp, bottom = 15.dp, end = 15.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.25f)
            )
        }
        item {
            Text(
                text = "Latest Release",
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        if (albums.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .clickable {
                            onDiscographyItemClick(albums.first())
                        }
                        .fillMaxWidth()
                        .padding(start = 10.dp, bottom = 15.dp, end = 15.dp, top = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoilImage(
                        imgUrl = albums[0].images.first().url,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        contentDescription = ""
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Column(modifier = Modifier.fillMaxWidth(if (albums.first().album_type.contains("single")) 0.75f else 1f)) {
                        Text(
                            text = albums[0].name,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = albums[0].album_type.capitalize(),
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalContentColor.current.copy(0.85f)
                        )
                        if (albums[0].artists.joinToString { it.name } != specificArtistFromSpotifyDTO.value.name) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                color = LocalContentColor.current.copy(0.85f),
                                text = albums[0].artists.joinToString { it.name },
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                    if (latestReleases.isNotEmpty() && albums.first().album_type.contains("single")) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedTrackId.value == currentTrackID.value && (isAnyTrackIsPlayingState.value || isAnyTrackInLoadingState.value)) {
                                if (isAnyTrackIsPlayingState.value) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        strokeWidth = 2.5.dp,
                                        progress = currentPlayingTrackDurationAsFloat.floatValue
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }
                            IconButton(onClick = {
                                onPlayClick(latestReleases.first())
                            }) {
                                Icon(
                                    imageVector = if (isAnyTrackInLoadingState.value && selectedTrackId.value == currentTrackID.value) Icons.Default.Audiotrack else if (isAnyTrackIsPlayingState.value && selectedTrackId.value == currentTrackID.value) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
                Text(
                    modifier = Modifier.padding(start = 10.dp, bottom = 15.dp),
                    color = LocalContentColor.current.copy(0.85f),
                    text = "Released on " + albums[0].release_date,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        item {
            Divider(
                modifier = Modifier.padding(start = 15.dp, bottom = 15.dp, end = 15.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.25f)
            )
        }
        item {
            Text(
                text = "Top Tracks",
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
                modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
            )
        }
        items(topTracks.value.tracks, key = {
            it.id
        }) {
            HorizontalTrackPreview(
                trackNumber = it.track_number.toString(),
                trackName = it.name,
                isExplicit = it.explicit,
                artists = it.artists.map { it.name },
                currentTrackID = it.id,
                selectedTrackId = selectedTrackId.value,
                isAnyTrackIsPlayingState = isAnyTrackIsPlayingState,
                isAnyTrackInLoadingState = isAnyTrackInLoadingState,
                onPlayClick = {
                    onPlayClick(
                        musicboxd.android.data.remote.api.spotify.model.tracklist.Item(
                            artists = it.artists.map {
                                Artist(
                                    href = it.href,
                                    id = it.id,
                                    name = it.name,
                                    type = it.type,
                                    uri = it.uri
                                )
                            },
                            explicit = it.explicit,
                            id = it.id,
                            name = it.name,
                            preview_url = it.preview_url,
                            track_number = it.track_number,
                            type = it.type,
                            uri = it.uri
                        )
                    )
                },
                currentPlayingTrackDurationAsFloat = currentPlayingTrackDurationAsFloat,
                trackImgUrl = it.album.images.first().url
            )
        }
        /*  item {
              Divider(
                  modifier = Modifier.padding(start = 15.dp, top = 10.dp, end = 15.dp),
                  color = MaterialTheme.colorScheme.outline.copy(0.25f)
              )
          }*/
        item {
            Text(
                text = "Discography",
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
                modifier = Modifier.padding(10.dp)
            )
        }
        item {
            Spacer(modifier = Modifier.height(5.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Spacer(modifier = Modifier.width(0.dp))
                }
                items(albums, key = {
                    it.id
                }) {
                    Column(
                        Modifier
                            .width(200.dp)
                            .height(275.dp)
                            .clickable(indication = null, interactionSource = remember {
                                MutableInteractionSource()
                            }, onClick = {
                                onDiscographyItemClick(it)
                            })
                    ) {
                        CoilImage(
                            imgUrl = it.images.first().url,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            contentDescription = "${it.name} cover art"
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalContentColor.current,
                            fontSize = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = it.album_type.capitalize(),
                            style = MaterialTheme.typography.titleSmall,
                            color = LocalContentColor.current.copy(0.85f),
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(0.dp))
                }
            }
        }
        item {
            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp), onClick = {
                isBtmSheetVisible.value = true
                isDiscographyBtmSheetEnabled.value = true
                coroutineScope.launch {
                    bottomModalSheetState.expand()
                }
            }) {
                Text(
                    text = "See discography",
                    color = LocalContentColor.current,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        /* item {
             Divider(
                 modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 5.dp),
                 color = MaterialTheme.colorScheme.outline.copy(0.25f)
             )
         }*/
        item {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
                modifier = Modifier.padding(10.dp)
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                CoilImage(
                    imgUrl = lastFmImage.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .fadedEdges(colorScheme)
                        .fadedEdges(colorScheme),
                    contentDescription = "${specificArtistFromSpotifyDTO.value.name} Wiki"
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .clickable(interactionSource = remember {
                            MutableInteractionSource()
                        }, indication = null, onClick = {
                            isBtmSheetVisible.value = true
                            isDiscographyBtmSheetEnabled.value = false
                            coroutineScope.launch {
                                bottomModalSheetState.expand()
                            }
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = artistBio.value.trim()
                            .replace("\n", "\n\n") + if (artistBio.value.endsWith(".")) "" else ".",
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalContentColor.current,
                        maxLines = 5,
                        fontSize = 16.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(10.dp)
                    )
                    IconButton(onClick = {
                        isBtmSheetVisible.value = true
                        isDiscographyBtmSheetEnabled.value = false
                        coroutineScope.launch {
                            bottomModalSheetState.expand()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "")
                    }
                }
            }
        }

        item {
            Text(
                text = "Socials",
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
                modifier = Modifier.padding(10.dp)
            )
        }
        item {
            detailsViewModel.artistSocials.value.forEach {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            localUriHandler.openUri(it)
                        }) {
                    IconButton(onClick = {
                        localUriHandler.openUri(it)
                    }) {
                        CoilImage(
                            imgUrl = when {
                                it.lowercase()
                                    .contains("facebook") -> "https://store-images.s-microsoft.com/image/apps.37935.9007199266245907.b029bd80-381a-4869-854f-bac6f359c5c9.91f8693c-c75b-4050-a796-63e1314d18c9?h=210"

                                it.lowercase()
                                    .contains("instagram") -> "https://lookaside.fbsbx.com/elementpath/media/?media_id=676073767417807&version=1711727173"

                                it.lowercase()
                                    .contains("twitter") -> "https://store-images.s-microsoft.com/image/apps.60673.9007199266244427.4d45042b-d7a5-4a83-be66-97779553b24d.5d82b7eb-9734-4b51-b65d-a0383348ab1b?h=210"

                                it.lowercase()
                                    .contains("wikipedia") -> "https://store-images.s-microsoft.com/image/apps.65178.9007199266246789.dfb1c4fb-983f-4ce1-82c2-212765396aff.e4c94818-3916-474c-ac14-3be893975101?h=210"

                                else -> ""
                            }, modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (!it
                                            .lowercase()
                                            .contains("instagram")
                                    ) Modifier.clip(CircleShape) else Modifier
                                ), contentDescription = ""
                        )
                    }
                    androidx.compose.material3.Text(
                        style = MaterialTheme.typography.titleSmall,
                        text = when {
                            it.lowercase()
                                .contains("facebook") -> "Facebook"

                            it.lowercase()
                                .contains("instagram") -> "Instagram"

                            it.lowercase()
                                .contains("twitter") -> "Twitter"

                            it.lowercase()
                                .contains("wikipedia") -> "Wikipedia"

                            else -> ""
                        }
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
    if (isBtmSheetVisible.value) {
        ModalBottomSheet(dragHandle = {
            if (isDiscographyBtmSheetEnabled.value)
                BottomSheetDefaults.DragHandle()
            else {
            }
        }, onDismissRequest = {
            coroutineScope.launch {
                bottomModalSheetState.hide()
            }.invokeOnCompletion {
                isBtmSheetVisible.value = false
            }
        }) {
            if (isDiscographyBtmSheetEnabled.value) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .navigationBarsPadding()
                ) {
                    item {
                        Text(
                            text = "Discography",
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalContentColor.current,
                            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp)
                        )
                    }
                    items(albums) {
                        AlbumxTrackHorizontalPreview(
                            onClick = {
                                onDiscographyItemClick(it)
                            },
                            itemType = it.album_type.capitalize(),
                            albumImgUrl = it.images.first().url,
                            albumTitle = it.name,
                            artistName = it.artists.joinToString { it.name },
                            isExplicit = false
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    ArtistCoverArt(
                        artistCoverArtState = ArtistCoverArtState(
                            specificArtistFromSpotifyDTO.value.name,
                            specificArtistFromSpotifyDTO.value.images.first().url
                        )
                    )
                    Text(
                        text = artistBio.value,
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalContentColor.current,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(10.dp)
                    )
                }
            }
        }
    }
    BackHandler {
        coroutineScope.launch {
            awaitAll(async {
                mediaPlayer.stop()
                mediaPlayer.reset()
            }, async {
                navController.popBackStack()
            })
        }
    }
}