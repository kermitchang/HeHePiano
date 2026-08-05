package com.ruxor.kermitpiano.app

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
import com.ruxor.kermitpiano.core.playerinput.PlayerInputSource
import com.ruxor.kermitpiano.feature.audio.AudioStartupInfo
import com.ruxor.kermitpiano.feature.audio.NoAudioEngine
import com.ruxor.kermitpiano.feature.audio.PianoAudioConfig
import com.ruxor.kermitpiano.feature.audio.PianoAudioEngine
import com.ruxor.kermitpiano.feature.audio.PlayerInputAudioRouter
import com.ruxor.kermitpiano.feature.audio.playTestC4
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardEventType
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardInput
import com.ruxor.kermitpiano.feature.midi.MidiFileSelection
import com.ruxor.kermitpiano.feature.midi.MidiInput
import com.ruxor.kermitpiano.feature.pianolayout.PianoDeviceProfile
import com.ruxor.kermitpiano.feature.pianolayout.PianoLayout
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewport
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode
import com.ruxor.kermitpiano.feature.playback.PlaybackAction
import com.ruxor.kermitpiano.feature.song.DemoSongRepository
import com.ruxor.kermitpiano.feature.songlibrary.SongSource
import com.ruxor.kermitpiano.feature.virtualpiano.VirtualPiano
import com.ruxor.kermitpiano.feature.waterfallrenderer.WaterfallRenderer
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun KermitPianoApp(
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
    val stateHolder = remember(songRepository, localSongSource, coroutineScope) {
        KermitPianoStateHolder(
            songRepository = songRepository,
            parentScope = coroutineScope,
            localSongSource = localSongSource,
        )
    }
    val appState by stateHolder.state.collectAsState()
    val playerInputState by keyboardInput.playerInputTracker.state.collectAsState()
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
        playerAudioRouter.enabled = appState.playerSoundEnabled
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
                keyboardInput.playerInputTracker.noteOn(PlayerInputSource.UsbMidi, note)
                playerAudioRouter.noteOn(note.value, velocity = velocity)
            },
            onNoteOff = { note ->
                keyboardInput.playerInputTracker.noteOff(PlayerInputSource.UsbMidi, note)
                playerAudioRouter.noteOff(note.value)
            },
            onPitchBend = { value -> playerAudioRouter.pitchBend(value) },
            onControlChange = { controller, value -> playerAudioRouter.controlChange(controller, value) },
        )

        onDispose {
            keyboardInput.releaseAll()
            keyboardInput.clearEventListener()
            midiInput?.stop()
            keyboardInput.playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
        }
    }

    LaunchedEffect(stateHolder) {
        while (true) {
            withFrameNanos {
                stateHolder.dispatch(KermitPianoAction.FrameAdvanced)
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
                            keyboardInput.playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
                            playerAudioRouter.onRestart()
                        }
                        stateHolder.dispatch(KermitPianoAction.Playback(action))
                    },
                    viewportMode = appState.viewportMode,
                    onViewportModeChanged = { stateHolder.dispatch(KermitPianoAction.SetViewportMode(it)) },
                    debugVisible = appState.debugVisible,
                    onDebugChanged = { stateHolder.dispatch(KermitPianoAction.ToggleDebug) },
                    onOpenMidi = {
                        requestMidiImport(openMidiFile) { selected ->
                            stateHolder.dispatch(KermitPianoAction.LoadMidiFile(selected))
                        }
                    },
                    songTitle = appState.song.title,
                    onSelectNextSong = {
                        keyboardInput.releaseAll()
                        keyboardInput.playerInputTracker.releaseAll(PlayerInputSource.UsbMidi)
                        playerAudioRouter.onSongChanged()
                        stateHolder.dispatch(KermitPianoAction.SelectNextSong)
                    },
                    audioState = audioState,
                    playerSoundEnabled = appState.playerSoundEnabled,
                    onPlayerSoundChanged = { stateHolder.dispatch(KermitPianoAction.SetPlayerSoundEnabled(it)) },
                    onLibrary = { stateHolder.dispatch(KermitPianoAction.ShowLibrary) },
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
                            onMappingChanged = { index, hand ->
                                stateHolder.dispatch(KermitPianoAction.UpdateTrackMapping(index, hand))
                            },
                            onCancel = { stateHolder.dispatch(KermitPianoAction.CancelMidiAnalysis) },
                            onImport = { stateHolder.dispatch(KermitPianoAction.ImportAnalyzedMidi) },
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
                                OutlinedButton(onClick = { stateHolder.dispatch(KermitPianoAction.CancelMidiAnalysis) }) {
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
                            onRefresh = { stateHolder.dispatch(KermitPianoAction.RefreshLocalSongs) },
                            onOpen = { stateHolder.dispatch(KermitPianoAction.OpenLocalSong(it)) },
                            onClose = { stateHolder.dispatch(KermitPianoAction.HideLibrary) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}
