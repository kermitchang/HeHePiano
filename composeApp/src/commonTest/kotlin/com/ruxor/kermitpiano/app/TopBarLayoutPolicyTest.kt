package com.ruxor.kermitpiano.app

import kotlin.test.Test
import kotlin.test.assertEquals

class TopBarLayoutPolicyTest {
    @Test
    fun `wide layouts keep all top bar groups visible`() {
        assertEquals(TopBarLayoutMode.Wide, TopBarLayoutPolicy.modeFor(1_600))
    }

    @Test
    fun `medium layouts use compact controls`() {
        assertEquals(TopBarLayoutMode.Compact, TopBarLayoutPolicy.modeFor(1_200))
    }

    @Test
    fun `narrow layouts move utilities into more menu`() {
        assertEquals(TopBarLayoutMode.Narrow, TopBarLayoutPolicy.modeFor(999))
    }

    @Test
    fun `every top bar layout exposes MIDI import and local library`() {
        TopBarLayoutMode.entries.forEach { mode ->
            val features = TopBarLayoutPolicy.featuresFor(mode)
            assertEquals(true, features.showsOpenMidi)
            assertEquals(true, features.exposesLocalLibrary)
        }
    }

    @Test
    fun `only wide layout reserves room for song title`() {
        assertEquals(true, TopBarLayoutPolicy.featuresFor(TopBarLayoutMode.Wide).showsSongTitle)
        assertEquals(false, TopBarLayoutPolicy.featuresFor(TopBarLayoutMode.Compact).showsSongTitle)
        assertEquals(false, TopBarLayoutPolicy.featuresFor(TopBarLayoutMode.Narrow).showsSongTitle)
    }
}
