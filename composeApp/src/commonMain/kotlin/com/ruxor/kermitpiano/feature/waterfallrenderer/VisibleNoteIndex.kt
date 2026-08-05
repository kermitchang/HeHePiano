package com.ruxor.kermitpiano.feature.waterfallrenderer

import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongNote
import com.ruxor.kermitpiano.core.timeline.SongTime

internal class VisibleNoteIndex(private val song: Song) {
    private val prefixMaxEndTimes = buildList {
        var maxEnd = SongTime.zero
        song.notes.forEach { note ->
            maxEnd = maxOf(maxEnd, note.endTime)
            add(maxEnd)
        }
    }

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

    fun visibleIndicesBetween(from: SongTime, through: SongTime): List<Int> {
        if (from > through || song.notes.isEmpty()) return emptyList()

        val endExclusive = upperBoundStart(through)
        if (endExclusive == 0) return emptyList()

        val firstCandidate = firstIndexWithEndAtLeast(from, endExclusive)
        if (firstCandidate >= endExclusive) return emptyList()

        return (firstCandidate until endExclusive)
            .filter { index -> song.notes[index].endTime >= from }
    }

    private fun upperBoundStart(time: SongTime): Int {
        var low = 0
        var high = song.notes.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (song.notes[middle].songTime <= time) low = middle + 1 else high = middle
        }
        return low
    }

    private fun firstIndexWithEndAtLeast(time: SongTime, endExclusive: Int): Int {
        var low = 0
        var high = endExclusive
        while (low < high) {
            val middle = (low + high) ushr 1
            if (prefixMaxEndTimes[middle] < time) low = middle + 1 else high = middle
        }
        return low
    }
}
