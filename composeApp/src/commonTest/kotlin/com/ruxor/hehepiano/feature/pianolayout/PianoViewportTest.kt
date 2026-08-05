package com.ruxor.hehepiano.feature.pianolayout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PianoViewportTest {
    @Test
    fun `practice and full viewports stay within the standard piano range`() {
        assertEquals(48..96, PianoViewport.practice.visibleMidiRange)
        assertEquals(21..108, PianoViewport.full88.visibleMidiRange)
    }

    @Test
    fun `AK490 practice viewport follows keyboard octave without leaving piano bounds`() {
        assertEquals(48..96, PianoViewport.ak490(4).visibleMidiRange)
        assertEquals(60..108, PianoViewport.ak490(5).visibleMidiRange)
        assertEquals(21..69, PianoViewport.ak490(1).visibleMidiRange)
    }

    @Test
    fun `rejects a viewport outside the standard piano range`() {
        assertFailsWith<IllegalArgumentException> {
            PianoViewport(20, 60, PianoViewportMode.Practice)
        }
    }
}
