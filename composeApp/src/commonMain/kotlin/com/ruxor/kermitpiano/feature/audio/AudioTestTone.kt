package com.ruxor.kermitpiano.feature.audio

import kotlinx.coroutines.delay

internal suspend fun playTestC4(
    engine: PianoAudioEngine,
    pause: suspend (Long) -> Unit = { delay(it) },
): Boolean {
    if (engine.state.value !is AudioEngineState.Ready) return false
    engine.noteOn(60, 100)
    pause(500)
    engine.noteOff(60)
    return true
}
