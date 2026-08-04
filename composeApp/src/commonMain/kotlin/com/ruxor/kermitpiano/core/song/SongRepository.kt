package com.ruxor.kermitpiano.core.song

/**
 * Provides materialized songs without exposing how their source is stored or parsed.
 */
internal interface SongRepository {
    fun availableSongs(): List<Song>
}
