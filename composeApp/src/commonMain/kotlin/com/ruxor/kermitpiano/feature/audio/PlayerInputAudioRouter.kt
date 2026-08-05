package com.ruxor.kermitpiano.feature.audio

/** Routes immediate player input without coupling keyboard or MIDI devices to a backend. */
internal class PlayerInputAudioRouter(private val engine: PianoAudioEngine) {
    var enabled: Boolean = true
        set(value) {
            if (field && !value) engine.allNotesOff()
            field = value
        }

    fun noteOn(note: Int, velocity: Int, channel: Int = 0) {
        if (enabled) engine.noteOn(note, velocity, channel)
    }

    fun noteOff(note: Int, channel: Int = 0) {
        engine.noteOff(note, channel)
    }

    fun pitchBend(value: Int, channel: Int = 0) {
        if (enabled) engine.pitchBend(value, channel)
    }

    fun controlChange(controller: Int, value: Int, channel: Int = 0) {
        if (enabled) engine.controlChange(controller, value, channel)
    }

    fun onRestart() = engine.allNotesOff()

    fun onSongChanged() = engine.allNotesOff()
}
