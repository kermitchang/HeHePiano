package com.ruxor.kermitpiano.core.playerinput

import com.ruxor.kermitpiano.core.music.MidiNote

internal data class PlayerInputState(
    val pressedNotes: Set<MidiNote> = emptySet(),
    val notesBySource: Map<PlayerInputSource, Set<MidiNote>> = emptyMap(),
)
