package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.feature.midi.MidiFileSelection
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import com.ruxor.kermitpiano.feature.midi.TrackHand
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode
import com.ruxor.kermitpiano.feature.playback.PlaybackAction
import com.ruxor.kermitpiano.feature.song.DemoSongRepository
import com.ruxor.kermitpiano.feature.songlibrary.LoadableSongSource
import com.ruxor.kermitpiano.feature.songlibrary.SongFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KermitPianoStateHolderTest {
    @Test
    fun `dispatch updates application state without Compose state`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = KermitPianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            assertEquals("demo-do-re-mi", holder.state.value.song.id)

            holder.dispatch(KermitPianoAction.Playback(PlaybackAction.Play))
            holder.dispatch(KermitPianoAction.SetViewportMode(PianoViewportMode.Full88))
            holder.dispatch(KermitPianoAction.ToggleDebug)
            holder.dispatch(KermitPianoAction.ShowLibrary)
            holder.dispatch(KermitPianoAction.SetPlayerSoundEnabled(false))

            assertEquals(com.ruxor.kermitpiano.core.timeline.PlaybackState.Playing, holder.state.value.playbackState.playbackState)
            assertEquals(PianoViewportMode.Full88, holder.state.value.viewportMode)
            assertTrue(holder.state.value.debugVisible)
            assertTrue(holder.state.value.libraryVisible)
            assertFalse(holder.state.value.playerSoundEnabled)

            holder.dispatch(KermitPianoAction.SelectNextSong)

            assertEquals("chromatic-alignment-test", holder.state.value.song.id)
            assertEquals(com.ruxor.kermitpiano.core.timeline.PlaybackState.Paused, holder.state.value.playbackState.playbackState)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `MIDI analysis and import are state owner actions`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = KermitPianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(KermitPianoAction.AnalyzeMidi(SelectedMidiFile("scale.mid", scaleMidi())))

            val analysis = assertIs<MidiImportState.Ready>(holder.state.value.midiImport).analysis
            assertEquals("scale", analysis.songName)
            assertEquals(1, analysis.noteCount)
            assertEquals(TrackHand.Right, holder.state.value.trackMappings[0])

            holder.dispatch(KermitPianoAction.UpdateTrackMapping(0, TrackHand.Ignore))
            holder.dispatch(KermitPianoAction.ImportAnalyzedMidi)

            assertEquals(MidiImportState.Idle, holder.state.value.midiImport)
            assertEquals("scale", holder.state.value.song.id)
            assertEquals(0, holder.state.value.song.notes.size)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `MIDI file selection loads before analysis`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var loadCount = 0
        val holder = KermitPianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            backgroundDispatcher = Dispatchers.Unconfined,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(
                KermitPianoAction.LoadMidiFile(
                    MidiFileSelection("selected.mid") {
                        loadCount += 1
                        SelectedMidiFile("selected.mid", scaleMidi())
                    },
                ),
            )

            assertEquals(1, loadCount)
            assertEquals("selected", assertIs<MidiImportState.Ready>(holder.state.value.midiImport).analysis.songName)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `local library loading is represented by state and actions`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val source = FakeSongSource()
        val holder = KermitPianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            localSongSource = source,
            backgroundDispatcher = Dispatchers.Unconfined,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            assertEquals(listOf(source.file), holder.state.value.localSongs)
            assertFalse(holder.state.value.localLibraryLoading)

            holder.dispatch(KermitPianoAction.ShowLibrary)
            holder.dispatch(KermitPianoAction.OpenLocalSong(source.file))

            assertFalse(holder.state.value.libraryVisible)
            assertFalse(holder.state.value.localLibraryLoading)
            assertFalse(holder.state.value.midiImport is MidiImportState.Analyzing)
            assertEquals("scale", assertIs<MidiImportState.Ready>(holder.state.value.midiImport).analysis.songName)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `invalid MIDI input becomes a failure state instead of escaping`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = KermitPianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(KermitPianoAction.AnalyzeMidi(SelectedMidiFile("empty.mid", byteArrayOf())))

            val failure = assertIs<MidiImportState.Failure>(holder.state.value.midiImport)
            assertEquals("empty.mid", failure.fileName)
            assertEquals("MIDI file is empty.", failure.message)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    private fun scaleMidi(): ByteArray {
        val track = byteArrayOf(
            0, 0x90.toByte(), 60, 90,
            0x87.toByte(), 0x40, 0x80.toByte(), 60, 0,
            0, 0xFF.toByte(), 0x2F, 0,
        )
        val header = byteArrayOf(
            'M'.code.toByte(), 'T'.code.toByte(), 'h'.code.toByte(), 'd'.code.toByte(),
            0, 0, 0, 6, 0, 0, 0, 1, 1, 0xE0.toByte(),
        )
        val chunk = byteArrayOf(
            'M'.code.toByte(), 'T'.code.toByte(), 'r'.code.toByte(), 'k'.code.toByte(),
            0, 0, 0, track.size.toByte(),
        )
        return header + chunk + track
    }
}

private class FakeSongSource : LoadableSongSource {
    val file = SongFile("scale.mid", "scale.mid", 12, 0)

    override suspend fun listSongs(): List<SongFile> = listOf(file)

    override suspend fun load(songFile: SongFile): SelectedMidiFile = SelectedMidiFile(songFile.name, scaleMidiBytes())

    private fun scaleMidiBytes(): ByteArray {
        val track = byteArrayOf(
            0, 0x90.toByte(), 60, 90,
            0x87.toByte(), 0x40, 0x80.toByte(), 60, 0,
            0, 0xFF.toByte(), 0x2F, 0,
        )
        val header = byteArrayOf(
            'M'.code.toByte(), 'T'.code.toByte(), 'h'.code.toByte(), 'd'.code.toByte(),
            0, 0, 0, 6, 0, 0, 0, 1, 1, 0xE0.toByte(),
        )
        val chunk = byteArrayOf(
            'M'.code.toByte(), 'T'.code.toByte(), 'r'.code.toByte(), 'k'.code.toByte(),
            0, 0, 0, track.size.toByte(),
        )
        return header + chunk + track
    }
}
