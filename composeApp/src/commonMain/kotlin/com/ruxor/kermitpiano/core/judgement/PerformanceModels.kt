package com.ruxor.kermitpiano.core.judgement

import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.time.Duration

internal enum class Judgement {
    Perfect,
    Good,
    Miss,
}

internal data class JudgementWindows(
    val perfect: Duration,
    val good: Duration,
) {
    init {
        require(perfect.isFinite() && perfect >= Duration.ZERO) {
            "The perfect window must be finite and non-negative."
        }
        require(good.isFinite() && good >= perfect) {
            "The good window must be finite and no smaller than the perfect window."
        }
    }
}

internal data class ComboState(
    val current: Int = 0,
    val best: Int = 0,
) {
    init {
        require(current >= 0) { "Current combo must not be negative." }
        require(best >= current) { "Best combo must not be smaller than current combo." }
    }
}

internal data class JudgementResult(
    val judgement: Judgement,
    val combo: ComboState,
    val timingOffset: Duration?,
)

internal data class NoteTiming(
    val noteTime: SongTime,
    val inputTime: SongTime,
)
