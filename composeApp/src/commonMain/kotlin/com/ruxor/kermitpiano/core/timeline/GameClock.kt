package com.ruxor.kermitpiano.core.timeline

internal fun interface GameClock {
    fun now(): GameTime
}
