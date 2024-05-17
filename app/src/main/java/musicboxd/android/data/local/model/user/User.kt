package musicboxd.android.data.local.model.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val userProfileName: String,
    val userName: String,
    val userBio: String,
    val userWebsite: String,
    val profilePicBase64Data: String,
    val profileHeaderBase64Data: String,
    val userLocation: String,
    val userJoinedDate: String,
    val followingCount: Long,
    val followersCount: Long,
    val reviewsCount: Long,
    val reviews: List<Long>, // Long = remoteReviewId from `Review` table
    val recommendations: List<Long>, // Long = releaseId from `Release` table
    val likedReleases: List<Long>, // Long = releaseId from `Release` table
    val listenedReleases: List<Long>, // Long = releaseId from `Release` table
    val optedInNotifications: List<String>,
    val accountStatus: String,
    val lastActive: String
)