package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.timeline.TimelineSnapshot
import com.ruxor.kermitpiano.feature.autoplay.AutoPlayState
import com.ruxor.kermitpiano.feature.midi.TrackHand
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode
import com.ruxor.kermitpiano.feature.practice.PracticeMode
import com.ruxor.kermitpiano.feature.songlibrary.SongFile

internal data class KermitPianoUiState(
    val librarySongs: List<Song>,
    val song: Song,
    val playbackState: TimelineSnapshot,
    val demoModeEnabled: Boolean = false,
    val demoState: AutoPlayState = AutoPlayState.Off,
    val practiceMode: PracticeMode = PracticeMode.BothHands,
    val midiImport: MidiImportState = MidiImportState.Idle,
    val trackMappings: Map<Int, TrackHand> = emptyMap(),
    val viewportMode: PianoViewportMode = PianoViewportMode.Practice,
    val debugVisible: Boolean = false,
    val libraryVisible: Boolean = false,
    val localSongs: List<SongFile> = emptyList(),
    val localLibraryLoading: Boolean = false,
    val playerSoundEnabled: Boolean = true,
    val errorMessage: String? = null,
)
