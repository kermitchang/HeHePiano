package com.ruxor.kermitpiano.feature.waterfallrenderer

import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongNote
import com.ruxor.kermitpiano.core.timeline.SongTime

internal class VisibleNoteIndex(private val song: Song) {
    fun noteAt(index: Int): SongNote = song.notes[index]

    fun indicesBetween(from: SongTime, through: SongTime): IntRange {
        if (from > through) return IntRange.EMPTY

        var low = 0
        var high = song.notes.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (song.notes[middle].songTime < from) {
                low = middle + 1
            } else {
                high = middle
            }
        }

        val firstVisibleIndex = low
        while (low < song.notes.size && song.notes[low].songTime <= through) {
            low += 1
        }

        return firstVisibleIndex until low
    }
}
