package com.ruxor.kermitpiano.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.timeline.PlaybackState
import com.ruxor.kermitpiano.core.timeline.SongTime
import com.ruxor.kermitpiano.core.timeline.TimelineSnapshot
import com.ruxor.kermitpiano.feature.audio.AudioEngineDiagnostics
import com.ruxor.kermitpiano.feature.audio.AudioEngineState
import com.ruxor.kermitpiano.feature.audio.AudioStartupInfo
import com.ruxor.kermitpiano.feature.gamevisual.GameVisualTokens
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardInputState
import com.ruxor.kermitpiano.feature.midi.MidiAnalysis
import com.ruxor.kermitpiano.feature.midi.TrackHand
import com.ruxor.kermitpiano.feature.pianolayout.PianoModel
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewport
import com.ruxor.kermitpiano.feature.songlibrary.SongFile
import kotlin.time.Duration.Companion.microseconds

@Composable
internal fun SongInformationPanel(
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
internal fun MidiAnalysisPanel(
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
internal fun LocalSongLibraryPanel(
    songs: List<SongFile>,
    loading: Boolean,
    errorMessage: String?,
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
            if (loading) {
                Text("Loading MIDI library…", color = MaterialTheme.colorScheme.outline)
            } else if (songs.isEmpty()) {
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
            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
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
