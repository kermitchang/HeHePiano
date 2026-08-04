package com.ruxor.kermitpiano.feature.keyboardinput

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal fun KeyboardInput.handle(event: KeyEvent): Boolean {
    when (event.key) {
        Key.Z -> {
            if (event.type == KeyEventType.KeyDown) octaveDown()
            return true
        }

        Key.X -> {
            if (event.type == KeyEventType.KeyDown) octaveUp()
            return true
        }
    }

    val key = event.key.toPianoKeyboardKey() ?: return false

    when (event.type) {
        KeyEventType.KeyDown -> onKeyDown(key)
        KeyEventType.KeyUp -> onKeyUp(key)
        else -> return false
    }

    return true
}

private fun Key.toPianoKeyboardKey(): PianoKeyboardKey? = when (this) {
    Key.A -> PianoKeyboardKey.A
    Key.W -> PianoKeyboardKey.W
    Key.S -> PianoKeyboardKey.S
    Key.E -> PianoKeyboardKey.E
    Key.D -> PianoKeyboardKey.D
    Key.F -> PianoKeyboardKey.F
    Key.T -> PianoKeyboardKey.T
    Key.G -> PianoKeyboardKey.G
    Key.Y -> PianoKeyboardKey.Y
    Key.H -> PianoKeyboardKey.H
    Key.U -> PianoKeyboardKey.U
    Key.J -> PianoKeyboardKey.J
    Key.K -> PianoKeyboardKey.K
    Key.L -> PianoKeyboardKey.L
    Key.Semicolon -> PianoKeyboardKey.Semicolon
    else -> null
}
