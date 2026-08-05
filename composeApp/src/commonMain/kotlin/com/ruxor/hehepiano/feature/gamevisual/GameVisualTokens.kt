package com.ruxor.hehepiano.feature.gamevisual

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.core.song.PianoHand as SongPianoHand

internal enum class PianoHand { Left, Right, Unassigned }

internal data class HandVisualStyle(
    val topColor: Color,
    val bottomColor: Color,
    val borderColor: Color,
    val glowColor: Color,
    val pressedKeyTopColor: Color,
    val pressedKeyBottomColor: Color,
) {
    fun noteBrush(): Brush = Brush.verticalGradient(listOf(topColor, bottomColor))
}

internal object GameVisualTokens {
    val background = Color(0xFF070B13)
    val waterfallBackground = Color(0xFF0B1220)
    val glassSurface = Color(0xD91A2432)
    val glassBorder = Color(0x665B7090)
    val judgeCore = Color(0xFFF2FBFF)
    val judgeGlow = Color(0x8872B9FF)
    val leftHand = HandVisualStyle(
        Color(0xFF80C6FF), Color(0xFF2461D8), Color(0xFFB9E1FF), Color(0x553E91FF),
        Color(0x9979C7FF), Color(0x99406DE0),
    )
    val rightHand = HandVisualStyle(
        Color(0xFFC7FF85), Color(0xFF37A65A), Color(0xFFE1FFC2), Color(0x5549E27C),
        Color(0x99B9F98A), Color(0x9942A963),
    )
    val unassigned = HandVisualStyle(
        Color(0xFFD5A6FF), Color(0xFF7652BE), Color(0xFFE6CCFF), Color(0x554F9CFF),
        Color(0x99D4A6FF), Color(0x996F4BB2),
    )

    fun handFor(note: MidiNote): PianoHand = when {
        note.value < 60 -> PianoHand.Left
        else -> PianoHand.Right
    }

    fun styleFor(note: MidiNote): HandVisualStyle = when (handFor(note)) {
        PianoHand.Left -> leftHand
        PianoHand.Right -> rightHand
        PianoHand.Unassigned -> unassigned
    }

    fun styleFor(note: MidiNote, hand: SongPianoHand): HandVisualStyle = when (hand) {
        SongPianoHand.Left -> leftHand
        SongPianoHand.Right -> rightHand
    }
}
