package com.ruxor.kermitpiano.feature.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruxor.kermitpiano.core.timeline.PlaybackSpeed
import com.ruxor.kermitpiano.core.timeline.PlaybackState
import com.ruxor.kermitpiano.core.timeline.TimelineSnapshot

@Composable
internal fun PlaybackControls(
    state: TimelineSnapshot,
    onAction: (PlaybackAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.playbackState == PlaybackState.Playing) {
            Button(onClick = { onAction(PlaybackAction.Pause) }) {
                Text("Pause")
            }
        } else {
            Button(onClick = { onAction(PlaybackAction.Play) }) {
                Text("Play")
            }
        }

        OutlinedButton(onClick = { onAction(PlaybackAction.Restart) }) {
            Text("Restart")
        }

        playbackSpeeds.forEach { speed ->
            OutlinedButton(
                enabled = speed != state.speed,
                onClick = { onAction(PlaybackAction.SetSpeed(speed)) },
            ) {
                Text("${speed.multiplier}×")
            }
        }
    }
}

private val playbackSpeeds = listOf(
    PlaybackSpeed(0.5),
    PlaybackSpeed.normal,
    PlaybackSpeed(1.5),
    PlaybackSpeed(2.0),
)
