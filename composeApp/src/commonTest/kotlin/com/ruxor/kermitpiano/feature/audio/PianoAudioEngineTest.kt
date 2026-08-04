package com.ruxor.kermitpiano.feature.audio

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PianoAudioEngineTest {
    @Test
    fun `fake engine forwards note on and note off`() = runBlocking {
        val engine = FakePianoAudioEngine()
        engine.initialize(PianoAudioConfig())
        engine.noteOn(60, 96)
        engine.noteOff(60)

        assertEquals(listOf(AudioEvent.NoteOn(60, 96, 0), AudioEvent.NoteOff(60, 0)), engine.events)
    }

    @Test
    fun `audio config rejects invalid values`(): Unit = runBlocking {
        val engine = FakePianoAudioEngine()
        engine.initialize(PianoAudioConfig(program = 128))

        assertIs<AudioEngineState.Error>(engine.state.value)
    }

    @Test
    fun `engine lifecycle supports all notes off and close`() = runBlocking {
        val engine = FakePianoAudioEngine()
        engine.initialize(PianoAudioConfig())
        engine.allNotesOff()
        engine.close()

        assertEquals(listOf(AudioEvent.AllNotesOff, AudioEvent.Closed), engine.events)
        assertEquals(AudioEngineState.Uninitialized, engine.state.value)
    }

    @Test
    fun `router forwards input and clears notes for restart and song changes`() {
        val engine = FakePianoAudioEngine()
        val router = PlayerInputAudioRouter(engine)

        router.noteOn(60, 96)
        router.noteOff(60)
        router.onRestart()
        router.onSongChanged()

        assertEquals(
            listOf(AudioEvent.NoteOn(60, 96, 0), AudioEvent.NoteOff(60, 0), AudioEvent.AllNotesOff, AudioEvent.AllNotesOff),
            engine.events,
        )
    }
}
