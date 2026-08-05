package com.ruxor.hehepiano.feature.virtualpiano

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.feature.gamevisual.GameVisualTokens
import com.ruxor.hehepiano.feature.pianolayout.PianoKeyKind
import com.ruxor.hehepiano.feature.pianolayout.PianoKeyLayout
import com.ruxor.hehepiano.feature.pianolayout.PianoLayout
import com.ruxor.hehepiano.feature.pianolayout.PianoViewportMode

@Composable
internal fun VirtualPiano(
    layout: PianoLayout,
    pressedNotes: Set<MidiNote>,
    density: Density,
    viewportMode: PianoViewportMode,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(with(density) { layout.height.toDp() })
            .clip(pianoShape)
            .background(Color(0xFF11161F)),
    ) {
        layout.keys.sortedBy { key -> key.zOrder }.forEach { keyLayout ->
            PianoKeySurface(
                keyLayout = keyLayout,
                pressed = keyLayout.key.note in pressedNotes,
                density = density,
                viewportMode = viewportMode,
            )
        }
    }
}

@Composable
private fun PianoKeySurface(
    keyLayout: PianoKeyLayout,
    pressed: Boolean,
    density: Density,
    viewportMode: PianoViewportMode,
) {
    val key = keyLayout.key
    val shape = if (key.kind == PianoKeyKind.White) whiteKeyShape else blackKeyShape
    val handStyle = GameVisualTokens.styleFor(key.note)
    val keyBrush = when {
        pressed -> Brush.verticalGradient(listOf(handStyle.pressedKeyTopColor, handStyle.pressedKeyBottomColor))
        key.kind == PianoKeyKind.White -> whiteKeyBrush
        else -> blackKeyBrush
    }
    val textColor = if (key.kind == PianoKeyKind.White) Color(0xFF10131A) else Color(0xFFE4EAF5)
    val rect = keyLayout.keyRect

    Box(
        modifier = Modifier
            .offset(x = with(density) { rect.left.toDp() })
            .width(with(density) { rect.width.toDp() })
            .height(with(density) { rect.height.toDp() })
            .zIndex(keyLayout.zOrder)
            .shadow(if (pressed) 3.dp else 8.dp, shape)
            .clip(shape)
            .background(keyBrush)
            .border(width = 1.dp, color = Color(0x55000000), shape = shape),
    ) {
        if (shouldShowLabel(keyLayout, viewportMode)) {
            Text(
                text = key.note.label,
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-16).dp),
            )
        }
    }
}

private fun shouldShowLabel(keyLayout: PianoKeyLayout, viewportMode: PianoViewportMode): Boolean {
    return keyLayout.key.kind == PianoKeyKind.White &&
        (viewportMode != PianoViewportMode.Full88 || keyLayout.key.note.value % 12 == 0)
}

private val pianoShape = RoundedCornerShape(14.dp)
private val whiteKeyShape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
private val blackKeyShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
private val whiteKeyBrush = Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFF), Color(0xFFCED5E2)))
private val blackKeyBrush = Brush.verticalGradient(colors = listOf(Color(0xFF3B4554), Color(0xFF090C12)))
