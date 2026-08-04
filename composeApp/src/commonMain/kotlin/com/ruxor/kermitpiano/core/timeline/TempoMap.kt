package com.ruxor.kermitpiano.core.timeline

import kotlin.jvm.JvmInline
import kotlin.time.Duration.Companion.seconds

@JvmInline
internal value class BeatPosition(val value: Double) : Comparable<BeatPosition> {
    init {
        require(value.isFinite() && value >= 0.0) {
            "Beat position must be finite and non-negative."
        }
    }

    override fun compareTo(other: BeatPosition): Int = value.compareTo(other.value)
}

@JvmInline
internal value class Tempo(val beatsPerMinute: Double) {
    init {
        require(beatsPerMinute.isFinite() && beatsPerMinute > 0.0) {
            "Tempo must be finite and greater than zero."
        }
    }
}

internal data class TempoChange(
    val atBeat: BeatPosition,
    val tempo: Tempo,
)

internal class TempoMap(changes: List<TempoChange>) {
    private val changes = changes.toList()

    init {
        require(this.changes.isNotEmpty()) { "A tempo map needs an initial tempo." }
        require(this.changes.first().atBeat == BeatPosition(0.0)) {
            "The first tempo must begin at beat zero."
        }
        require(this.changes.zipWithNext().all { (first, second) -> first.atBeat < second.atBeat }) {
            "Tempo changes must be in strictly ascending beat order."
        }
    }

    fun songTimeAt(beat: BeatPosition): SongTime {
        var elapsedSeconds = 0.0

        changes.forEachIndexed { index, change ->
            if (beat <= change.atBeat) return@forEachIndexed

            val nextChangeBeat = changes.getOrNull(index + 1)?.atBeat?.value ?: beat.value
            val segmentEnd = minOf(beat.value, nextChangeBeat)
            val beatCount = segmentEnd - change.atBeat.value
            elapsedSeconds += beatCount * SECONDS_PER_MINUTE / change.tempo.beatsPerMinute
        }

        return SongTime(elapsedSeconds.seconds)
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
    }
}
