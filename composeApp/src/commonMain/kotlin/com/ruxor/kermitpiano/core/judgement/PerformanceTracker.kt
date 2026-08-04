package com.ruxor.kermitpiano.core.judgement

import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.time.Duration

internal class PerformanceTracker(windows: JudgementWindows) {
    private val timingJudge = TimingJudge(windows)
    private var combo = ComboState()

    fun snapshot(): ComboState = combo

    fun evaluate(noteTiming: NoteTiming): JudgementResult {
        val judgement = timingJudge.judge(noteTiming.noteTime, noteTiming.inputTime)
        return record(judgement, noteTiming.inputTime.elapsed - noteTiming.noteTime.elapsed)
    }

    fun recordMiss(): JudgementResult = record(Judgement.Miss, timingOffset = null)

    private fun record(judgement: Judgement, timingOffset: Duration?): JudgementResult {
        combo = when (judgement) {
            Judgement.Perfect,
            Judgement.Good,
            -> {
                val current = combo.current + 1
                ComboState(current = current, best = maxOf(combo.best, current))
            }

            Judgement.Miss -> ComboState(best = combo.best)
        }

        return JudgementResult(
            judgement = judgement,
            combo = combo,
            timingOffset = timingOffset,
        )
    }
}
