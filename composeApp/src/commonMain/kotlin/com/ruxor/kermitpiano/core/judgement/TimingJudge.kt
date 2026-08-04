package com.ruxor.kermitpiano.core.judgement

import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.time.Duration

internal class TimingJudge(private val windows: JudgementWindows) {
    fun judge(noteTime: SongTime, inputTime: SongTime): Judgement {
        val offset = inputTime.elapsed - noteTime.elapsed
        val absoluteOffset = if (offset < Duration.ZERO) -offset else offset

        return when {
            absoluteOffset <= windows.perfect -> Judgement.Perfect
            absoluteOffset <= windows.good -> Judgement.Good
            else -> Judgement.Miss
        }
    }
}
