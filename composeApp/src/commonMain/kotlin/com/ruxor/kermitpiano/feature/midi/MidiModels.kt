package com.ruxor.kermitpiano.feature.midi

internal data class ParsedMidiFile(
    val format: Int,
    val ticksPerQuarter: Int,
    val tracks: List<ParsedMidiTrack>,
    val tempos: List<MidiTempo>,
    val timeSignature: String?,
    val keySignature: String?,
)

internal data class ParsedMidiTrack(
    val index: Int,
    val name: String?,
    val program: Int?,
    val channels: Set<Int>,
    val notes: List<MidiTickNote>,
)

internal data class MidiTickNote(
    val note: Int,
    val velocity: Int,
    val channel: Int,
    val startTick: Long,
    val endTick: Long,
)

internal data class MidiTempo(val tick: Long, val microsecondsPerQuarter: Int)

internal enum class TrackHand { Left, Right, Ignore }

internal data class TrackAnalysis(
    val index: Int,
    val name: String,
    val instrument: String,
    val channels: Set<Int>,
    val averagePitch: Double?,
    val suggestedHand: TrackHand,
    val noteCount: Int,
)

internal data class MidiAnalysis(
    val songName: String,
    val durationMicroseconds: Long,
    val tempoBpm: Double,
    val timeSignature: String,
    val keySignature: String,
    val trackCount: Int,
    val noteCount: Int,
    val minNote: Int?,
    val maxNote: Int?,
    val tracks: List<TrackAnalysis>,
    val parsed: ParsedMidiFile,
)

internal data class SelectedMidiFile(val name: String, val bytes: ByteArray)

internal data class MidiFileSelection(
    val name: String,
    val load: suspend () -> SelectedMidiFile,
)
