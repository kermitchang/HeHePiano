package com.ruxor.kermitpiano.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongRepository
import com.ruxor.kermitpiano.core.timeline.MonotonicGameClock
import com.ruxor.kermitpiano.core.timeline.PlaybackState
import com.ruxor.kermitpiano.core.timeline.SongLoop
import com.ruxor.kermitpiano.core.timeline.SongTime
import com.ruxor.kermitpiano.core.timeline.TimelineSnapshot
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardInputState
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardInput
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardEventType
import com.ruxor.kermitpiano.feature.audio.AudioEngineState
import com.ruxor.kermitpiano.feature.audio.AudioEngineDiagnostics
import com.ruxor.kermitpiano.feature.audio.AudioStartupInfo
import com.ruxor.kermitpiano.feature.audio.NoAudioEngine
import com.ruxor.kermitpiano.feature.audio.PianoAudioConfig
import com.ruxor.kermitpiano.feature.audio.PianoAudioEngine
import com.ruxor.kermitpiano.feature.audio.PlayerInputAudioRouter
import com.ruxor.kermitpiano.feature.audio.playTestC4
import com.ruxor.kermitpiano.feature.midi.MidiAnalysis
import com.ruxor.kermitpiano.feature.midi.MidiAnalyzer
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import com.ruxor.kermitpiano.feature.midi.StandardMidiParser
import com.ruxor.kermitpiano.feature.midi.TrackHand
import com.ruxor.kermitpiano.feature.gamevisual.GameVisualTokens
import com.ruxor.kermitpiano.feature.pianolayout.PianoLayout
import com.ruxor.kermitpiano.feature.pianolayout.PianoModel
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewport
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode
import com.ruxor.kermitpiano.feature.pianolayout.PianoDeviceProfile
import com.ruxor.kermitpiano.feature.playback.PlaybackController
import com.ruxor.kermitpiano.feature.playback.PlaybackAction
import com.ruxor.kermitpiano.feature.playback.PlaybackControls
import com.ruxor.kermitpiano.feature.song.DemoSongRepository
import com.ruxor.kermitpiano.feature.songlibrary.LoadableSongSource
import com.ruxor.kermitpiano.feature.songlibrary.SongFile
import com.ruxor.kermitpiano.feature.songlibrary.SongSource
import com.ruxor.kermitpiano.feature.virtualpiano.VirtualPiano
import com.ruxor.kermitpiano.feature.waterfallrenderer.WaterfallRenderer
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch

@Composable
internal fun KermitPianoApp(
    keyboardInput: KeyboardInput,
    openMidiFile: () -> SelectedMidiFile? = { null },
    localSongSource: SongSource? = null,
    audioEngine: PianoAudioEngine = NoAudioEngine(),
    audioConfig: PianoAudioConfig = PianoAudioConfig(),
    audioStartupInfo: AudioStartupInfo = AudioStartupInfo("", null, null, emptyList(), null, null),
) {
    val keyboardInputState by keyboardInput.state.collectAsState()
    val songRepository: SongRepository = remember { DemoSongRepository() }
    val coroutineScope = rememberCoroutineScope()
    val playerAudioRouter = remember(audioEngine) { PlayerInputAudioRouter(audioEngine) }
    var librarySongs by remember(songRepository) { mutableStateOf(songRepository.availableSongs()) }
    var song by remember(songRepository) { mutableStateOf(librarySongs.first()) }
    var analysis by remember { mutableStateOf<MidiAnalysis?>(null) }
    var trackMappings by remember { mutableStateOf<Map<Int, TrackHand>>(emptyMap()) }
    val gameClock = remember { MonotonicGameClock() }
    val playbackController = remember(song.id) {
        PlaybackController(
            gameClock = gameClock,
            loop = SongLoop(start = SongTime.zero, endExclusive = song.duration),
        )
    }
    val playbackState by playbackController.state.collectAsState()
    var viewportMode by remember { mutableStateOf(PianoViewportMode.Practice) }
    var debugVisible by remember { mutableStateOf(false) }
    var libraryVisible by remember { mutableStateOf(false) }
    var localSongs by remember { mutableStateOf<List<SongFile>>(emptyList()) }
    var playerSoundEnabled by remember { mutableStateOf(true) }
    val audioState by audioEngine.state.collectAsState()
    val audioDiagnostics by audioEngine.diagnostics.collectAsState()
    val followTarget = remember(song, playbackState.songTime) { followSongViewport(song, playbackState.songTime) }
    val followFirst by animateIntAsState(followTarget.firstVisibleMidi, tween(450))
    val followLast by animateIntAsState(followTarget.lastVisibleMidi, tween(450))
    val viewport = when (viewportMode) {
        PianoViewportMode.Practice -> PianoViewport.practice(PianoDeviceProfile.ak490, keyboardInputState.octave - 4)
        PianoViewportMode.Full88 -> PianoViewport.full88
        PianoViewportMode.FollowSong -> PianoViewport(followFirst, followLast, PianoViewportMode.FollowSong)
    }

    LaunchedEffect(localSongSource) {
        localSongs = localSongSource?.listSongs().orEmpty()
    }

    LaunchedEffect(audioEngine, audioConfig) {
        audioEngine.initialize(audioConfig)
    }

    DisposableEffect(audioEngine) {
        onDispose { coroutineScope.launch { audioEngine.close() } }
    }

    SideEffect {
        playerAudioRouter.enabled = playerSoundEnabled
        keyboardInput.setEventListener { event ->
            when (event.type) {
                KeyboardEventType.KeyDown -> playerAudioRouter.noteOn(event.note.value, velocity = 96)
                KeyboardEventType.KeyUp -> playerAudioRouter.noteOff(event.note.value)
            }
        }
    }

    LaunchedEffect(playbackController) {
        while (true) {
            withFrameNanos {
                playbackController.onFrame()
            }
        }
    }

    MaterialTheme(colorScheme = kermitDarkColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                PianoTopBar(
                    playbackState = playbackState,
                    onPlaybackAction = { action ->
                        if (action == PlaybackAction.Restart) playerAudioRouter.onRestart()
                        playbackController.dispatch(action)
                    },
                    viewportMode = viewportMode,
                    onViewportModeChanged = { viewportMode = it },
                    debugVisible = debugVisible,
                    onDebugChanged = { debugVisible = !debugVisible },
                    onOpenMidi = {
                        requestMidiImport(openMidiFile) { selected ->
                            analysis = MidiAnalyzer().analyze(selected.name, StandardMidiParser().parse(selected.bytes))
                            trackMappings = analysis!!.tracks.associate { it.index to it.suggestedHand }
                        }
                    },
                    songTitle = song.title,
                    onSelectNextSong = {
                        playerAudioRouter.onSongChanged()
                        val index = librarySongs.indexOfFirst { it.id == song.id }
                        song = librarySongs[(index + 1).mod(librarySongs.size)]
                    },
                    audioState = audioState,
                    playerSoundEnabled = playerSoundEnabled,
                    onPlayerSoundChanged = { playerSoundEnabled = it },
                    onLibrary = { libraryVisible = true },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
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
                                song = song,
                                songTime = playbackState.songTime,
                                pianoLayout = pianoLayout,
                                modifier = Modifier.weight(1f).fillMaxWidth().clip(workspaceShape),
                            )
                            VirtualPiano(
                                layout = pianoLayout,
                                pressedNotes = keyboardInputState.pressedNotes,
                                density = density,
                                viewportMode = viewportMode,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (debugVisible) SongInformationPanel(
                        song, playbackState, keyboardInputState, viewport, pianoLayoutKeyCount(viewport),
                        audioState, audioStartupInfo, audioDiagnostics,
                        onTestC4 = {
                            coroutineScope.launch {
                                playTestC4(audioEngine)
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    )
                    analysis?.let { midi ->
                        MidiAnalysisPanel(
                            analysis = midi,
                            mappings = trackMappings,
                            onMappingChanged = { index, hand -> trackMappings = trackMappings + (index to hand) },
                            onCancel = { analysis = null },
                            onImport = {
                                song = MidiAnalyzer().import(midi, trackMappings).song
                                librarySongs = librarySongs.filterNot { it.id == song.id } + song
                                analysis = null
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    if (libraryVisible) {
                        LocalSongLibraryPanel(
                            songs = localSongs,
                            onRefresh = {
                                coroutineScope.launch { localSongs = localSongSource?.listSongs().orEmpty() }
                            },
                            onOpen = { localFile ->
                                val source = localSongSource as? LoadableSongSource ?: return@LocalSongLibraryPanel
                                coroutineScope.launch {
                                    val selected = source.load(localFile)
                                    analysis = MidiAnalyzer().analyze(selected.name, StandardMidiParser().parse(selected.bytes))
                                    trackMappings = analysis!!.tracks.associate { it.index to it.suggestedHand }
                                    libraryVisible = false
                                }
                            },
                            onClose = { libraryVisible = false },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PianoTopBar(
    playbackState: TimelineSnapshot,
    onPlaybackAction: (PlaybackAction) -> Unit,
    viewportMode: PianoViewportMode,
    onViewportModeChanged: (PianoViewportMode) -> Unit,
    debugVisible: Boolean,
    onDebugChanged: () -> Unit,
    onOpenMidi: () -> Unit,
    songTitle: String,
    onSelectNextSong: () -> Unit,
    audioState: AudioEngineState,
    playerSoundEnabled: Boolean,
    onPlayerSoundChanged: (Boolean) -> Unit,
    onLibrary: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp)) {
            val policy = TopBarLayoutPolicy.modeFor(maxWidth.value.toInt())
            when (policy) {
                TopBarLayoutMode.Wide -> WideTopBar(
                    playbackState, onPlaybackAction, viewportMode, onViewportModeChanged,
                    debugVisible, onDebugChanged, onOpenMidi, songTitle, onSelectNextSong,
                    audioState, playerSoundEnabled, onPlayerSoundChanged, onLibrary,
                )
                TopBarLayoutMode.Compact -> CompactTopBar(
                    playbackState, onPlaybackAction, viewportMode, onViewportModeChanged,
                    debugVisible, onDebugChanged, onOpenMidi, songTitle, onSelectNextSong,
                    audioState, playerSoundEnabled, onPlayerSoundChanged, onLibrary,
                )
                TopBarLayoutMode.Narrow -> NarrowTopBar(
                    playbackState, onPlaybackAction, viewportMode, onViewportModeChanged,
                    debugVisible, onDebugChanged, onOpenMidi,
                    audioState, playerSoundEnabled, onPlayerSoundChanged, onLibrary,
                )
            }
        }
    }
}

@Composable
private fun WideTopBar(
    state: TimelineSnapshot,
    onAction: (PlaybackAction) -> Unit,
    viewport: PianoViewportMode,
    onViewportChanged: (PianoViewportMode) -> Unit,
    debugVisible: Boolean,
    onDebugChanged: () -> Unit,
    onOpenMidi: () -> Unit,
    songTitle: String,
    onSelectSong: () -> Unit,
    audioState: AudioEngineState,
    playerSoundEnabled: Boolean,
    onPlayerSoundChanged: (Boolean) -> Unit,
    onLibrary: () -> Unit,
) = TopBarRow {
    Brand(showName = true)
    SongControls(songTitle, onOpenMidi, onSelectSong, showSongTitle = true)
    PlaybackGroup(state, onAction, showRestart = true, compactSpeed = false)
    PianoViewControls(viewport, onViewportChanged)
    InputBadge()
    AudioStatusButton(audioState, playerSoundEnabled, onPlayerSoundChanged)
    OutlinedButton(onClick = onLibrary) { Text("Library") }
    OutlinedButton(onClick = onDebugChanged) { Text(if (debugVisible) "Hide Debug" else "Debug") }
}

@Composable
private fun CompactTopBar(
    state: TimelineSnapshot,
    onAction: (PlaybackAction) -> Unit,
    viewport: PianoViewportMode,
    onViewportChanged: (PianoViewportMode) -> Unit,
    debugVisible: Boolean,
    onDebugChanged: () -> Unit,
    onOpenMidi: () -> Unit,
    songTitle: String,
    onSelectSong: () -> Unit,
    audioState: AudioEngineState,
    playerSoundEnabled: Boolean,
    onPlayerSoundChanged: (Boolean) -> Unit,
    onLibrary: () -> Unit,
) = TopBarRow {
    Brand(showName = false)
    SongControls(songTitle, onOpenMidi, onSelectSong, showSongTitle = false)
    PlaybackGroup(state, onAction, showRestart = true, compactSpeed = true)
    PianoViewControls(viewport, onViewportChanged)
    AudioStatusButton(audioState, playerSoundEnabled, onPlayerSoundChanged)
    OutlinedButton(onClick = onLibrary) { Text("Library") }
    OutlinedButton(onClick = onDebugChanged) { Text("Debug") }
}

@Composable
private fun NarrowTopBar(
    state: TimelineSnapshot,
    onAction: (PlaybackAction) -> Unit,
    viewport: PianoViewportMode,
    onViewportChanged: (PianoViewportMode) -> Unit,
    debugVisible: Boolean,
    onDebugChanged: () -> Unit,
    onOpenMidi: () -> Unit,
    audioState: AudioEngineState,
    playerSoundEnabled: Boolean,
    onPlayerSoundChanged: (Boolean) -> Unit,
    onLibrary: () -> Unit,
) = TopBarRow {
    Text(text = "♬", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    Button(onClick = onOpenMidi) { Text("Open") }
    PlayPauseButton(state, onAction)
    ViewMenu(viewport, onViewportChanged)
    Spacer(Modifier.weight(1f))
    MoreMenu(state, onAction, debugVisible, onDebugChanged, audioState, playerSoundEnabled, onPlayerSoundChanged, onLibrary)
}

@Composable
private fun TopBarRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun Brand(showName: Boolean) {
    Text(text = "♬", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    if (showName) Text("KermitPiano", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun SongControls(
    songTitle: String,
    onOpenMidi: () -> Unit,
    onSelectSong: () -> Unit,
    showSongTitle: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onOpenMidi) { Text("Open MIDI") }
        if (showSongTitle) {
            OutlinedButton(onClick = onSelectSong, modifier = Modifier.widthIn(max = TopBarLayoutPolicy.songTitleLimit(TopBarLayoutMode.Wide).dp)) {
                Text(songTitle, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlaybackGroup(state: TimelineSnapshot, onAction: (PlaybackAction) -> Unit, showRestart: Boolean, compactSpeed: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayPauseButton(state, onAction)
        if (showRestart) OutlinedButton(onClick = { onAction(PlaybackAction.Restart) }) { Text("Restart") }
        if (compactSpeed) SpeedMenu(state, onAction) else SpeedSegmentedControl(state, onAction)
    }
}

@Composable
private fun SpeedSegmentedControl(state: TimelineSnapshot, onAction: (PlaybackAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(0.5, 0.75, 1.0, 1.25, 1.5).forEach { multiplier ->
            OutlinedButton(
                enabled = state.speed.multiplier != multiplier,
                onClick = { onAction(PlaybackAction.SetSpeed(com.ruxor.kermitpiano.core.timeline.PlaybackSpeed(multiplier))) },
            ) { Text("${(multiplier * 100).toInt()}%") }
        }
    }
}

@Composable
private fun PlayPauseButton(state: TimelineSnapshot, onAction: (PlaybackAction) -> Unit) {
    Button(onClick = { onAction(if (state.playbackState == PlaybackState.Playing) PlaybackAction.Pause else PlaybackAction.Play) }) {
        Text(if (state.playbackState == PlaybackState.Playing) "Pause" else "Play")
    }
}

@Composable
private fun SpeedMenu(state: TimelineSnapshot, onAction: (PlaybackAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("${state.speed.multiplier}×") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0.5, 0.75, 1.0, 1.25, 1.5).forEach { multiplier ->
                DropdownMenuItem(text = { Text("${multiplier}×") }, onClick = {
                    onAction(PlaybackAction.SetSpeed(com.ruxor.kermitpiano.core.timeline.PlaybackSpeed(multiplier)))
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun ViewMenu(mode: PianoViewportMode, onChanged: (PianoViewportMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("View") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PianoViewportMode.entries.forEach { choice ->
                DropdownMenuItem(text = { Text(choice.topBarName()) }, onClick = { onChanged(choice); expanded = false })
            }
        }
    }
}

private fun PianoViewportMode.topBarName(): String = when (this) {
    PianoViewportMode.Practice -> "Practice"
    PianoViewportMode.FollowSong -> "Follow Song"
    PianoViewportMode.Full88 -> "Full 88"
}

@Composable
private fun MoreMenu(
    state: TimelineSnapshot,
    onAction: (PlaybackAction) -> Unit,
    debugVisible: Boolean,
    onDebugChanged: () -> Unit,
    audioState: AudioEngineState,
    playerSoundEnabled: Boolean,
    onPlayerSoundChanged: (Boolean) -> Unit,
    onLibrary: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("More") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Restart") }, onClick = { onAction(PlaybackAction.Restart); expanded = false })
            listOf(0.5, 0.75, 1.0, 1.25, 1.5).forEach { multiplier ->
                DropdownMenuItem(text = { Text("Speed ${multiplier}×") }, onClick = { onAction(PlaybackAction.SetSpeed(com.ruxor.kermitpiano.core.timeline.PlaybackSpeed(multiplier))); expanded = false })
            }
            DropdownMenuItem(text = { Text(if (debugVisible) "Hide Debug" else "Debug") }, onClick = { onDebugChanged(); expanded = false })
            DropdownMenuItem(text = { Text(if (playerSoundEnabled) "Player Sound: On" else "Player Sound: Off") }, onClick = { onPlayerSoundChanged(!playerSoundEnabled); expanded = false })
            DropdownMenuItem(text = { Text(audioState.topBarLabel()) }, onClick = { expanded = false })
            DropdownMenuItem(text = { Text("Local Library") }, onClick = { onLibrary(); expanded = false })
        }
    }
}

@Composable
private fun AudioStatusButton(audioState: AudioEngineState, playerSoundEnabled: Boolean, onPlayerSoundChanged: (Boolean) -> Unit) {
    OutlinedButton(onClick = { onPlayerSoundChanged(!playerSoundEnabled) }, modifier = Modifier.widthIn(max = 116.dp)) {
        Text(
            text = if (playerSoundEnabled) audioState.topBarLabel() else "Audio Off",
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun AudioEngineState.topBarLabel(): String = when (this) {
    AudioEngineState.Uninitialized -> "Audio Off"
    AudioEngineState.Initializing -> "Audio…"
    is AudioEngineState.Ready -> "Audio Ready"
    is AudioEngineState.Error -> if (message.startsWith("SoundFont Missing")) "SF2 Missing" else if (message.startsWith("FluidSynth Missing")) "FluidSynth Missing" else "Audio Error"
}

@Composable
private fun InputBadge() {
    Surface(
        color = Color(0xFF17391F),
        contentColor = Color(0xFF9EEB88),
        shape = workspaceShape,
    ) {
        Text(
            text = "Input: Keyboard",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PianoViewControls(
    viewportMode: PianoViewportMode,
    onViewportModeChanged: (PianoViewportMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            enabled = viewportMode != PianoViewportMode.Practice,
            onClick = { onViewportModeChanged(PianoViewportMode.Practice) },
        ) { Text("Practice") }
        OutlinedButton(
            enabled = viewportMode != PianoViewportMode.Full88,
            onClick = { onViewportModeChanged(PianoViewportMode.Full88) },
        ) { Text("Full 88") }
        OutlinedButton(
            enabled = viewportMode != PianoViewportMode.FollowSong,
            onClick = { onViewportModeChanged(PianoViewportMode.FollowSong) },
        ) { Text("Follow Song") }
    }
}

@Composable
private fun SongInformationPanel(
    song: Song,
    playbackState: TimelineSnapshot,
    keyboardInputState: KeyboardInputState,
    viewport: PianoViewport,
    visibleKeyCount: Int,
    audioState: AudioEngineState,
    audioStartupInfo: AudioStartupInfo,
    audioDiagnostics: AudioEngineDiagnostics,
    onTestC4: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(340.dp).heightIn(max = 680.dp),
        colors = CardDefaults.cardColors(containerColor = GameVisualTokens.glassSurface),
        shape = workspaceShape,
    ) {
        Column(
            modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PanelTitle("Song Info")
            InformationRow("Title", song.title)
            InformationRow("Time", "${playbackState.songTime.toClockText()} / ${song.duration.toClockText()}")
            InformationRow("Notes", song.notes.size.toString())
            PanelDivider()
            PanelTitle("Playback")
            InformationRow(
                "State",
                if (playbackState.playbackState == PlaybackState.Playing) "Playing" else "Paused",
                accent = if (playbackState.playbackState == PlaybackState.Playing) Color(0xFF9EEB88) else Color(0xFFFFC46B),
            )
            InformationRow("Speed", "${playbackState.speed.multiplier}×")
            PanelDivider()
            PanelTitle("Keyboard")
            InformationRow("Pressed", keyboardInputState.pressedNotes.size.toString())
            InformationRow("Last note", keyboardInputState.lastEvent?.note?.label ?: "—")
            InformationRow("Event", keyboardInputState.lastEvent?.type?.name ?: "—")
            PanelDivider()
            PanelTitle("Debug")
            InformationRow("Keyboard octave", keyboardInputState.octave.toString())
            InformationRow("Keyboard MIDI", keyboardInputState.midiRange.toString())
            InformationRow("Viewport", viewport.mode.name)
            InformationRow("Visible MIDI", viewport.visibleMidiRange.toString())
            InformationRow("Total Piano Keys", PianoModel.keys.size.toString())
            InformationRow("Visible Keys", visibleKeyCount.toString())
            PanelDivider()
            PanelTitle("Audio")
            InformationRow("State", audioState.topBarLabel())
            InformationRow("Backend", audioDiagnostics.backend)
            InformationRow("Executable", audioDiagnostics.executablePath ?: "—")
            InformationRow("Selected SF2", audioStartupInfo.selectedSoundFontPath ?: "—")
            InformationRow("Process PID", audioDiagnostics.processId?.toString() ?: "—")
            InformationRow("Last error", audioDiagnostics.lastError ?: audioStartupInfo.discoveryFailureReason ?: "—")
            if (audioState is AudioEngineState.Ready) Button(onClick = onTestC4) { Text("Test C4") }
            PanelDivider()
            PanelTitle("SoundFont Discovery")
            InformationRow("user.dir", audioStartupInfo.userDirectory)
            InformationRow("Project root", audioStartupInfo.projectRoot ?: "—")
            InformationRow("Configured", audioStartupInfo.configuredSoundFontPath ?: "—")
            audioStartupInfo.candidates.forEach { candidate ->
                Text(candidate.source, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                InformationRow("Path", candidate.absolutePath)
                InformationRow("Exists / regular / readable", "${candidate.exists} / ${candidate.regularFile} / ${candidate.readable}")
                InformationRow("Size / valid", "${candidate.sizeBytes ?: "—"} / ${candidate.valid}")
            }
            audioDiagnostics.stderr?.let { InformationRow("FluidSynth stderr", it) }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Judgement line aligned to PianoLayout",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun MidiAnalysisPanel(
    analysis: MidiAnalysis,
    mappings: Map<Int, TrackHand>,
    onMappingChanged: (Int, TrackHand) -> Unit,
    onCancel: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(620.dp).heightIn(max = 680.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = workspaceShape,
    ) {
        Column(
            modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PanelTitle("MIDI Analysis")
            InformationRow("Song Name", analysis.songName)
            InformationRow("Duration", SongTime(analysis.durationMicroseconds.microseconds).toClockText())
            InformationRow("Tempo", "${analysis.tempoBpm.toInt()} BPM")
            InformationRow("Time Signature", analysis.timeSignature)
            InformationRow("Key Signature", analysis.keySignature)
            InformationRow("Track Count", analysis.trackCount.toString())
            InformationRow("Note Count", analysis.noteCount.toString())
            InformationRow("Min / Max Note", "${analysis.minNote ?: "—"} / ${analysis.maxNote ?: "—"}")
            PanelDivider()
            analysis.tracks.forEach { track ->
                Text(track.name, fontWeight = FontWeight.Bold)
                InformationRow("Instrument", track.instrument)
                InformationRow("Channel", track.channels.joinToString().ifEmpty { "—" })
                InformationRow("Average Pitch", track.averagePitch?.toInt()?.toString() ?: "—")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TrackHand.entries.forEach { hand ->
                        OutlinedButton(
                            enabled = mappings[track.index] != hand,
                            onClick = { onMappingChanged(track.index, hand) },
                        ) { Text(hand.name.uppercase()) }
                    }
                }
                PanelDivider()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onImport) { Text("Import Song") }
            }
        }
    }
}

@Composable
private fun LocalSongLibraryPanel(
    songs: List<SongFile>,
    onRefresh: () -> Unit,
    onOpen: (SongFile) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(620.dp).heightIn(max = 620.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = workspaceShape,
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PanelTitle("Local Song Library")
                OutlinedButton(onClick = onRefresh) { Text("Refresh Library") }
            }
            if (songs.isEmpty()) {
                Text("No MIDI files found in source/midi. Add .mid or .midi files, then refresh.", color = MaterialTheme.colorScheme.outline)
            } else {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    songs.forEach { file ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text("${file.byteSize / 1_024} KB • modified ${file.modifiedEpochMillis} • Not analyzed • Duration —", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Button(onClick = { onOpen(file) }) { Text("Analyze") }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
private fun PanelTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun InformationRow(label: String, value: String, accent: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

private fun SongTime.toClockText(): String {
    val totalSeconds = elapsed.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

private fun pianoLayoutKeyCount(viewport: PianoViewport): Int =
    PianoModel.keys.count { key -> key.note.value in viewport.visibleMidiRange }

private fun followSongViewport(song: Song, songTime: SongTime): PianoViewport {
    val upcomingEnd = songTime.elapsed + 4.seconds
    val upcoming = song.notes.filter { note -> note.songTime.elapsed in songTime.elapsed..upcomingEnd }
    val min = upcoming.minOfOrNull { it.note.value } ?: 60
    val max = upcoming.maxOfOrNull { it.note.value } ?: 72
    val center = (min + max) / 2
    val first = (center - 15).coerceIn(PianoModel.firstMidi, PianoModel.lastMidi - 30)
    return PianoViewport(first, first + 30, PianoViewportMode.FollowSong)
}

private val kermitDarkColorScheme = darkColorScheme(
    primary = Color(0xFF72A8FF),
    onPrimary = Color(0xFF06152D),
    secondary = Color(0xFF9EEB88),
    background = Color(0xFF090D13),
    surface = Color(0xFF121923),
    surfaceVariant = Color(0xFF18212D),
    onSurface = Color(0xFFF0F4FC),
    outline = Color(0xFF9AA6B8),
    outlineVariant = Color(0xFF435063),
)

private val workspaceShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
