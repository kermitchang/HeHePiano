package com.ruxor.hehepiano.feature.pianolayout

internal data class PianoViewport(
    val firstVisibleMidi: Int,
    val lastVisibleMidi: Int,
    val mode: PianoViewportMode,
) {
    init {
        require(firstVisibleMidi in PianoModel.firstMidi..PianoModel.lastMidi) {
            "Viewport start must be inside the 88-key piano range."
        }
        require(lastVisibleMidi in PianoModel.firstMidi..PianoModel.lastMidi) {
            "Viewport end must be inside the 88-key piano range."
        }
        require(firstVisibleMidi <= lastVisibleMidi) { "Viewport start must not follow its end." }
    }

    val visibleMidiRange: IntRange = firstVisibleMidi..lastVisibleMidi

    internal companion object {
        val practice = PianoViewport(48, 96, PianoViewportMode.Practice)
        val full88 = PianoViewport(PianoModel.firstMidi, PianoModel.lastMidi, PianoViewportMode.Full88)

        fun practice(profile: PianoDeviceProfile, octaveDelta: Int): PianoViewport {
            val range = profile.shiftedByOctaves(octaveDelta)
            return PianoViewport(range.lowestMidiNote, range.highestMidiNote, PianoViewportMode.Practice)
        }

        fun ak490(keyboardOctave: Int): PianoViewport =
            practice(PianoDeviceProfile.ak490, keyboardOctave - 4)
        }
}

internal enum class PianoViewportMode {
    Practice,
    Full88,
    FollowSong,
}
