package com.ruxor.kermitpiano.feature.midi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MidiImportPolicyTest {
    @Test
    fun `accepts a non-empty file at the maximum size`() {
        val file = SelectedMidiFile("valid.mid", ByteArray(MidiImportPolicy.MAX_FILE_BYTES))

        assertNull(MidiImportPolicy.validationError(file))
    }

    @Test
    fun `rejects empty and oversized files with actionable messages`() {
        val empty = MidiImportPolicy.validationError(SelectedMidiFile("empty.mid", byteArrayOf()))
        val oversized = MidiImportPolicy.validationError(
            SelectedMidiFile("large.mid", ByteArray(MidiImportPolicy.MAX_FILE_BYTES + 1)),
        )

        assertEquals("MIDI file is empty.", empty)
        assertEquals("MIDI file exceeds the 16 MiB limit.", oversized)
    }
}
