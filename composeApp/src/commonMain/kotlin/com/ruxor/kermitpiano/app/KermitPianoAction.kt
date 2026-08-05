package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.feature.midi.MidiFileSelection
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import com.ruxor.kermitpiano.feature.midi.TrackHand
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode
import com.ruxor.kermitpiano.feature.playback.PlaybackAction
import com.ruxor.kermitpiano.feature.songlibrary.SongFile

internal sealed interface KermitPianoAction {
    data object FrameAdvanced : KermitPianoAction

    data class Playback(val action: PlaybackAction) : KermitPianoAction

    data object SelectNextSong : KermitPianoAction

    data class SetViewportMode(val mode: PianoViewportMode) : KermitPianoAction

    data object ToggleDebug : KermitPianoAction

    data object ShowLibrary : KermitPianoAction

    data object HideLibrary : KermitPianoAction

    data class SetPlayerSoundEnabled(val enabled: Boolean) : KermitPianoAction

    data class AnalyzeMidi(val file: SelectedMidiFile) : KermitPianoAction

    data class LoadMidiFile(val selection: MidiFileSelection) : KermitPianoAction

    data class UpdateTrackMapping(val trackIndex: Int, val hand: TrackHand) : KermitPianoAction

    data object CancelMidiAnalysis : KermitPianoAction

    data object ImportAnalyzedMidi : KermitPianoAction

    data object RefreshLocalSongs : KermitPianoAction

    data class OpenLocalSong(val file: SongFile) : KermitPianoAction
}
