package com.ruxor.kermitpiano.app

import kotlin.test.Test
import kotlin.test.assertEquals

class TopBarLayoutPolicyTest {
    @Test
    fun `wide layouts keep all top bar groups visible`() {
        assertEquals(TopBarLayoutMode.Wide, TopBarLayoutPolicy.modeFor(1_120))
    }

    @Test
    fun `medium layouts use compact controls`() {
        assertEquals(TopBarLayoutMode.Compact, TopBarLayoutPolicy.modeFor(900))
    }

    @Test
    fun `narrow layouts move utilities into more menu`() {
        assertEquals(TopBarLayoutMode.Narrow, TopBarLayoutPolicy.modeFor(759))
    }
}
