package musicboxd.android.data.local.model.review

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import musicboxd.android.data.local.model.user.User
import musicboxd.android.data.remote.api.spotify.model.tracklist.Artist

@Entity(
    tableName = "review", foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["reviewedByUserID"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val releaseType: String,
    val releaseName: String,
    val artists: List<Artist>,
    val spotifyUri: String,
    val isExplicit: Boolean,
    val reviewContent: String,
    val isLiked: Boolean,
    val isRecommended: Boolean,
    val gotAddedIntoAnyLists: Boolean,
    val listIds: List<Long>?,
    val reviewedByUserID: Long,
    val reviewedOnDate: String,
    val reviewTitle: String,
    val rating: Float,
    val timeStamp: Long,
    val genres: List<String>,
    val noOfLikesForThisReview: Long,
    val reviewTags: List<String>,
    val reviewMood: String?
)