package musicboxd.android.data.remote.api.musicbrainz

import musicboxd.android.data.remote.api.musicbrainz.model.artist.MusicBrainzArtistData

interface MusicBrainzAPIRepo {
    suspend fun getArtistMetaData(artistMBID: String): MusicBrainzArtistData
}