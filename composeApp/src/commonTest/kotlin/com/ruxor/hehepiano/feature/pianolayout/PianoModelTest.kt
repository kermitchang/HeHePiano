package com.ruxor.hehepiano.feature.pianolayout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PianoModelTest {
    @Test
    fun `contains the complete 88-key A0 through C8 piano`() {
        assertEquals(88, PianoModel.keys.size)
        assertEquals(52, PianoModel.whiteKeys.size)
        assertEquals(36, PianoModel.blackKeys.size)
        assertEquals(21, PianoModel.keys.first().note.value)
        assertEquals("A0", PianoModel.keys.first().note.label)
        assertEquals(108, PianoModel.keys.last().note.value)
        assertEquals("C8", PianoModel.keys.last().note.label)
    }

    @Test
    fun `finds exactly one key for every MIDI note in the piano range`() {
        val keys = (PianoModel.firstMidi..PianoModel.lastMidi).map { midiValue ->
            assertNotNull(PianoModel.keyFor(midiValue))
        }

        assertEquals(88, keys.distinctBy { key -> key.note.value }.size)
    }
}
