package com.ruxor.kermitpiano.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.timeline.SongTime
import com.ruxor.kermitpiano.feature.pianolayout.PianoModel
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewport
import com.ruxor.kermitpiano.feature.pianolayout.PianoViewportMode
import kotlin.time.Duration.Companion.seconds

internal fun pianoLayoutKeyCount(viewport: PianoViewport): Int =
    PianoModel.keys.count { key -> key.note.value in viewport.visibleMidiRange }

internal fun followSongViewport(song: Song, songTime: SongTime): PianoViewport {
    val upcomingEnd = songTime.elapsed + 4.seconds
    val upcoming = song.notes.filter { note -> note.songTime.elapsed in songTime.elapsed..upcomingEnd }
    val min = upcoming.minOfOrNull { it.note.value } ?: 60
    val max = upcoming.maxOfOrNull { it.note.value } ?: 72
    val center = (min + max) / 2
    val first = (center - 15).coerceIn(PianoModel.firstMidi, PianoModel.lastMidi - 30)
    return PianoViewport(first, first + 30, PianoViewportMode.FollowSong)
}

internal val kermitDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF72A8FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF06152D),
    secondary = androidx.compose.ui.graphics.Color(0xFF9EEB88),
    background = androidx.compose.ui.graphics.Color(0xFF090D13),
    surface = androidx.compose.ui.graphics.Color(0xFF121923),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF18212D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF0F4FC),
    outline = androidx.compose.ui.graphics.Color(0xFF9AA6B8),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF435063),
)

internal val workspaceShape = RoundedCornerShape(14.dp)
