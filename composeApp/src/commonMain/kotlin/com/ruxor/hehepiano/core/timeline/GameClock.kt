package com.ruxor.hehepiano.core.timeline

internal fun interface GameClock {
    fun now(): GameTime
}
