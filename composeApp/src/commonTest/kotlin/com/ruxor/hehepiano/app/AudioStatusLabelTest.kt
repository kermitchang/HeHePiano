package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.feature.audio.AudioEngineState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioStatusLabelTest {
    @Test
    fun `audio status labels are compact single line strings`() {
        val labels = listOf(
            AudioEngineState.Uninitialized.topBarLabel(),
            AudioEngineState.Initializing.topBarLabel(),
            AudioEngineState.Ready("test", "piano.sf2").topBarLabel(),
            AudioEngineState.Error("SoundFont Missing").topBarLabel(),
            AudioEngineState.Error("FluidSynth Missing").topBarLabel(),
        )

        assertEquals(listOf("Audio Off", "Audio…", "Audio Ready", "SF2 Missing", "FluidSynth Missing"), labels)
        assertTrue(labels.all { it.length <= 18 && '\n' !in it })
    }
}
