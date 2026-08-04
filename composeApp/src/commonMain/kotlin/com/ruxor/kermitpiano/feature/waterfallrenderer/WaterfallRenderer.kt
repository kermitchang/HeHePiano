package com.ruxor.kermitpiano.feature.waterfallrenderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.timeline.SongTime
import com.ruxor.kermitpiano.feature.gamevisual.GameVisualTokens
import com.ruxor.kermitpiano.feature.pianolayout.PianoLayout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun WaterfallRenderer(
    song: Song,
    songTime: SongTime,
    pianoLayout: PianoLayout,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val noteIndex = remember(song) { VisibleNoteIndex(song) }

    Canvas(modifier = modifier.background(GameVisualTokens.waterfallBackground)) {
        drawWaterfall(
            noteIndex = noteIndex,
            pianoLayout = pianoLayout,
            songTime = songTime,
            guideColor = colors.outlineVariant,
            judgementLineColor = GameVisualTokens.judgeCore,
        )
    }
}

private fun DrawScope.drawWaterfall(
    noteIndex: VisibleNoteIndex,
    pianoLayout: PianoLayout,
    songTime: SongTime,
    guideColor: Color,
    judgementLineColor: Color,
) {
    val noteHeight = noteHeightDp.toPx()
    val cornerRadius = noteCornerRadius.toPx()
    val pixelsPerSecond = size.height / VISIBLE_SECONDS
    val projector = WaterfallProjector(pixelsPerSecond = pixelsPerSecond)
    val judgementLineY = size.height - judgementLineInset.toPx()
    val firstVisibleTime = SongTime(
        elapsed = (songTime.elapsed - ((size.height - judgementLineY) / pixelsPerSecond).toDouble().seconds)
            .coerceAtLeast(Duration.ZERO),
    )
    val lastVisibleTime = SongTime(
        elapsed = songTime.elapsed + ((judgementLineY + noteHeight) / pixelsPerSecond).toDouble().seconds,
    )

    pianoLayout.keys.forEach { keyLayout ->
        drawLine(
            color = guideColor.copy(alpha = if (keyLayout.zOrder > 0f) 0.5f else 0.25f),
            start = Offset(keyLayout.centerX, 0f),
            end = Offset(keyLayout.centerX, judgementLineY),
            strokeWidth = guideLineWidth.toPx(),
        )
    }

    noteIndex.indicesBetween(firstVisibleTime, lastVisibleTime).forEach { index ->
        val note = noteIndex.noteAt(index)
        val keyLayout = pianoLayout.keyFor(note.note.value) ?: return@forEach
        val y = judgementLineY + projector.positionAt(noteTime = note.songTime, songTime = songTime).y
        val noteWidth = keyLayout.keyRect.width * if (keyLayout.zOrder > 0f) BLACK_NOTE_WIDTH_RATIO else NOTE_WIDTH_RATIO
        val visualStyle = GameVisualTokens.styleFor(note.note)

        drawRoundRect(
            color = visualStyle.glowColor,
            topLeft = Offset(x = keyLayout.centerX - noteWidth / 2f - glowInset.toPx(), y = y - glowInset.toPx()),
            size = Size(width = noteWidth + glowInset.toPx() * 2f, height = noteHeight + glowInset.toPx() * 2f),
            cornerRadius = CornerRadius(cornerRadius + glowInset.toPx()),
        )
        drawRoundRect(
            brush = visualStyle.noteBrush(),
            topLeft = Offset(x = keyLayout.centerX - noteWidth / 2f, y = y),
            size = Size(width = noteWidth, height = noteHeight),
            cornerRadius = CornerRadius(cornerRadius),
        )
        drawLine(
            color = visualStyle.borderColor,
            start = Offset(keyLayout.centerX - noteWidth / 2f + highlightInset.toPx(), y + highlightInset.toPx()),
            end = Offset(keyLayout.centerX + noteWidth / 2f - highlightInset.toPx(), y + highlightInset.toPx()),
            strokeWidth = highlightStroke.toPx(),
        )
    }

    drawLine(color = GameVisualTokens.judgeGlow, start = Offset(0f, judgementLineY), end = Offset(size.width, judgementLineY), strokeWidth = judgeGlowWidth.toPx())
    drawLine(
        color = judgementLineColor,
        start = Offset(0f, judgementLineY),
        end = Offset(size.width, judgementLineY),
        strokeWidth = judgementLineWidth.toPx(),
    )
}

private const val VISIBLE_SECONDS = 4f
private const val NOTE_WIDTH_RATIO = 0.68f
private const val BLACK_NOTE_WIDTH_RATIO = 0.82f
private val noteHeightDp = 36.dp
private val noteCornerRadius = 7.dp
private val judgementLineInset = 12.dp
private val guideLineWidth = 1.dp
private val judgementLineWidth = 3.dp
private val judgeGlowWidth = 9.dp
private val glowInset = 4.dp
private val highlightInset = 3.dp
private val highlightStroke = 1.dp
