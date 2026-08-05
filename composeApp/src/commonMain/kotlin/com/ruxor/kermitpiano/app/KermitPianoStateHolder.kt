package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongRepository
import com.ruxor.kermitpiano.core.timeline.GameClock
import com.ruxor.kermitpiano.core.timeline.MonotonicGameClock
import com.ruxor.kermitpiano.core.timeline.SongLoop
import com.ruxor.kermitpiano.core.timeline.SongTime
import com.ruxor.kermitpiano.feature.midi.MidiAnalyzer
import com.ruxor.kermitpiano.feature.midi.MidiImportPolicy
import com.ruxor.kermitpiano.feature.midi.MidiFileSelection
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import com.ruxor.kermitpiano.feature.midi.StandardMidiParser
import com.ruxor.kermitpiano.feature.playback.PlaybackAction
import com.ruxor.kermitpiano.feature.playback.PlaybackController
import com.ruxor.kermitpiano.feature.songlibrary.LoadableSongSource
import com.ruxor.kermitpiano.feature.songlibrary.SongFile
import com.ruxor.kermitpiano.feature.songlibrary.SongSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class KermitPianoStateHolder(
    songRepository: SongRepository,
    parentScope: CoroutineScope,
    private val localSongSource: SongSource? = null,
    private val gameClock: GameClock = MonotonicGameClock(),
    private val midiParser: StandardMidiParser = StandardMidiParser(),
    private val midiAnalyzer: MidiAnalyzer = MidiAnalyzer(),
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val workScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]),
    )
    private val songs = songRepository.availableSongs().toList().also { available ->
        require(available.isNotEmpty()) { "KermitPiano requires at least one song." }
    }
    private var playbackController = createPlaybackController(songs.first())
    private var importJob: Job? = null
    private val mutableState = MutableStateFlow(
        KermitPianoUiState(
            librarySongs = songs,
            song = songs.first(),
            playbackState = playbackController.state.value,
        ),
    )

    val state: StateFlow<KermitPianoUiState> = mutableState.asStateFlow()

    init {
        if (localSongSource != null) dispatch(KermitPianoAction.RefreshLocalSongs)
    }

    fun dispatch(action: KermitPianoAction) {
        when (action) {
            KermitPianoAction.FrameAdvanced -> advanceFrame()
            is KermitPianoAction.Playback -> dispatchPlayback(action.action)
            KermitPianoAction.SelectNextSong -> selectNextSong()
            is KermitPianoAction.SetViewportMode -> update { it.copy(viewportMode = action.mode) }
            KermitPianoAction.ToggleDebug -> update { it.copy(debugVisible = !it.debugVisible) }
            KermitPianoAction.ShowLibrary -> update { it.copy(libraryVisible = true) }
            KermitPianoAction.HideLibrary -> update { it.copy(libraryVisible = false) }
            is KermitPianoAction.SetPlayerSoundEnabled -> update { it.copy(playerSoundEnabled = action.enabled) }
            is KermitPianoAction.AnalyzeMidi -> analyzeMidi(action.file)
            is KermitPianoAction.LoadMidiFile -> loadMidiFile(action.selection)
            is KermitPianoAction.UpdateTrackMapping -> update { current ->
                current.copy(trackMappings = current.trackMappings + (action.trackIndex to action.hand))
            }
            KermitPianoAction.CancelMidiAnalysis -> {
                importJob?.cancel()
                importJob = null
                update {
                    it.copy(midiImport = MidiImportState.Idle, trackMappings = emptyMap(), errorMessage = null)
                }
            }
            KermitPianoAction.ImportAnalyzedMidi -> importAnalyzedMidi()
            KermitPianoAction.RefreshLocalSongs -> refreshLocalSongs()
            is KermitPianoAction.OpenLocalSong -> openLocalSong(action.file)
        }
    }

    fun close() {
        importJob = null
        workScope.cancel()
    }

    private fun advanceFrame() {
        playbackController.onFrame()
        update { it.copy(playbackState = playbackController.state.value) }
    }

    private fun dispatchPlayback(action: PlaybackAction) {
        playbackController.dispatch(action)
        update { it.copy(playbackState = playbackController.state.value) }
    }

    private fun selectNextSong() {
        importJob?.cancel()
        importJob = null
        val current = state.value
        val index = current.librarySongs.indexOfFirst { it.id == current.song.id }
        val nextSong = current.librarySongs[(index + 1).mod(current.librarySongs.size)]
        playbackController = createPlaybackController(nextSong)
        update {
            it.copy(
                song = nextSong,
                playbackState = playbackController.state.value,
                midiImport = MidiImportState.Idle,
                trackMappings = emptyMap(),
                errorMessage = null,
            )
        }
    }

    private fun analyzeMidi(file: SelectedMidiFile) {
        markAnalyzing(file.name)
        launchImport(file.name) {
            analyzeAndPublish(file)
        }
    }

    private fun loadMidiFile(selection: MidiFileSelection) {
        markAnalyzing(selection.name)
        launchImport(selection.name) {
            val file = withContext(backgroundDispatcher) { selection.load() }
            analyzeAndPublish(file)
        }
    }

    private fun markAnalyzing(fileName: String) {
        update {
            it.copy(
                midiImport = MidiImportState.Analyzing(fileName),
                trackMappings = emptyMap(),
                errorMessage = null,
            )
        }
    }

    private suspend fun analyzeAndPublish(file: SelectedMidiFile) {
        MidiImportPolicy.validationError(file)?.let { message ->
            throw IllegalArgumentException(message)
        }
        val analysis = withContext(analysisDispatcher) {
            midiAnalyzer.analyze(file.name, midiParser.parse(file.bytes))
        }
        currentCoroutineContext().ensureActive()
        update {
            it.copy(
                midiImport = MidiImportState.Ready(analysis),
                trackMappings = analysis.tracks.associate { track -> track.index to track.suggestedHand },
                errorMessage = null,
            )
        }
    }

    private fun importAnalyzedMidi() {
        val current = state.value
        val analysis = (current.midiImport as? MidiImportState.Ready)?.analysis ?: return
        val playable = midiAnalyzer.import(analysis, current.trackMappings)
        playbackController = createPlaybackController(playable.song)
        update {
            it.copy(
                librarySongs = it.librarySongs.filterNot { song -> song.id == playable.song.id } + playable.song,
                song = playable.song,
                playbackState = playbackController.state.value,
                midiImport = MidiImportState.Idle,
                trackMappings = emptyMap(),
                errorMessage = null,
            )
        }
    }

    private fun refreshLocalSongs() {
        val source = localSongSource ?: return
        update { it.copy(localLibraryLoading = true, errorMessage = null) }
        launchWork {
            val songs = withContext(backgroundDispatcher) { source.listSongs().toList() }
            update { it.copy(localSongs = songs, localLibraryLoading = false, errorMessage = null) }
        }
    }

    private fun openLocalSong(file: SongFile) {
        val source = localSongSource as? LoadableSongSource ?: return
        markAnalyzing(file.name)
        update {
            it.copy(
                localLibraryLoading = true,
                errorMessage = null,
            )
        }
        launchImport(file.name) {
            val selected = withContext(backgroundDispatcher) { source.load(file) }
            analyzeAndPublish(selected)
            update {
                it.copy(
                    libraryVisible = false,
                    localLibraryLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    private fun createPlaybackController(song: Song): PlaybackController = PlaybackController(
        gameClock = gameClock,
        loop = SongLoop(start = SongTime.zero, endExclusive = song.duration),
    )

    private fun launchImport(fileName: String, block: suspend () -> Unit) {
        importJob?.cancel()
        importJob = launchWork(fileName, block)
    }

    private fun launchWork(fileName: String? = null, block: suspend () -> Unit): Job = workScope.launch {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            update {
                it.copy(
                    localLibraryLoading = false,
                    midiImport = fileName?.let { name ->
                        MidiImportState.Failure(name, exception.message ?: "MIDI import failed.")
                    } ?: it.midiImport,
                    errorMessage = exception.message ?: "Operation failed.",
                )
            }
        }
    }

    private inline fun update(transform: (KermitPianoUiState) -> KermitPianoUiState) {
        mutableState.update(transform)
    }
}
