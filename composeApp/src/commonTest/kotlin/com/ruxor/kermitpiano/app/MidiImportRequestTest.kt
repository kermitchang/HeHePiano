package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.feature.midi.MidiFileSelection
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import kotlin.test.Test
import kotlin.test.assertEquals

class MidiImportRequestTest {
    @Test
    fun `open MIDI request invokes the existing picker and forwards its selected file`() {
        val selected = SelectedMidiFile("practice.mid", byteArrayOf(1, 2, 3))
        val selection = MidiFileSelection(selected.name) { selected }
        var pickerCalls = 0
        var received: MidiFileSelection? = null

        requestMidiImport(
            openMidiFile = { pickerCalls += 1; selection },
            onFileSelected = { received = it },
        )

        assertEquals(1, pickerCalls)
        assertEquals(selected.name, received?.name)
    }
}
