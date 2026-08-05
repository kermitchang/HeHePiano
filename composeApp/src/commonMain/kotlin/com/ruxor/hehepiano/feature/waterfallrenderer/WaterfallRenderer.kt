package com.ruxor.hehepiano.feature.waterfallrenderer

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
import com.ruxor.hehepiano.core.song.Song
import com.ruxor.hehepiano.core.timeline.SongTime
import com.ruxor.hehepiano.feature.gamevisual.GameVisualTokens
import com.ruxor.hehepiano.feature.pianolayout.PianoLayout
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
    val minimumNoteHeight = minimumNoteHeightDp.toPx()
    val cornerRadius = noteCornerRadius.toPx()
    val pixelsPerSecond = size.height / VISIBLE_SECONDS
    val projector = WaterfallProjector(pixelsPerSecond = pixelsPerSecond)
    val judgementLineY = size.height - judgementLineInset.toPx()
    val firstVisibleTime = SongTime(
        elapsed = (songTime.elapsed - ((size.height - judgementLineY) / pixelsPerSecond).toDouble().seconds)
            .coerceAtLeast(Duration.ZERO),
    )
    val lastVisibleTime = SongTime(
        elapsed = songTime.elapsed + (judgementLineY / pixelsPerSecond).toDouble().seconds,
    )

    pianoLayout.keys.forEach { keyLayout ->
        drawLine(
            color = guideColor.copy(alpha = if (keyLayout.zOrder > 0f) 0.5f else 0.25f),
            start = Offset(keyLayout.centerX, 0f),
            end = Offset(keyLayout.centerX, judgementLineY),
            strokeWidth = guideLineWidth.toPx(),
        )
    }

    noteIndex.visibleIndicesBetween(firstVisibleTime, lastVisibleTime).forEach { index ->
        val note = noteIndex.noteAt(index)
        val keyLayout = pianoLayout.keyFor(note.note.value) ?: return@forEach
        val span = projector.spanAt(note.songTime, note.duration, songTime)
        val bottomY = judgementLineY + span.bottomY
        val durationHeight = (span.bottomY - span.topY).coerceAtLeast(minimumNoteHeight)
        val topY = bottomY - durationHeight
        val noteWidth = keyLayout.keyRect.width * if (keyLayout.zOrder > 0f) BLACK_NOTE_WIDTH_RATIO else NOTE_WIDTH_RATIO
        val visualStyle = GameVisualTokens.styleFor(note.note, note.hand)

        drawRoundRect(
            color = visualStyle.glowColor,
            topLeft = Offset(x = keyLayout.centerX - noteWidth / 2f - glowInset.toPx(), y = topY - glowInset.toPx()),
            size = Size(width = noteWidth + glowInset.toPx() * 2f, height = durationHeight + glowInset.toPx() * 2f),
            cornerRadius = CornerRadius(cornerRadius + glowInset.toPx()),
        )
        drawRoundRect(
            brush = visualStyle.noteBrush(),
            topLeft = Offset(x = keyLayout.centerX - noteWidth / 2f, y = topY),
            size = Size(width = noteWidth, height = durationHeight),
            cornerRadius = CornerRadius(cornerRadius),
        )
        drawLine(
            color = visualStyle.borderColor,
            start = Offset(keyLayout.centerX - noteWidth / 2f + highlightInset.toPx(), topY + highlightInset.toPx()),
            end = Offset(keyLayout.centerX + noteWidth / 2f - highlightInset.toPx(), topY + highlightInset.toPx()),
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
private val minimumNoteHeightDp = 8.dp
private val noteCornerRadius = 7.dp
private val judgementLineInset = 12.dp
private val guideLineWidth = 1.dp
private val judgementLineWidth = 3.dp
private val judgeGlowWidth = 9.dp
private val glowInset = 4.dp
private val highlightInset = 3.dp
private val highlightStroke = 1.dp
