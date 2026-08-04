package com.ruxor.kermitpiano.feature.midi

import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongNote
import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.microseconds

internal class MidiAnalyzer {
    fun analyze(fileName: String, parsed: ParsedMidiFile): MidiAnalysis {
        val allNotes = parsed.tracks.flatMap { it.notes }
        val converter = TickTimeConverter(parsed.ticksPerQuarter, parsed.tempos)
        val duration = allNotes.maxOfOrNull { converter.microsecondsAt(it.endTick) } ?: 0L
        val tracks = parsed.tracks.map { track ->
            val average = track.notes.takeIf { it.isNotEmpty() }?.map { it.note }?.average()
            TrackAnalysis(
                index = track.index,
                name = track.name ?: "Track ${track.index + 1}",
                instrument = GeneralMidiPrograms.name(track.program),
                channels = track.channels,
                averagePitch = average,
                suggestedHand = when { average == null -> TrackHand.Ignore; average < 60 -> TrackHand.Left; else -> TrackHand.Right },
                noteCount = track.notes.size,
            )
        }
        return MidiAnalysis(
            songName = fileName.substringBeforeLast('.'), durationMicroseconds = duration,
            tempoBpm = 60_000_000.0 / (parsed.tempos.firstOrNull()?.microsecondsPerQuarter ?: 500_000),
            timeSignature = parsed.timeSignature ?: "4/4", keySignature = parsed.keySignature ?: "Unknown",
            trackCount = parsed.tracks.size, noteCount = allNotes.size,
            minNote = allNotes.minOfOrNull { it.note }, maxNote = allNotes.maxOfOrNull { it.note }, tracks = tracks, parsed = parsed,
        )
    }

    fun import(analysis: MidiAnalysis, mappings: Map<Int, TrackHand>): PlayableSong {
        val converter = TickTimeConverter(analysis.parsed.ticksPerQuarter, analysis.parsed.tempos)
        val notes = analysis.parsed.tracks.flatMap { track ->
            if ((mappings[track.index] ?: analysis.tracks[track.index].suggestedHand) == TrackHand.Ignore) emptyList() else
                track.notes.map { note -> SongNote(MidiNote(note.note), SongTime(converter.microsecondsAt(note.startTick).microseconds)) }
        }.sortedBy { it.songTime }
        return PlayableSong(
            song = Song(analysis.songName.lowercase().replace(' ', '-'), analysis.songName, SongTime(analysis.durationMicroseconds.microseconds), notes),
            analysis = analysis,
            trackMappings = mappings,
        )
    }
}

internal data class PlayableSong(val song: Song, val analysis: MidiAnalysis, val trackMappings: Map<Int, TrackHand>)

private class TickTimeConverter(private val division: Int, tempos: List<MidiTempo>) {
    private val tempos = (tempos + MidiTempo(0, 500_000)).distinctBy { it.tick }.sortedBy { it.tick }
    fun microsecondsAt(tick: Long): Long {
        var elapsed = 0.0; var previousTick = 0L; var tempo = 500_000
        for (change in tempos) { if (change.tick > tick) break; elapsed += (change.tick - previousTick) * tempo.toDouble() / division; previousTick = change.tick; tempo = change.microsecondsPerQuarter }
        return (elapsed + (tick - previousTick) * tempo.toDouble() / division).roundToLong()
    }
}

private object GeneralMidiPrograms {
    fun name(program: Int?): String = when (program) { null -> "Unknown"; in 0..7 -> "Piano"; in 8..15 -> "Chromatic Percussion"; in 16..23 -> "Organ"; in 24..31 -> "Guitar"; in 32..39 -> "Bass"; in 40..47 -> "Strings"; else -> "Program ${program + 1}" }
}
