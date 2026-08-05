package com.ruxor.hehepiano.feature.autoplay

import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.core.song.PianoHand
import com.ruxor.hehepiano.core.song.Song
import com.ruxor.hehepiano.core.song.SongNote
import com.ruxor.hehepiano.core.timeline.SongTime

/** Deterministically turns a song into ordered piano note effects. */
internal class AutoPlayScheduler {
    private var song: Song? = null
    private var playableNotes: List<SongNote> = emptyList()
    private var events: List<TimedEvent> = emptyList()
    private var nextEventIndex = 0
    private val activeNotes = mutableMapOf<AutoPlayKey, Int>()
    private var playing = false

    fun load(song: Song, includedHands: Set<PianoHand> = PianoHand.entries.toSet()) {
        this.song = song
        playableNotes = song.notes.filter { note -> note.hand in includedHands }
        events = playableNotes
            .flatMap(::eventsFor)
            .sortedWith(compareBy<TimedEvent> { it.time }.thenBy { it.order })
        nextEventIndex = 0
        activeNotes.clear()
        playing = false
    }

    fun startAt(time: SongTime): List<AutoPlayEffect> {
        require(song != null) { "A song must be loaded before auto-play starts." }
        val startTime = time.coerceAtLeast(SongTime.zero)
        activeNotes.clear()
        nextEventIndex = events.indexOfFirst { event -> event.time > startTime }
            .takeUnless { it < 0 }
            ?: events.size
        val effects = buildList {
            playableNotes
                .filter { note -> note.songTime <= startTime && startTime < note.endTime }
                .sortedBy { note -> note.songTime }
                .forEach { note ->
                    add(noteOn(note))
                }
        }
        playing = true
        return effects
    }

    fun pause(): List<AutoPlayEffect> {
        if (!playing) return emptyList()
        playing = false
        activeNotes.clear()
        return listOf(AutoPlayEffect.AllNotesOff)
    }

    fun stop(): List<AutoPlayEffect> {
        val shouldStop = playing || activeNotes.isNotEmpty()
        playing = false
        activeNotes.clear()
        nextEventIndex = 0
        return if (shouldStop) listOf(AutoPlayEffect.AllNotesOff) else emptyList()
    }

    fun advance(previous: SongTime, current: SongTime): AutoPlayAdvanceResult {
        if (!playing) return AutoPlayAdvanceResult()
        if (current < previous) {
            val tailEffects = buildList {
                while (nextEventIndex < events.size && events[nextEventIndex].time <= song!!.duration) {
                    val event = events[nextEventIndex]
                    nextEventIndex += 1
                    if (event.time > previous) add(apply(event))
                }
            }.filterNotNull()
            activeNotes.clear()
            playing = false
            return AutoPlayAdvanceResult(
                effects = tailEffects + AutoPlayEffect.AllNotesOff,
                completed = true,
            )
        }

        val effects = buildList {
            while (nextEventIndex < events.size && events[nextEventIndex].time <= current) {
                val event = events[nextEventIndex]
                nextEventIndex += 1
                if (event.time > previous) add(apply(event))
            }
        }.filterNotNull()
        return AutoPlayAdvanceResult(effects = effects)
    }

    private fun apply(event: TimedEvent): AutoPlayEffect? = when (event.type) {
        TimedEventType.NoteOn -> {
            val note = event.note ?: return null
            val key = AutoPlayKey(note.channel, note.note)
            activeNotes[key] = (activeNotes[key] ?: 0) + 1
            AutoPlayEffect.NoteOn(note.note, note.velocity, note.channel)
        }
        TimedEventType.NoteOff -> {
            val note = checkNotNull(event.note)
            val key = AutoPlayKey(note.channel, note.note)
            val count = activeNotes[key] ?: return null
            if (count <= 1) {
                activeNotes.remove(key)
                AutoPlayEffect.NoteOff(key.note, key.channel)
            } else {
                activeNotes[key] = count - 1
                null
            }
        }
    }

    private fun noteOn(note: SongNote): AutoPlayEffect.NoteOn {
        val key = AutoPlayKey(note.channel, note.note)
        activeNotes[key] = (activeNotes[key] ?: 0) + 1
        return AutoPlayEffect.NoteOn(note.note, note.velocity, note.channel)
    }

    private fun eventsFor(note: SongNote): List<TimedEvent> = listOf(
        TimedEvent(note.songTime, order = 1, TimedEventType.NoteOn, note),
        TimedEvent(note.endTime, order = 0, TimedEventType.NoteOff, note),
    )

    private data class AutoPlayKey(val channel: Int, val note: MidiNote)

    private data class TimedEvent(
        val time: SongTime,
        val order: Int,
        val type: TimedEventType,
        val note: SongNote?,
    )

    private enum class TimedEventType { NoteOn, NoteOff }
}

private fun SongTime.coerceAtLeast(minimum: SongTime): SongTime = if (this < minimum) minimum else this
