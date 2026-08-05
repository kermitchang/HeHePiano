package com.ruxor.hehepiano.app

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruxor.hehepiano.core.playerinput.PlayerInputSource
import com.ruxor.hehepiano.feature.audio.AudioEngineState
import com.ruxor.hehepiano.feature.audio.AudioStartupInfo
import com.ruxor.hehepiano.feature.audio.NoAudioEngine
import com.ruxor.hehepiano.feature.audio.PianoAudioConfig
import com.ruxor.hehepiano.feature.audio.PianoAudioEngine
import com.ruxor.hehepiano.feature.audio.PlayerInputAudioRouter
import com.ruxor.hehepiano.feature.audio.playTestC4
import com.ruxor.hehepiano.feature.keyboardinput.KeyboardEventType
import com.ruxor.hehepiano.feature.keyboardinput.KeyboardInput
import com.ruxor.hehepiano.feature.midi.MidiFileSelection
import com.ruxor.hehepiano.feature.midi.MidiInput
import com.ruxor.hehepiano.feature.pianolayout.PianoDeviceProfile
import com.ruxor.hehepiano.feature.pianolayout.PianoLayout
import com.ruxor.hehepiano.feature.pianolayout.PianoViewport
import com.ruxor.hehepiano.feature.pianolayout.PianoViewportMode
import com.ruxor.hehepiano.feature.playback.PlaybackAction
import com.ruxor.hehepiano.feature.song.DemoSongRepository
import com.ruxor.hehepiano.feature.songlibrary.SongSource
import com.ruxor.hehepiano.feature.virtualpiano.VirtualPiano
import com.ruxor.hehepiano.feature.waterfallrenderer.WaterfallRenderer
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HeHePianoApp(
    keyboardInput: KeyboardInput,
    openMidiFile: () -> MidiFileSelection? = { null },
    localSongSource: SongSource? = null,
    audioEngine: PianoAudioEngine = NoAudioEngine(),
    audioConfig: PianoAudioConfig = PianoAudioConfig(),
    audioStartupInfo: AudioStartupInfo = AudioStartupInfo("", null, null, emptyList(), null, null),
    midiInput: MidiInput? = null,
) {
    val keyboardInputState by keyboardInput.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val songRepository = remember { DemoSongRepository() }
    val playerInputTracker = keyboardInput.playerInputTracker
    val autoPlayOutput = remember(audioEngine, playerInputTracker) {
        PianoAutoPlayOutput(audioEngine, playerInputTracker)
    }
    val stateHolder = remember(songRepository, localSongSource, coroutineScope, autoPlayOutput) {
        HeHePianoStateHolder(
            songRepository = songRepository,
            parentScope = coroutineScope,
            localSongSource = localSongSource,
            autoPlayOutput = autoPlayOutput,
        )
    }
    val appState by stateHolder.state.collectAsState()
    val playerInputState by playerInputTracker.state.collectAsState()
    val playerAudioRouter = remember(audioEngine) { PlayerInputAudioRouter(audioEngine) }
    val audioState by audioEngine.state.collectAsState()
    val audioDiagnostics by audioEngine.diagnostics.collectAsState()
    val followTarget = remember(appState.song, appState.playbackState.songTime) {
        followSongViewport(appState.song, appState.playbackState.songTime)
    }
    val followFirst by animateIntAsState(followTarget.firstVisibleMidi, tween(450))
    val followLast by animateIntAsState(followTarget.lastVisibleMidi, tween(450))
    val viewport = when (appState.viewportMode) {
        PianoViewportMode.Practice -> PianoViewport.practice(PianoDeviceProfile.ak490, keyboardInputState.octave - 4)
        PianoViewportMode.Full88 -> PianoViewport.full88
        PianoViewportMode.FollowSong -> PianoViewport(followFirst, followLast, PianoViewportMode.FollowSong)
    }

    DisposableEffect(stateHolder) {
        onDispose { stateHolder.close() }
    }

    SideEffect {
        keyboardInput.enabled = !appState.demoModeEnabled
        playerAudioRouter.enabled = appState.playerSoundEnabled
        playerAudioRouter.inputSuspended = appState.demoModeEnabled
    }

    LaunchedEffect(audioEngine, audioConfig) {
        try {
            audioEngine.initialize(audioConfig)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                audioEngine.close()
            }
        }
    }

    DisposableEffect(key1 = keyboardInput, key2 = midiInput, key3 = playerAudioRouter) {
        keyboardInput.setEventListener { event ->
            when (event.type) {
                KeyboardEventType.KeyDown -> playerAudioRouter.noteOn(event.note.value, velocity = 96)
                KeyboardEventType.KeyUp -> playerAudioRouter.noteOff(event.note.value)
            }
        }
        midiInput?.start(
            onNoteOn = { note, velocity ->
                if (playerAudioRouter.inputSuspended) return@start
                playerInputTracker.noteOn(PlayerInputSource.UsbMidi, note)
                playerAudioRouter.noteOn(note.value, velocity = velocity)
            },
            onNoteOff = { note ->
                if (playerAudioRouter.inputSuspended) return@start
                playerInputTracker.noteOff(PlayerInputSource.UsbMidi, note)
                playerAudioRouter.noteOff(note.value)
            },
            onPitchBend = { value -> playerAudioRouter.pitchBend(value) },
            onControlChange = { controller, value -> playerAudioRouter.controlChange(controller, value) },
        )

        onDispose {
            keyboardInput.releaseAll()
            keyboardInput.clearEventListener()
            midiInput?.stop()
            playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
            autoPlayOutput.stop()
        }
    }

    LaunchedEffect(stateHolder) {
        while (true) {
            withFrameNanos {
                stateHolder.dispatch(HeHePianoAction.FrameAdvanced)
            }
        }
    }

    MaterialTheme(colorScheme = kermitDarkColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                PianoTopBar(
                    playbackState = appState.playbackState,
                    onPlaybackAction = { action ->
                        if (action == PlaybackAction.Restart) {
                            keyboardInput.releaseAll()
                            playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
                            playerAudioRouter.onRestart()
                        }
                        stateHolder.dispatch(HeHePianoAction.Playback(action))
                    },
                    viewportMode = appState.viewportMode,
                    onViewportModeChanged = { stateHolder.dispatch(HeHePianoAction.SetViewportMode(it)) },
                    debugVisible = appState.debugVisible,
                    onDebugChanged = { stateHolder.dispatch(HeHePianoAction.ToggleDebug) },
                    demoModeEnabled = appState.demoModeEnabled,
                    demoState = appState.demoState,
                    audioReady = audioState is AudioEngineState.Ready,
                    onDemoModeChanged = { enabled ->
                        if (enabled) {
                            keyboardInput.releaseAll()
                            playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
                        }
                        playerAudioRouter.inputSuspended = enabled
                        stateHolder.dispatch(HeHePianoAction.SetDemoMode(enabled))
                    },
                    onOpenMidi = {
                        requestMidiImport(openMidiFile) { selected ->
                            stateHolder.dispatch(HeHePianoAction.LoadMidiFile(selected))
                        }
                    },
                    songTitle = appState.song.title,
                    onSelectNextSong = {
                        keyboardInput.releaseAll()
                        playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
                        playerAudioRouter.onSongChanged()
                        stateHolder.dispatch(HeHePianoAction.SelectNextSong)
                    },
                    audioState = audioState,
                    playerSoundEnabled = appState.playerSoundEnabled,
                    onPlayerSoundChanged = { stateHolder.dispatch(HeHePianoAction.SetPlayerSoundEnabled(it)) },
                    onLibrary = { stateHolder.dispatch(HeHePianoAction.ShowLibrary) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val density = LocalDensity.current
                        val pianoHeight = maxHeight * 0.26f
                        val pianoLayout = remember(viewport, maxWidth, pianoHeight, density) {
                            PianoLayout.create(
                                viewport = viewport,
                                width = with(density) { maxWidth.toPx() },
                                height = with(density) { pianoHeight.toPx() },
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            WaterfallRenderer(
                                song = appState.song,
                                songTime = appState.playbackState.songTime,
                                pianoLayout = pianoLayout,
                                modifier = Modifier.weight(1f).fillMaxWidth().clip(workspaceShape),
                            )
                            VirtualPiano(
                                layout = pianoLayout,
                                pressedNotes = playerInputState.pressedNotes,
                                density = density,
                                viewportMode = appState.viewportMode,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (appState.debugVisible) SongInformationPanel(
                        appState.song,
                        appState.playbackState,
                        keyboardInputState,
                        viewport,
                        pianoLayoutKeyCount(viewport),
                        audioState,
                        audioStartupInfo,
                        audioDiagnostics,
                        onTestC4 = {
                            coroutineScope.launch { playTestC4(audioEngine) }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    )
                    when (val importState = appState.midiImport) {
                        is MidiImportState.Ready -> MidiAnalysisPanel(
                            analysis = importState.analysis,
                            mappings = appState.trackMappings,
                            practiceMode = appState.practiceMode,
                            onMappingChanged = { index, hand ->
                                stateHolder.dispatch(HeHePianoAction.UpdateTrackMapping(index, hand))
                            },
                            onPracticeModeChanged = { mode ->
                                stateHolder.dispatch(HeHePianoAction.SetPracticeMode(mode))
                            },
                            onCancel = { stateHolder.dispatch(HeHePianoAction.CancelMidiAnalysis) },
                            onImport = { stateHolder.dispatch(HeHePianoAction.ImportAnalyzedMidi) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                        is MidiImportState.Analyzing -> Card(
                            modifier = Modifier.align(Alignment.Center),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Text("Analyzing ${importState.fileName}…", modifier = Modifier.padding(24.dp))
                        }
                        is MidiImportState.Failure -> Card(
                            modifier = Modifier.align(Alignment.Center),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Unable to import ${importState.fileName}", fontWeight = FontWeight.Bold)
                                Text(importState.message, color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = { stateHolder.dispatch(HeHePianoAction.CancelMidiAnalysis) }) {
                                    Text("Close")
                                }
                            }
                        }
                        MidiImportState.Idle -> Unit
                    }
                    if (appState.libraryVisible) {
                        LocalSongLibraryPanel(
                            songs = appState.localSongs,
                            loading = appState.localLibraryLoading,
                            errorMessage = appState.errorMessage,
                            onRefresh = { stateHolder.dispatch(HeHePianoAction.RefreshLocalSongs) },
                            onOpen = { stateHolder.dispatch(HeHePianoAction.OpenLocalSong(it)) },
                            onClose = { stateHolder.dispatch(HeHePianoAction.HideLibrary) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}
