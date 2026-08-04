package com.ruxor.kermitpiano.feature.midi

internal class StandardMidiParser {
    fun parse(bytes: ByteArray): ParsedMidiFile {
        val reader = MidiReader(bytes)
        require(reader.ascii(4) == "MThd") { "Missing MIDI header." }
        val headerLength = reader.int32()
        require(headerLength >= 6) { "Invalid MIDI header length." }
        val format = reader.int16()
        require(format == 0 || format == 1) { "Only MIDI Type 0 and Type 1 are supported." }
        val trackCount = reader.int16()
        val division = reader.int16()
        require(division and 0x8000 == 0 && division > 0) { "SMPTE time division is not supported." }
        reader.skip(headerLength - 6)

        val tracks = mutableListOf<ParsedMidiTrack>()
        val tempos = mutableListOf<MidiTempo>()
        var timeSignature: String? = null
        var keySignature: String? = null
        repeat(trackCount) { trackIndex ->
            require(reader.ascii(4) == "MTrk") { "Missing MIDI track chunk." }
            val trackLength = reader.int32()
            val trackEnd = reader.position + trackLength
            var tick = 0L
            var runningStatus: Int? = null
            var name: String? = null
            var program: Int? = null
            val channels = linkedSetOf<Int>()
            val activeNotes = mutableMapOf<Pair<Int, Int>, ArrayDeque<Pair<Long, Int>>>()
            val notes = mutableListOf<MidiTickNote>()
            while (reader.position < trackEnd) {
                tick += reader.vlq()
                val first = reader.peek()
                val status = if (first >= 0x80) reader.byte().also { if (it < 0xF0) runningStatus = it } else runningStatus
                    ?: error("Running status without prior channel status.")
                when {
                    status == 0xFF -> {
                        val type = reader.byte()
                        val data = reader.bytes(reader.vlq().toInt())
                        when (type) {
                            0x2F -> Unit
                            0x03 -> name = data.decodeToString()
                            0x51 -> if (data.size == 3) tempos += MidiTempo(tick, data.fold(0) { value, byte -> value shl 8 or byte.toUByte().toInt() })
                            0x58 -> if (data.size >= 2) timeSignature = "${data[0].toUByte().toInt()}/${1 shl data[1].toUByte().toInt()}"
                            0x59 -> if (data.size >= 2) keySignature = keyName(data[0].toInt(), data[1].toInt())
                        }
                    }
                    status == 0xF0 || status == 0xF7 -> reader.skip(reader.vlq().toInt())
                    else -> {
                        val command = status and 0xF0
                        val channel = status and 0x0F
                        channels += channel
                        val data1 = reader.byte()
                        val data2 = if (command == 0xC0 || command == 0xD0) 0 else reader.byte()
                        when (command) {
                            0x80 -> closeNote(activeNotes, notes, channel, data1, tick)
                            0x90 -> if (data2 == 0) closeNote(activeNotes, notes, channel, data1, tick) else
                                activeNotes.getOrPut(channel to data1) { ArrayDeque() }.addLast(tick to data2)
                            0xC0 -> program = data1
                        }
                    }
                }
            }
            reader.position = trackEnd
            tracks += ParsedMidiTrack(trackIndex, name, program, channels, notes.sortedBy { it.startTick })
        }
        return ParsedMidiFile(format, division, tracks, tempos.sortedBy { it.tick }, timeSignature, keySignature)
    }

    private fun closeNote(active: MutableMap<Pair<Int, Int>, ArrayDeque<Pair<Long, Int>>>, notes: MutableList<MidiTickNote>, channel: Int, note: Int, tick: Long) {
        val started = active[channel to note]?.removeFirstOrNull() ?: return
        notes += MidiTickNote(note, started.second, channel, started.first, tick)
    }

    private fun keyName(sharps: Int, minor: Int): String {
        val names = if (minor == 0) majorKeys else minorKeys
        return names[(sharps + 7).coerceIn(0, 14)]
    }

    private companion object {
        val majorKeys = listOf("Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#")
        val minorKeys = listOf("Abm", "Ebm", "Bbm", "Fm", "Cm", "Gm", "Dm", "Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "A#m")
    }
}

private class MidiReader(private val data: ByteArray) {
    var position = 0
    fun peek() = data[position].toUByte().toInt()
    fun byte() = data[position++].toUByte().toInt()
    fun bytes(count: Int) = data.copyOfRange(position, position + count).also { position += count }
    fun ascii(count: Int) = bytes(count).decodeToString()
    fun int16() = byte() shl 8 or byte()
    fun int32() = byte() shl 24 or (byte() shl 16) or (byte() shl 8) or byte()
    fun skip(count: Int) { position += count }
    fun vlq(): Long { var value = 0L; do { val next = byte(); value = (value shl 7) or (next and 0x7F).toLong() } while (next and 0x80 != 0); return value }
}
