package com.ruxor.hehepiano.feature.songlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MidiLibraryPolicyTest {
    @Test
    fun `only mid and midi files are accepted`() {
        assertTrue(MidiLibraryPolicy.isMidiFile("song.mid"))
        assertTrue(MidiLibraryPolicy.isMidiFile("SONG.MIDI"))
        assertFalse(MidiLibraryPolicy.isMidiFile("song.mp3"))
    }

    @Test
    fun `copy policy never silently overwrites duplicate names`() {
        assertEquals("theme (3).mid", MidiLibraryPolicy.uniqueCopyName("theme.mid", setOf("theme.mid", "theme (2).mid")))
    }
}
