package musicboxd.android.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import musicboxd.android.TEMP_PASSWORD
import musicboxd.android.TEMP_USER_NAME
import musicboxd.android.data.local.model.review.Review
import musicboxd.android.data.local.review.ReviewRepo
import musicboxd.android.data.remote.api.APIResult
import musicboxd.android.data.remote.api.musicboxd.MusicBoxdAPIRepo
import musicboxd.android.data.remote.api.musicboxd.model.MusicBoxdLoginDTO
import musicboxd.android.data.remote.api.musicboxd.model.ReviewDTO
import musicboxd.android.data.remote.api.spotify.model.tracklist.Artist
import musicboxd.android.ui.details.album.AlbumDetailScreenState
import javax.inject.Inject

@HiltViewModel
class ReviewScreenViewModel @Inject constructor(
    private val musicBoxdAPIRepo: MusicBoxdAPIRepo,
    private val reviewRepo: ReviewRepo
) :
    ViewModel() {

    private val _reviewScreenUIChannel = Channel<ReviewScreenUIEvent>()
    val reviewScreenUIChannel = _reviewScreenUIChannel.receiveAsFlow()

    fun postANewReview(reviewDTO: ReviewDTO) {
        viewModelScope.launch(Dispatchers.Default) {
            when (val tokenData = musicBoxdAPIRepo.getUserToken(
                MusicBoxdLoginDTO(
                    userName = TEMP_USER_NAME,
                    password = TEMP_PASSWORD
                )
            )) {
                is APIResult.Failure -> TODO()
                is APIResult.Success -> {
                    when (val postedReview =
                        musicBoxdAPIRepo.postANewReview(reviewDTO, tokenData.data.jwt)) {
                        is APIResult.Failure -> {
                            sendAnEvent(ReviewScreenUIEvent.ShowToast(postedReview.message))
                        }

                        is APIResult.Success -> {
                            viewModelScope.launch {
                                sendAnEvent(ReviewScreenUIEvent.ShowToast(postedReview.data))
                            }
                        }
                    }
                }
            }
        }
    }

    var currentLocalReview = Review(
        localReviewId = 0L,
        remoteReviewId = 0L,
        releaseType = "",
        releaseName = "",
        artists = listOf(),
        spotifyUri = "",
        isExplicit = false,
        reviewContent = "",
        isLiked = false,
        isRecommended = false,
        gotAddedIntoAnyLists = false,
        listIds = listOf(),
        reviewedByUserID = 0L,
        reviewedOnDate = "",
        reviewTitle = "",
        rating = 0.0f,
        timeStamp = 0L,
        genres = listOf(),
        noOfLikesForThisReview = 0L,
        reviewTags = listOf(),
        releaseImgUrl = ""
    )

    fun createANewLocalReview(albumDetailScreenState: AlbumDetailScreenState) {
        viewModelScope.launch {
            if (!reviewRepo.doesThisReviewExistsOnLocalDevice(albumDetailScreenState.itemUri)) {
                reviewRepo.createANewLocalReview(
                    Review(
                        remoteReviewId = 0L,
                        releaseType = albumDetailScreenState.itemType,
                        releaseName = albumDetailScreenState.albumTitle,
                        artists = albumDetailScreenState.artists.map {
                            Artist(
                                id = it.id,
                                name = it.name,
                                uri = it.uri
                            )
                        },
                        spotifyUri = albumDetailScreenState.itemUri,
                        isExplicit = false,
                        reviewContent = "",
                        isLiked = false,
                        isRecommended = false,
                        gotAddedIntoAnyLists = false,
                        listIds = listOf(),
                        reviewedByUserID = 0L,
                        reviewedOnDate = "",
                        reviewTitle = "",
                        rating = 0.0f,
                        timeStamp = 0L,
                        genres = listOf(),
                        noOfLikesForThisReview = 0L,
                        reviewTags = listOf(),
                        releaseImgUrl = albumDetailScreenState.albumImgUrl
                    )
                )
                currentLocalReview = reviewRepo.getTheLatestAddedLocalReview()
            } else {
                currentLocalReview =
                    reviewRepo.getASpecificLocalReview(albumDetailScreenState.itemUri)
            }
        }
    }

    private val _localReviews = MutableStateFlow(emptyList<Review>())
    val localReviews = _localReviews.asStateFlow()

    fun loadLocalReviews() {
        viewModelScope.launch {
            reviewRepo.getAllExistingLocalReviews().collectLatest {
                _localReviews.emit(it)
            }
        }
    }

    fun updateAnExistingLocalReview(review: Review) {
        viewModelScope.launch {
            reviewRepo.updateAnExistingLocalReview(review)
        }
    }

    private suspend fun sendAnEvent(reviewScreenUIEvent: ReviewScreenUIEvent) {
        _reviewScreenUIChannel.send(reviewScreenUIEvent)
    }
}