package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.core.playerinput.PlayerInputSource
import com.ruxor.kermitpiano.core.playerinput.PlayerInputTracker
import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.feature.audio.PianoAudioEngine
import com.ruxor.kermitpiano.feature.autoplay.AutoPlayEffect
import com.ruxor.kermitpiano.feature.autoplay.AutoPlayOutput

internal class PianoAutoPlayOutput(
    private val audioEngine: PianoAudioEngine,
    private val playerInputTracker: PlayerInputTracker,
) : AutoPlayOutput {
    private val activeNotes = mutableMapOf<Pair<Int, MidiNote>, Int>()

    override fun submit(effects: List<AutoPlayEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is AutoPlayEffect.NoteOn -> {
                    val key = effect.channel to effect.note
                    activeNotes[key] = (activeNotes[key] ?: 0) + 1
                    playerInputTracker.noteOn(PlayerInputSource.AutoPlay, effect.note)
                    audioEngine.noteOn(effect.note.value, effect.velocity, effect.channel)
                }
                is AutoPlayEffect.NoteOff -> {
                    val key = effect.channel to effect.note
                    val count = activeNotes[key] ?: return@forEach
                    playerInputTracker.noteOff(PlayerInputSource.AutoPlay, effect.note)
                    if (count <= 1) {
                        activeNotes.remove(key)
                        audioEngine.noteOff(effect.note.value, effect.channel)
                    } else {
                        activeNotes[key] = count - 1
                    }
                }
                AutoPlayEffect.AllNotesOff -> stop()
            }
        }
    }

    override fun stop() {
        activeNotes.keys.forEach { (channel, note) ->
            audioEngine.noteOff(note.value, channel)
        }
        activeNotes.clear()
        playerInputTracker.releaseAll(PlayerInputSource.AutoPlay)
    }
}
