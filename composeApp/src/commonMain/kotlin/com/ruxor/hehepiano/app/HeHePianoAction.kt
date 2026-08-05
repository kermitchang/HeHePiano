package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.feature.midi.MidiFileSelection
import com.ruxor.hehepiano.feature.midi.SelectedMidiFile
import com.ruxor.hehepiano.feature.midi.TrackHand
import com.ruxor.hehepiano.feature.pianolayout.PianoViewportMode
import com.ruxor.hehepiano.feature.practice.PracticeMode
import com.ruxor.hehepiano.feature.playback.PlaybackAction
import com.ruxor.hehepiano.feature.songlibrary.SongFile

internal sealed interface HeHePianoAction {
    data object FrameAdvanced : HeHePianoAction

    data class Playback(val action: PlaybackAction) : HeHePianoAction

    data object SelectNextSong : HeHePianoAction

    data class SetViewportMode(val mode: PianoViewportMode) : HeHePianoAction

    data object ToggleDebug : HeHePianoAction

    data object ShowLibrary : HeHePianoAction

    data object HideLibrary : HeHePianoAction

    data class SetPlayerSoundEnabled(val enabled: Boolean) : HeHePianoAction

    data class SetDemoMode(val enabled: Boolean) : HeHePianoAction

    data class SetPracticeMode(val mode: PracticeMode) : HeHePianoAction

    data class AnalyzeMidi(val file: SelectedMidiFile) : HeHePianoAction

    data class LoadMidiFile(val selection: MidiFileSelection) : HeHePianoAction

    data class UpdateTrackMapping(val trackIndex: Int, val hand: TrackHand) : HeHePianoAction

    data object CancelMidiAnalysis : HeHePianoAction

    data object ImportAnalyzedMidi : HeHePianoAction

    data object RefreshLocalSongs : HeHePianoAction

    data class OpenLocalSong(val file: SongFile) : HeHePianoAction
}
