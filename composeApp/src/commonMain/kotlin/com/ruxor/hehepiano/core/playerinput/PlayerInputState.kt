package com.ruxor.hehepiano.core.playerinput

import com.ruxor.hehepiano.core.music.MidiNote

internal data class PlayerInputState(
    val pressedNotes: Set<MidiNote> = emptySet(),
    val notesBySource: Map<PlayerInputSource, Set<MidiNote>> = emptyMap(),
)
