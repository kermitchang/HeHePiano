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

    @Test
    fun `disabling player sound clears the engine and still forwards note off`() {
        val engine = FakePianoAudioEngine()
        val router = PlayerInputAudioRouter(engine)

        router.noteOn(60, 96)
        router.enabled = false
        router.noteOff(60)

        assertEquals(
            listOf(
                AudioEvent.NoteOn(60, 96, 0),
                AudioEvent.AllNotesOff,
                AudioEvent.NoteOff(60, 0),
            ),
            engine.events,
        )
    }

    @Test
    fun `suspending player input clears audio and ignores input until resumed`() {
        val engine = FakePianoAudioEngine()
        val router = PlayerInputAudioRouter(engine)

        router.noteOn(60, 96)
        router.inputSuspended = true
        router.noteOn(62, 96)
        router.noteOff(60)
        router.inputSuspended = false
        router.noteOn(64, 96)

        assertEquals(
            listOf(
                AudioEvent.NoteOn(60, 96, 0),
                AudioEvent.AllNotesOff,
                AudioEvent.NoteOn(64, 96, 0),
            ),
            engine.events,
        )
    }

    @Test
    fun `test C4 sends a note on then note off only when the engine is ready`() = runBlocking {
        val engine = FakePianoAudioEngine()

        assertEquals(false, playTestC4(engine, pause = {}))
        engine.initialize(PianoAudioConfig())

        assertEquals(true, playTestC4(engine, pause = {}))
        assertEquals(listOf(AudioEvent.NoteOn(60, 100, 0), AudioEvent.NoteOff(60, 0)), engine.events)
    }
}
