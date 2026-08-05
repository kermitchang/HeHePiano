package com.ruxor.kermitpiano.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruxor.kermitpiano.core.timeline.PlaybackState
import com.ruxor.kermitpiano.core.timeline.PlaybackSpeed
import com.ruxor.kermitpiano.core.timeline.TimelineSnapshot
import com.ruxor.kermitpiano.feature.audio.AudioEngineState
import com.ruxor.kermitpiano.feature.autoplay.AutoPlayState
import com.ruxor.kermitpiano.feature.playback.PlaybackAction
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode

@Composable
internal fun PianoTopBar(
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
    demoModeEnabled: Boolean,
    demoState: AutoPlayState,
    audioReady: Boolean,
    onDemoModeChanged: (Boolean) -> Unit,
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
                    demoModeEnabled, demoState, audioReady, onDemoModeChanged,
                )
                TopBarLayoutMode.Compact -> CompactTopBar(
                    playbackState, onPlaybackAction, viewportMode, onViewportModeChanged,
                    debugVisible, onDebugChanged, onOpenMidi, songTitle, onSelectNextSong,
                    audioState, playerSoundEnabled, onPlayerSoundChanged, onLibrary,
                    demoModeEnabled, demoState, audioReady, onDemoModeChanged,
                )
                TopBarLayoutMode.Narrow -> NarrowTopBar(
                    playbackState, onPlaybackAction, viewportMode, onViewportModeChanged,
                    debugVisible, onDebugChanged, onOpenMidi,
                    audioState, playerSoundEnabled, onPlayerSoundChanged, onLibrary,
                    demoModeEnabled, demoState, audioReady, onDemoModeChanged,
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
    demoModeEnabled: Boolean,
    demoState: AutoPlayState,
    audioReady: Boolean,
    onDemoModeChanged: (Boolean) -> Unit,
) = TopBarRow {
    Brand(showName = true)
    SongControls(songTitle, onOpenMidi, onSelectSong, showSongTitle = true)
    PlaybackGroup(state, onAction, showRestart = true, compactSpeed = false)
    PianoViewControls(viewport, onViewportChanged)
    InputBadge(demoModeEnabled)
    AudioStatusButton(audioState, playerSoundEnabled, onPlayerSoundChanged)
    DemoModeButton(demoModeEnabled, demoState, audioReady, onDemoModeChanged)
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
    demoModeEnabled: Boolean,
    demoState: AutoPlayState,
    audioReady: Boolean,
    onDemoModeChanged: (Boolean) -> Unit,
) = TopBarRow {
    Brand(showName = false)
    SongControls(songTitle, onOpenMidi, onSelectSong, showSongTitle = false)
    PlaybackGroup(state, onAction, showRestart = true, compactSpeed = true)
    PianoViewControls(viewport, onViewportChanged)
    AudioStatusButton(audioState, playerSoundEnabled, onPlayerSoundChanged)
    DemoModeButton(demoModeEnabled, demoState, audioReady, onDemoModeChanged)
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
    demoModeEnabled: Boolean,
    demoState: AutoPlayState,
    audioReady: Boolean,
    onDemoModeChanged: (Boolean) -> Unit,
) = TopBarRow {
    Text(text = "♬", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    Button(onClick = onOpenMidi) { Text("Open") }
    PlayPauseButton(state, onAction)
    ViewMenu(viewport, onViewportChanged)
    Spacer(Modifier.weight(1f))
    MoreMenu(
        state = state,
        onAction = onAction,
        debugVisible = debugVisible,
        onDebugChanged = onDebugChanged,
        audioState = audioState,
        playerSoundEnabled = playerSoundEnabled,
        onPlayerSoundChanged = onPlayerSoundChanged,
        onLibrary = onLibrary,
        demoModeEnabled = demoModeEnabled,
        demoState = demoState,
        audioReady = audioReady,
        onDemoModeChanged = onDemoModeChanged,
    )
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
                onClick = { onAction(PlaybackAction.SetSpeed(PlaybackSpeed(multiplier))) },
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
                    onAction(PlaybackAction.SetSpeed(PlaybackSpeed(multiplier)))
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
    demoModeEnabled: Boolean,
    demoState: AutoPlayState,
    audioReady: Boolean,
    onDemoModeChanged: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("More") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Restart") }, onClick = { onAction(PlaybackAction.Restart); expanded = false })
            listOf(0.5, 0.75, 1.0, 1.25, 1.5).forEach { multiplier ->
                DropdownMenuItem(text = { Text("Speed ${multiplier}×") }, onClick = { onAction(PlaybackAction.SetSpeed(PlaybackSpeed(multiplier))); expanded = false })
            }
            DropdownMenuItem(text = { Text(if (debugVisible) "Hide Debug" else "Debug") }, onClick = { onDebugChanged(); expanded = false })
            DropdownMenuItem(text = { Text(if (playerSoundEnabled) "Player Sound: On" else "Player Sound: Off") }, onClick = { onPlayerSoundChanged(!playerSoundEnabled); expanded = false })
            DropdownMenuItem(text = { Text(audioState.topBarLabel()) }, onClick = { expanded = false })
            DropdownMenuItem(
                enabled = audioReady || demoModeEnabled,
                text = {
                    Text(
                        when {
                            !audioReady && !demoModeEnabled -> "Demo Mode: Audio unavailable"
                            demoModeEnabled && demoState == AutoPlayState.Playing -> "Demo Mode: Playing"
                            demoModeEnabled -> "Demo Mode: On"
                            else -> "Demo Mode: Off"
                        },
                    )
                },
                onClick = { onDemoModeChanged(!demoModeEnabled); expanded = false },
            )
            DropdownMenuItem(text = { Text("Local Library") }, onClick = { onLibrary(); expanded = false })
        }
    }
}

@Composable
private fun DemoModeButton(
    enabled: Boolean,
    state: AutoPlayState,
    audioReady: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    OutlinedButton(
        enabled = audioReady || enabled,
        onClick = { onChanged(!enabled) },
    ) {
        Text(
            when {
                enabled && state == AutoPlayState.Playing -> "Demo Playing"
                enabled -> "Demo On"
                else -> "Demo Off"
            },
        )
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
private fun InputBadge(demoModeEnabled: Boolean) {
    Surface(
        color = Color(0xFF17391F),
        contentColor = Color(0xFF9EEB88),
        shape = workspaceShape,
    ) {
        Text(
            text = if (demoModeEnabled) "Input: Demo" else "Input: Keyboard",
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
