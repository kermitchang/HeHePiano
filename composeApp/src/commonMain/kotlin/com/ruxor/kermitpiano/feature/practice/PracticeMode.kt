package com.ruxor.kermitpiano.feature.practice

import com.ruxor.kermitpiano.core.song.PianoHand

internal enum class PracticeMode(
    val playerHands: Set<PianoHand>,
    val computerHands: Set<PianoHand>,
) {
    LeftHand(
        playerHands = setOf(PianoHand.Left),
        computerHands = setOf(PianoHand.Right),
    ),
    RightHand(
        playerHands = setOf(PianoHand.Right),
        computerHands = setOf(PianoHand.Left),
    ),
    BothHands(
        playerHands = PianoHand.entries.toSet(),
        computerHands = emptySet(),
    ),
}
