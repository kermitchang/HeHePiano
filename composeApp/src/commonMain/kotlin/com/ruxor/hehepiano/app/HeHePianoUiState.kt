package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.core.song.Song
import com.ruxor.hehepiano.core.timeline.TimelineSnapshot
import com.ruxor.hehepiano.feature.autoplay.AutoPlayState
import com.ruxor.hehepiano.feature.midi.TrackHand
import com.ruxor.hehepiano.feature.pianolayout.PianoViewportMode
import com.ruxor.hehepiano.feature.practice.PracticeMode
import com.ruxor.hehepiano.feature.songlibrary.SongFile

internal data class HeHePianoUiState(
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
