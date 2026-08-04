package com.ruxor.kermitpiano.feature.pianolayout

import kotlin.test.Test
import kotlin.test.assertEquals

class PianoDeviceProfileTest {
    @Test
    fun `AK490 practice range includes exactly forty nine midi notes`() {
        val viewport = PianoViewport.practice(PianoDeviceProfile.ak490, octaveDelta = 0)

        assertEquals(49, viewport.visibleMidiRange.count())
        assertEquals(48, viewport.firstVisibleMidi)
        assertEquals(96, viewport.lastVisibleMidi)
    }

    @Test
    fun `practice octave shifts clamp to piano boundaries`() {
        val low = PianoViewport.practice(PianoDeviceProfile.ak490, octaveDelta = -10)
        val high = PianoViewport.practice(PianoDeviceProfile.ak490, octaveDelta = 10)

        assertEquals(21, low.firstVisibleMidi)
        assertEquals(69, low.lastVisibleMidi)
        assertEquals(60, high.firstVisibleMidi)
        assertEquals(108, high.lastVisibleMidi)
    }
}
