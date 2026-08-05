package com.ruxor.hehepiano.feature.gamevisual

import com.ruxor.hehepiano.core.music.MidiNote
import kotlin.test.Test
import kotlin.test.assertEquals

class GameVisualTokensTest {
    @Test
    fun `maps lower notes to left hand and middle C upward to right hand`() {
        assertEquals(PianoHand.Left, GameVisualTokens.handFor(MidiNote(48)))
        assertEquals(PianoHand.Right, GameVisualTokens.handFor(MidiNote(60)))
    }
}
