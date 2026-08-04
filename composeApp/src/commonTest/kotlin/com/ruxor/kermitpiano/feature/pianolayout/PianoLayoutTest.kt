package com.ruxor.kermitpiano.feature.pianolayout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PianoLayoutTest {
    @Test
    fun `uses key rect centers for white and black key alignment`() {
        val layout = PianoLayout.create(PianoViewport.practice, width = 900f, height = 196f)
        val cSharp = layout.keyFor(61)!!
        val dSharp = layout.keyFor(63)!!
        val e = layout.keyFor(64)!!
        val f = layout.keyFor(65)!!

        assertEquals(cSharp.keyRect.center.x, cSharp.centerX)
        assertEquals(dSharp.keyRect.center.x, dSharp.centerX)
        assertTrue(cSharp.zOrder > layout.keyFor(60)!!.zOrder)
        assertTrue(e.centerX < f.centerX)
        assertFalse(layout.keys.any { key -> key.key.note.value == 65 && key.key.kind == PianoKeyKind.Black })
        assertFalse(layout.keys.any { key -> key.key.note.value == 72 && key.key.kind == PianoKeyKind.Black })
    }

    @Test
    fun `recalculates all visible key positions when layout width changes`() {
        val narrow = PianoLayout.create(PianoViewport.full88, width = 880f, height = 196f)
        val wide = PianoLayout.create(PianoViewport.full88, width = 1_760f, height = 196f)

        assertEquals(88, narrow.keys.size)
        assertEquals(88, wide.keys.size)
        assertEquals(narrow.keyFor(60)!!.centerX * 2f, wide.keyFor(60)!!.centerX)
    }
}
