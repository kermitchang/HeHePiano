package com.ruxor.kermitpiano

import com.ruxor.kermitpiano.feature.midi.StandardMidiParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifyMidiFileTest {

    @Test
    fun `FF7 Tifa theme MIDI file parses successfully`() {
        // 測試工作目錄可能是 composeApp/，往上找包含實際 .mid 檔的專案根目錄
        val cwd = File(System.getProperty("user.dir"))
        val projectRoot = generateSequence(cwd) { it.parentFile }
            .firstOrNull { File(it, "source/midi/FFVII - Tifas Theme [mk].mid").isFile }
            ?: cwd
        val midiFile = File(projectRoot, "source/midi/FFVII - Tifas Theme [mk].mid")
        println("專案根目錄: $projectRoot")
        println("📄 MIDI 檔: ${midiFile.name} (${midiFile.length()} bytes)")
        assertTrue(midiFile.isFile, "MIDI 檔不存在: $midiFile")

        val bytes = midiFile.readBytes()
        val parsed = StandardMidiParser().parse(bytes)

        println("格式: Type ${parsed.format}")
        println("Time Division: ${parsed.ticksPerQuarter}")
        println("音軌數: ${parsed.tracks.size}")
        println("拍號: ${parsed.timeSignature ?: "未設定"}")
        println("調號: ${parsed.keySignature ?: "未設定"}")

        parsed.tracks.forEachIndexed { i, track ->
            println("  [軌$i] '${track.name ?: "未命名"}' 音符數=${track.notes.size} 頻道=${track.channels}")
        }

        val totalNotes = parsed.tracks.sumOf { it.notes.size }
        println("總音符數: $totalNotes")

        assertTrue(parsed.tracks.isNotEmpty(), "應該有至少一條音軌")
        assertTrue(totalNotes > 0, "應該有音符資料")
        println("🎉 解析成功！")
    }
}
