package com.ruxor.hehepiano.core.playerinput

import com.ruxor.hehepiano.core.music.MidiNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks overlapping notes from independent input sources without releasing notes too early. */
internal class PlayerInputTracker {
    private val lock = Any()
    private val noteCounts = PlayerInputSource.entries.associateWith { mutableMapOf<MidiNote, Int>() }.toMutableMap()
    private val mutableState = MutableStateFlow(PlayerInputState())

    val state: StateFlow<PlayerInputState> = mutableState.asStateFlow()

    fun noteOn(source: PlayerInputSource, note: MidiNote) {
        synchronized(lock) {
            val sourceNotes = checkNotNull(noteCounts[source])
            sourceNotes[note] = (sourceNotes[note] ?: 0) + 1
            publishState()
        }
    }

    fun noteOff(source: PlayerInputSource, note: MidiNote) {
        synchronized(lock) {
            val sourceNotes = checkNotNull(noteCounts[source])
            val count = sourceNotes[note] ?: return
            if (count <= 1) sourceNotes.remove(note) else sourceNotes[note] = count - 1
            publishState()
        }
    }

    fun releaseAll(source: PlayerInputSource) {
        synchronized(lock) {
            checkNotNull(noteCounts[source]).clear()
            publishState()
        }
    }

    fun releaseAll() {
        synchronized(lock) {
            noteCounts.values.forEach { notes -> notes.clear() }
            publishState()
        }
    }

    private fun publishState() {
        val notesBySource = noteCounts.mapValues { (_, notes) -> notes.keys.toSet() }
        mutableState.value = PlayerInputState(
            pressedNotes = notesBySource.values.flatten().toSet(),
            notesBySource = notesBySource,
        )
    }
}
