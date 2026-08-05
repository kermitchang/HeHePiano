package com.ruxor.hehepiano.feature.audio

/** Routes immediate player input without coupling keyboard or MIDI devices to a backend. */
internal class PlayerInputAudioRouter(private val engine: PianoAudioEngine) {
    var enabled: Boolean = true
        set(value) {
            if (field && !value) engine.allNotesOff()
            field = value
        }

    var inputSuspended: Boolean = false
        set(value) {
            if (!field && value) engine.allNotesOff()
            field = value
        }

    fun noteOn(note: Int, velocity: Int, channel: Int = 0) {
        if (enabled && !inputSuspended) engine.noteOn(note, velocity, channel)
    }

    fun noteOff(note: Int, channel: Int = 0) {
        if (!inputSuspended) engine.noteOff(note, channel)
    }

    fun pitchBend(value: Int, channel: Int = 0) {
        if (enabled && !inputSuspended) engine.pitchBend(value, channel)
    }

    fun controlChange(controller: Int, value: Int, channel: Int = 0) {
        if (enabled && !inputSuspended) engine.controlChange(controller, value, channel)
    }

    fun onRestart() = engine.allNotesOff()

    fun onSongChanged() = engine.allNotesOff()
}
