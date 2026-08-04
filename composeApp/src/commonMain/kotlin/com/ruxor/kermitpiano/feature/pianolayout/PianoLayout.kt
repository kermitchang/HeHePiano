package com.ruxor.kermitpiano.feature.pianolayout

import androidx.compose.ui.geometry.Rect

internal class PianoLayout private constructor(
    val viewport: PianoViewport,
    val width: Float,
    val height: Float,
    val keys: List<PianoKeyLayout>,
) {
    private val keysByMidi = keys.associateBy { key -> key.key.note.value }

    fun keyFor(midiValue: Int): PianoKeyLayout? = keysByMidi[midiValue]

    internal companion object {
        fun create(viewport: PianoViewport, width: Float, height: Float): PianoLayout {
            require(width >= 0f) { "Layout width must not be negative." }
            require(height >= 0f) { "Layout height must not be negative." }

            val visibleKeys = PianoModel.keys.filter { key -> key.note.value in viewport.visibleMidiRange }
            val visibleWhiteKeys = visibleKeys.filter { key -> key.kind == PianoKeyKind.White }
            val whiteKeyWidth = if (visibleWhiteKeys.isEmpty()) 0f else width / visibleWhiteKeys.size
            val whiteIndexByMidi = visibleWhiteKeys.mapIndexed { index, key -> key.note.value to index }.toMap()

            val layoutKeys = visibleKeys.map { key ->
                val rect = when (key.kind) {
                    PianoKeyKind.White -> {
                        val whiteIndex = whiteIndexByMidi.getValue(key.note.value)
                        Rect(
                            left = whiteIndex * whiteKeyWidth,
                            top = 0f,
                            right = (whiteIndex + 1) * whiteKeyWidth,
                            bottom = height,
                        )
                    }

                    PianoKeyKind.Black -> {
                        val precedingWhiteMidi = key.note.value - 1
                        val precedingWhiteIndex = whiteIndexByMidi[precedingWhiteMidi] ?: 0
                        val centerX = (precedingWhiteIndex + 1) * whiteKeyWidth
                        val blackKeyWidth = whiteKeyWidth * BLACK_KEY_WIDTH_RATIO
                        Rect(
                            left = centerX - blackKeyWidth / 2f,
                            top = 0f,
                            right = centerX + blackKeyWidth / 2f,
                            bottom = height * BLACK_KEY_HEIGHT_RATIO,
                        )
                    }
                }
                PianoKeyLayout(
                    key = key,
                    keyRect = rect,
                    centerX = rect.center.x,
                    zOrder = if (key.kind == PianoKeyKind.Black) 1f else 0f,
                )
            }

            return PianoLayout(viewport, width, height, layoutKeys)
        }

        private const val BLACK_KEY_WIDTH_RATIO = 0.62f
        private const val BLACK_KEY_HEIGHT_RATIO = 0.6f
    }
}

internal data class PianoKeyLayout(
    val key: PianoKey,
    val keyRect: Rect,
    val centerX: Float,
    val zOrder: Float,
)
