package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.core.timeline.GameClock
import com.ruxor.hehepiano.core.timeline.GameTime
import com.ruxor.hehepiano.core.timeline.PlaybackSpeed
import com.ruxor.hehepiano.feature.autoplay.AutoPlayEffect
import com.ruxor.hehepiano.feature.autoplay.AutoPlayOutput
import com.ruxor.hehepiano.feature.autoplay.AutoPlayState
import com.ruxor.hehepiano.feature.midi.MidiFileSelection
import com.ruxor.hehepiano.feature.midi.SelectedMidiFile
import com.ruxor.hehepiano.feature.midi.TrackHand
import com.ruxor.hehepiano.feature.pianolayout.PianoViewportMode
import com.ruxor.hehepiano.feature.playback.PlaybackAction
import com.ruxor.hehepiano.feature.practice.PracticeMode
import com.ruxor.hehepiano.feature.song.DemoSongRepository
import com.ruxor.hehepiano.feature.songlibrary.LoadableSongSource
import com.ruxor.hehepiano.feature.songlibrary.SongFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class HeHePianoStateHolderTest {
    @Test
    fun `dispatch updates application state without Compose state`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            assertEquals("demo-do-re-mi", holder.state.value.song.id)

            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.Play))
            holder.dispatch(HeHePianoAction.SetViewportMode(PianoViewportMode.Full88))
            holder.dispatch(HeHePianoAction.ToggleDebug)
            holder.dispatch(HeHePianoAction.ShowLibrary)
            holder.dispatch(HeHePianoAction.SetPlayerSoundEnabled(false))

            assertEquals(com.ruxor.hehepiano.core.timeline.PlaybackState.Playing, holder.state.value.playbackState.playbackState)
            assertEquals(PianoViewportMode.Full88, holder.state.value.viewportMode)
            assertTrue(holder.state.value.debugVisible)
            assertTrue(holder.state.value.libraryVisible)
            assertFalse(holder.state.value.playerSoundEnabled)

            holder.dispatch(HeHePianoAction.SelectNextSong)

            assertEquals("chromatic-alignment-test", holder.state.value.song.id)
            assertEquals(com.ruxor.hehepiano.core.timeline.PlaybackState.Paused, holder.state.value.playbackState.playbackState)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `demo mode schedules notes from the shared playback timeline`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val clock = MutableGameClock()
        val output = RecordingAutoPlayOutput()
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            gameClock = clock,
            autoPlayOutput = output,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(HeHePianoAction.SetDemoMode(true))
            assertEquals(AutoPlayState.Ready, holder.state.value.demoState)

            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.Play))
            assertEquals(AutoPlayState.Playing, holder.state.value.demoState)
            assertEquals(
                listOf<AutoPlayEffect>(AutoPlayEffect.NoteOn(MidiNote(60), 96, 0)),
                output.effects,
            )

            clock.elapsed = 1.seconds
            holder.dispatch(HeHePianoAction.FrameAdvanced)
            assertEquals(
                listOf<AutoPlayEffect>(
                    AutoPlayEffect.NoteOn(MidiNote(60), 96, 0),
                    AutoPlayEffect.NoteOff(MidiNote(60), 0),
                    AutoPlayEffect.NoteOn(MidiNote(62), 96, 0),
                ),
                output.effects,
            )

            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.Pause))
            assertEquals(AutoPlayState.Paused, holder.state.value.demoState)
            assertEquals(AutoPlayEffect.AllNotesOff, output.effects.last())
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `demo catches up crossed notes before changing speed`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val clock = MutableGameClock()
        val output = RecordingAutoPlayOutput()
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            gameClock = clock,
            autoPlayOutput = output,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(HeHePianoAction.SetDemoMode(true))
            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.Play))
            clock.elapsed = 1.seconds
            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.SetSpeed(PlaybackSpeed(0.5))))

            assertEquals(
                listOf<AutoPlayEffect>(
                    AutoPlayEffect.NoteOn(MidiNote(60), 96, 0),
                    AutoPlayEffect.NoteOff(MidiNote(60), 0),
                    AutoPlayEffect.NoteOn(MidiNote(62), 96, 0),
                ),
                output.effects,
            )
            assertEquals(AutoPlayState.Playing, holder.state.value.demoState)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `left hand practice auto plays only the right hand`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val clock = MutableGameClock()
        val output = RecordingAutoPlayOutput()
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            gameClock = clock,
            autoPlayOutput = output,
        )

        try {
            holder.dispatch(HeHePianoAction.SetPracticeMode(PracticeMode.LeftHand))
            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.Play))
            assertTrue(output.effects.isEmpty())

            clock.elapsed = 3.seconds
            holder.dispatch(HeHePianoAction.FrameAdvanced)

            assertTrue(output.effects.contains(AutoPlayEffect.NoteOn(MidiNote(65), 96, 0)))
            assertEquals(PracticeMode.LeftHand, holder.state.value.practiceMode)
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `both hands practice does not start computer accompaniment`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val output = RecordingAutoPlayOutput()
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            autoPlayOutput = output,
        )

        try {
            holder.dispatch(HeHePianoAction.SetPracticeMode(PracticeMode.LeftHand))
            holder.dispatch(HeHePianoAction.SetPracticeMode(PracticeMode.BothHands))
            holder.dispatch(HeHePianoAction.Playback(PlaybackAction.Play))

            assertTrue(output.effects.isEmpty())
        } finally {
            holder.close()
            scope.cancel()
        }
    }

    @Test
    fun `MIDI analysis and import are state owner actions`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(HeHePianoAction.AnalyzeMidi(SelectedMidiFile("scale.mid", scaleMidi())))

            val analysis = assertIs<MidiImportState.Ready>(holder.state.value.midiImport).analysis
            assertEquals("scale", analysis.songName)
            assertEquals(1, analysis.noteCount)
            assertEquals(TrackHand.Right, holder.state.value.trackMappings[0])

            holder.dispatch(HeHePianoAction.UpdateTrackMapping(0, TrackHand.Left))
            holder.dispatch(HeHePianoAction.ImportAnalyzedMidi)

            assertEquals(com.ruxor.hehepiano.core.song.PianoHand.Left, holder.state.value.song.notes.first().hand)

            holder.dispatch(HeHePianoAction.AnalyzeMidi(SelectedMidiFile("scale.mid", scaleMidi())))
            holder.dispatch(HeHePianoAction.UpdateTrackMapping(0, TrackHand.Ignore))
            holder.dispatch(HeHePianoAction.ImportAnalyzedMidi)

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
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            backgroundDispatcher = Dispatchers.Unconfined,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(
                HeHePianoAction.LoadMidiFile(
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
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            localSongSource = source,
            backgroundDispatcher = Dispatchers.Unconfined,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            assertEquals(listOf(source.file), holder.state.value.localSongs)
            assertFalse(holder.state.value.localLibraryLoading)

            holder.dispatch(HeHePianoAction.ShowLibrary)
            holder.dispatch(HeHePianoAction.OpenLocalSong(source.file))

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
        val holder = HeHePianoStateHolder(
            songRepository = DemoSongRepository(),
            parentScope = scope,
            analysisDispatcher = Dispatchers.Unconfined,
        )

        try {
            holder.dispatch(HeHePianoAction.AnalyzeMidi(SelectedMidiFile("empty.mid", byteArrayOf())))

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

private class MutableGameClock : GameClock {
    var elapsed: Duration = Duration.ZERO

    override fun now(): GameTime = GameTime(elapsed)
}

private class RecordingAutoPlayOutput : AutoPlayOutput {
    val effects = mutableListOf<AutoPlayEffect>()
    var stopCount = 0

    override fun submit(effects: List<AutoPlayEffect>) {
        this.effects += effects
    }

    override fun stop() {
        stopCount += 1
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
