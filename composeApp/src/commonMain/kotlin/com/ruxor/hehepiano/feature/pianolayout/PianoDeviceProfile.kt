package com.ruxor.hehepiano.feature.pianolayout

internal data class PianoDeviceProfile(
    val id: String,
    val displayName: String,
    val keyCount: Int,
    val lowestMidiNote: Int,
    val highestMidiNote: Int,
) {
    init {
        require(keyCount > 0)
        require(highestMidiNote - lowestMidiNote + 1 == keyCount)
    }

    fun shiftedByOctaves(octaveDelta: Int): PianoDeviceProfile {
        val requestedFirst = lowestMidiNote + octaveDelta * 12
        val first = requestedFirst.coerceIn(PianoModel.firstMidi, PianoModel.lastMidi - keyCount + 1)
        return copy(lowestMidiNote = first, highestMidiNote = first + keyCount - 1)
    }

    internal companion object {
        val computerKeyboard = PianoDeviceProfile("computer", "Computer Keyboard", 17, 60, 76)
        val ak490 = PianoDeviceProfile("ak490", "AK490 49 Keys", 49, 48, 96)

        fun customRange(firstMidi: Int, lastMidi: Int): PianoDeviceProfile = PianoDeviceProfile(
            id = "custom-$firstMidi-$lastMidi",
            displayName = "Custom Range",
            keyCount = lastMidi - firstMidi + 1,
            lowestMidiNote = firstMidi,
            highestMidiNote = lastMidi,
        )
    }
}
