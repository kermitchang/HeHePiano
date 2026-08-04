package com.ruxor.kermitpiano.feature.midi

import kotlin.test.Test
import kotlin.test.assertEquals

class StandardMidiParserTest {
    @Test
    fun `parses type zero running status velocity zero and metadata`() {
        val track = bytes(
            0, 0xFF, 0x03, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            0, 0xFF, 0x51, 3, 0x07, 0xA1, 0x20,
            0, 0xFF, 0x58, 4, 4, 2, 24, 8,
            0, 0xC0, 0,
            0, 0x90, 60, 100,
            0x83, 0x60, 60, 0,
            0, 0xFF, 0x2F, 0,
        )
        val parsed = StandardMidiParser().parse(midi(format = 0, division = 480, tracks = listOf(track)))

        assertEquals("Test", parsed.tracks.single().name)
        assertEquals(500_000, parsed.tempos.single().microsecondsPerQuarter)
        assertEquals("4/4", parsed.timeSignature)
        assertEquals(MidiTickNote(60, 100, 0, 0, 480), parsed.tracks.single().notes.single())
    }

    @Test
    fun `analyzes type one duration in microseconds without tick recalculation`() {
        val tempoTrack = bytes(0, 0xFF, 0x51, 3, 0x07, 0xA1, 0x20, 0, 0xFF, 0x2F, 0)
        val noteTrack = bytes(0, 0x90, 60, 90, 0x87, 0x40, 0x80, 60, 0, 0, 0xFF, 0x2F, 0)
        val parsed = StandardMidiParser().parse(midi(1, 480, listOf(tempoTrack, noteTrack)))
        val analysis = MidiAnalyzer().analyze("scale.mid", parsed)

        assertEquals(2, analysis.trackCount)
        assertEquals(1_000_000, analysis.durationMicroseconds)
        assertEquals(120.0, analysis.tempoBpm)
        assertEquals(1, analysis.noteCount)
    }

    private fun midi(format: Int, division: Int, tracks: List<ByteArray>): ByteArray {
        val result = mutableListOf<Int>()
        result += "MThd".map { it.code }; result += listOf(0, 0, 0, 6, 0, format, 0, tracks.size, division shr 8, division and 0xFF)
        tracks.forEach { track -> result += "MTrk".map { it.code }; result += listOf(0, 0, 0, track.size); result += track.map { it.toUByte().toInt() } }
        return result.map { it.toByte() }.toByteArray()
    }

    private fun bytes(vararg values: Int) = values.map { it.toByte() }.toByteArray()
}
