package com.ruxor.kermitpiano.feature.practice

import com.ruxor.kermitpiano.core.song.PianoHand
import kotlin.test.Test
import kotlin.test.assertEquals

class PracticeModeTest {
    @Test
    fun `left hand practice assigns right hand to computer`() {
        assertEquals(setOf(PianoHand.Left), PracticeMode.LeftHand.playerHands)
        assertEquals(setOf(PianoHand.Right), PracticeMode.LeftHand.computerHands)
    }

    @Test
    fun `right hand practice assigns left hand to computer`() {
        assertEquals(setOf(PianoHand.Right), PracticeMode.RightHand.playerHands)
        assertEquals(setOf(PianoHand.Left), PracticeMode.RightHand.computerHands)
    }

    @Test
    fun `both hands practice disables computer accompaniment`() {
        assertEquals(setOf(PianoHand.Left, PianoHand.Right), PracticeMode.BothHands.playerHands)
        assertEquals(emptySet(), PracticeMode.BothHands.computerHands)
    }
}
