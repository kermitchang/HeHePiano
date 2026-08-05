package com.ruxor.hehepiano.core.timeline

import kotlin.time.TimeSource

internal class MonotonicGameClock : GameClock {
    private val origin = TimeSource.Monotonic.markNow()

    override fun now(): GameTime = GameTime(origin.elapsedNow())
}
