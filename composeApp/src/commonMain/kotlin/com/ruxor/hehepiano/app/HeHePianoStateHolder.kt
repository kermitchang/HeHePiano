package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.core.song.Song
import com.ruxor.hehepiano.core.song.PianoHand
import com.ruxor.hehepiano.core.song.SongRepository
import com.ruxor.hehepiano.core.timeline.GameClock
import com.ruxor.hehepiano.core.timeline.MonotonicGameClock
import com.ruxor.hehepiano.core.timeline.SongLoop
import com.ruxor.hehepiano.core.timeline.SongTime
import com.ruxor.hehepiano.core.timeline.PlaybackState
import com.ruxor.hehepiano.feature.autoplay.AutoPlayOutput
import com.ruxor.hehepiano.feature.autoplay.AutoPlayScheduler
import com.ruxor.hehepiano.feature.autoplay.AutoPlayState
import com.ruxor.hehepiano.feature.autoplay.NoAutoPlayOutput
import com.ruxor.hehepiano.feature.midi.MidiAnalyzer
import com.ruxor.hehepiano.feature.midi.MidiImportPolicy
import com.ruxor.hehepiano.feature.midi.MidiFileSelection
import com.ruxor.hehepiano.feature.midi.SelectedMidiFile
import com.ruxor.hehepiano.feature.midi.StandardMidiParser
import com.ruxor.hehepiano.feature.playback.PlaybackAction
import com.ruxor.hehepiano.feature.playback.PlaybackController
import com.ruxor.hehepiano.feature.practice.PracticeMode
import com.ruxor.hehepiano.feature.songlibrary.LoadableSongSource
import com.ruxor.hehepiano.feature.songlibrary.SongFile
import com.ruxor.hehepiano.feature.songlibrary.SongSource
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

internal class HeHePianoStateHolder(
    songRepository: SongRepository,
    parentScope: CoroutineScope,
    private val localSongSource: SongSource? = null,
    private val gameClock: GameClock = MonotonicGameClock(),
    private val midiParser: StandardMidiParser = StandardMidiParser(),
    private val midiAnalyzer: MidiAnalyzer = MidiAnalyzer(),
    private val autoPlayOutput: AutoPlayOutput = NoAutoPlayOutput,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val workScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]),
    )
    private val songs = songRepository.availableSongs().toList().also { available ->
        require(available.isNotEmpty()) { "HeHePiano requires at least one song." }
    }
    private var playbackController = createPlaybackController(songs.first())
    private val autoPlayScheduler = AutoPlayScheduler()
    private var importJob: Job? = null
    private val mutableState = MutableStateFlow(
        HeHePianoUiState(
            librarySongs = songs,
            song = songs.first(),
            playbackState = playbackController.state.value,
        ),
    )

    val state: StateFlow<HeHePianoUiState> = mutableState.asStateFlow()

    init {
        autoPlayScheduler.load(songs.first())
        if (localSongSource != null) dispatch(HeHePianoAction.RefreshLocalSongs)
    }

    fun dispatch(action: HeHePianoAction) {
        when (action) {
            HeHePianoAction.FrameAdvanced -> advanceFrame()
            is HeHePianoAction.Playback -> dispatchPlayback(action.action)
            HeHePianoAction.SelectNextSong -> selectNextSong()
            is HeHePianoAction.SetViewportMode -> update { it.copy(viewportMode = action.mode) }
            HeHePianoAction.ToggleDebug -> update { it.copy(debugVisible = !it.debugVisible) }
            HeHePianoAction.ShowLibrary -> update { it.copy(libraryVisible = true) }
            HeHePianoAction.HideLibrary -> update { it.copy(libraryVisible = false) }
            is HeHePianoAction.SetPlayerSoundEnabled -> update { it.copy(playerSoundEnabled = action.enabled) }
            is HeHePianoAction.SetDemoMode -> setDemoMode(action.enabled)
            is HeHePianoAction.SetPracticeMode -> setPracticeMode(action.mode)
            is HeHePianoAction.AnalyzeMidi -> analyzeMidi(action.file)
            is HeHePianoAction.LoadMidiFile -> loadMidiFile(action.selection)
            is HeHePianoAction.UpdateTrackMapping -> update { current ->
                current.copy(trackMappings = current.trackMappings + (action.trackIndex to action.hand))
            }
            HeHePianoAction.CancelMidiAnalysis -> {
                importJob?.cancel()
                importJob = null
                update {
                    it.copy(midiImport = MidiImportState.Idle, trackMappings = emptyMap(), errorMessage = null)
                }
            }
            HeHePianoAction.ImportAnalyzedMidi -> importAnalyzedMidi()
            HeHePianoAction.RefreshLocalSongs -> refreshLocalSongs()
            is HeHePianoAction.OpenLocalSong -> openLocalSong(action.file)
        }
    }

    fun close() {
        importJob = null
        autoPlayOutput.stop()
        workScope.cancel()
    }

    private fun advanceFrame() {
        val previousState = state.value
        playbackController.onFrame()
        var demoState = previousState.demoState
        var playbackState = playbackController.state.value
        if (shouldAutoPlay(previousState) && previousState.playbackState.playbackState == PlaybackState.Playing) {
            val result = advanceAutoPlay(previousState.playbackState.songTime, playbackState.songTime)
            if (result.completed) {
                playbackController.dispatch(PlaybackAction.Pause)
                playbackController.dispatch(PlaybackAction.Restart)
                playbackState = playbackController.state.value
                demoState = if (previousState.demoModeEnabled) AutoPlayState.Completed else AutoPlayState.Off
            }
        }
        update { it.copy(playbackState = playbackState, demoState = demoState) }
    }

    private fun dispatchPlayback(action: PlaybackAction) {
        val before = state.value
        playbackController.dispatch(action)
        val after = playbackController.state.value
        var demoState = before.demoState
        var playbackState = after
        when (action) {
            PlaybackAction.Play -> {
                if (
                    shouldAutoPlay(before) &&
                    before.playbackState.playbackState != PlaybackState.Playing &&
                    after.playbackState == PlaybackState.Playing
                ) {
                    autoPlayOutput.submit(autoPlayScheduler.startAt(after.songTime))
                }
                if (before.demoModeEnabled) {
                    demoState = AutoPlayState.Playing
                }
            }
            PlaybackAction.Pause -> {
                autoPlayOutput.submit(autoPlayScheduler.pause())
                if (before.demoModeEnabled) {
                    demoState = AutoPlayState.Paused
                }
            }
            PlaybackAction.Restart -> {
                autoPlayOutput.submit(autoPlayScheduler.stop())
                if (shouldAutoPlay(before) && after.playbackState == PlaybackState.Playing) {
                    autoPlayOutput.submit(autoPlayScheduler.startAt(after.songTime))
                    if (before.demoModeEnabled) {
                        demoState = AutoPlayState.Playing
                    }
                } else if (before.demoModeEnabled) {
                    demoState = AutoPlayState.Ready
                }
            }
            is PlaybackAction.SetSpeed -> {
                if (shouldAutoPlay(before) && before.playbackState.playbackState == PlaybackState.Playing) {
                    val result = advanceAutoPlay(before.playbackState.songTime, after.songTime)
                    if (result.completed) {
                        playbackController.dispatch(PlaybackAction.Pause)
                        playbackController.dispatch(PlaybackAction.Restart)
                        playbackState = playbackController.state.value
                        if (before.demoModeEnabled) {
                            demoState = AutoPlayState.Completed
                        }
                    }
                }
            }
        }
        update { it.copy(playbackState = playbackState, demoState = demoState) }
    }

    private fun advanceAutoPlay(previous: SongTime, current: SongTime) =
        autoPlayScheduler.advance(previous, current).also { result -> autoPlayOutput.submit(result.effects) }

    private fun setDemoMode(enabled: Boolean) {
        val current = state.value
        if (enabled == current.demoModeEnabled) return
        autoPlayOutput.submit(autoPlayScheduler.stop())
        val nextState = current.copy(demoModeEnabled = enabled)
        autoPlayScheduler.load(current.song, computerHands(nextState))
        val isPlaying = current.playbackState.playbackState == PlaybackState.Playing
        if (isPlaying && computerHands(nextState).isNotEmpty()) {
            autoPlayOutput.submit(autoPlayScheduler.startAt(current.playbackState.songTime))
        }
        update {
            it.copy(
                demoModeEnabled = enabled,
                demoState = when {
                    !enabled -> AutoPlayState.Off
                    isPlaying -> AutoPlayState.Playing
                    else -> AutoPlayState.Ready
                },
            )
        }
    }

    private fun setPracticeMode(mode: PracticeMode) {
        val current = state.value
        if (current.practiceMode == mode) return

        autoPlayOutput.submit(autoPlayScheduler.stop())
        val nextState = current.copy(practiceMode = mode)
        autoPlayScheduler.load(current.song, computerHands(nextState))
        if (current.playbackState.playbackState == PlaybackState.Playing && computerHands(nextState).isNotEmpty()) {
            autoPlayOutput.submit(autoPlayScheduler.startAt(current.playbackState.songTime))
        }
        update { it.copy(practiceMode = mode) }
    }

    private fun selectNextSong() {
        importJob?.cancel()
        importJob = null
        autoPlayOutput.stop()
        val current = state.value
        val index = current.librarySongs.indexOfFirst { it.id == current.song.id }
        val nextSong = current.librarySongs[(index + 1).mod(current.librarySongs.size)]
        playbackController = createPlaybackController(nextSong)
        autoPlayScheduler.load(nextSong, computerHands(current))
        update {
            it.copy(
                song = nextSong,
                playbackState = playbackController.state.value,
                demoState = if (it.demoModeEnabled) AutoPlayState.Ready else AutoPlayState.Off,
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
        autoPlayOutput.submit(autoPlayScheduler.stop())
        update {
            it.copy(
                midiImport = MidiImportState.Analyzing(fileName),
                trackMappings = emptyMap(),
                practiceMode = PracticeMode.BothHands,
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
        autoPlayOutput.stop()
        playbackController = createPlaybackController(playable.song)
        autoPlayScheduler.load(playable.song, computerHands(current))
        update {
            it.copy(
                librarySongs = it.librarySongs.filterNot { song -> song.id == playable.song.id } + playable.song,
                song = playable.song,
                playbackState = playbackController.state.value,
                demoState = if (it.demoModeEnabled) AutoPlayState.Ready else AutoPlayState.Off,
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

    private fun shouldAutoPlay(state: HeHePianoUiState): Boolean = computerHands(state).isNotEmpty()

    private fun computerHands(state: HeHePianoUiState): Set<PianoHand> =
        if (state.demoModeEnabled) PianoHand.entries.toSet() else state.practiceMode.computerHands

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

    private inline fun update(transform: (HeHePianoUiState) -> HeHePianoUiState) {
        mutableState.update(transform)
    }
}
