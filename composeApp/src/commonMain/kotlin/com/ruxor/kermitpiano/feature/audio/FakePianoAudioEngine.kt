package com.ruxor.kermitpiano.feature.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakePianoAudioEngine : PianoAudioEngine {
    private val mutableState = MutableStateFlow<AudioEngineState>(AudioEngineState.Uninitialized)
    override val state: StateFlow<AudioEngineState> = mutableState.asStateFlow()
    private val mutableDiagnostics = MutableStateFlow(AudioEngineDiagnostics(backend = "Fake"))
    override val diagnostics: StateFlow<AudioEngineDiagnostics> = mutableDiagnostics.asStateFlow()
    val events = mutableListOf<AudioEvent>()

    override suspend fun initialize(config: PianoAudioConfig) {
        mutableState.value = config.validationError()?.let(AudioEngineState::Error) ?: AudioEngineState.Ready("Fake", "fake.sf2")
    }

    override fun noteOn(note: Int, velocity: Int, channel: Int) { events += AudioEvent.NoteOn(note, velocity, channel) }
    override fun noteOff(note: Int, channel: Int) { events += AudioEvent.NoteOff(note, channel) }
    override fun controlChange(controller: Int, value: Int, channel: Int) { events += AudioEvent.ControlChange(controller, value, channel) }
    override fun allNotesOff() { events += AudioEvent.AllNotesOff }
    override suspend fun close() { events += AudioEvent.Closed; mutableState.value = AudioEngineState.Uninitialized }
}

internal sealed interface AudioEvent {
    data class NoteOn(val note: Int, val velocity: Int, val channel: Int) : AudioEvent
    data class NoteOff(val note: Int, val channel: Int) : AudioEvent
    data class ControlChange(val controller: Int, val value: Int, val channel: Int) : AudioEvent
    data object AllNotesOff : AudioEvent
    data object Closed : AudioEvent
}
