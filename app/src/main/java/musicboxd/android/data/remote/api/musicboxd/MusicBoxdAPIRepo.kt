package musicboxd.android.data.remote.api.musicboxd

import musicboxd.android.data.remote.api.APIResult
import musicboxd.android.data.remote.api.musicboxd.model.MusicBoxdLoginDTO
import musicboxd.android.data.remote.api.musicboxd.model.MusicBoxdTokenDTO
import musicboxd.android.data.remote.api.musicboxd.model.ReviewDTO
import musicboxd.android.data.remote.api.musicboxd.model.review.MusicBoxdPublicReviews
import retrofit2.http.Body

interface MusicBoxdAPIRepo {
    suspend fun postANewReview(@Body reviewDTO: ReviewDTO, authorization: String): APIResult<String>
    suspend fun getReviews(): APIResult<List<MusicBoxdPublicReviews>>

    suspend fun getUserToken(@Body musicBoxdLoginDTO: MusicBoxdLoginDTO): APIResult<MusicBoxdTokenDTO>
}