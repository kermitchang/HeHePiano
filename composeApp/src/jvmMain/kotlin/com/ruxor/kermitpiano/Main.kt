package com.ruxor.kermitpiano

import androidx.compose.ui.window.singleWindowApplication
import com.ruxor.kermitpiano.app.KermitPianoApp
import com.ruxor.kermitpiano.feature.audio.FluidSynthPianoAudioEngine
import com.ruxor.kermitpiano.feature.audio.PianoAudioConfig
import com.ruxor.kermitpiano.feature.audio.SoundFontLocator
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardInput
import com.ruxor.kermitpiano.feature.keyboardinput.handle
import com.ruxor.kermitpiano.feature.midi.MidiFileSelection
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import com.ruxor.kermitpiano.feature.midi.readMidiBytes
import com.ruxor.kermitpiano.feature.midi.UsbMidiInput
import com.ruxor.kermitpiano.feature.songlibrary.LocalMidiDirectorySongSource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() {
    val keyboardInput = KeyboardInput()
    val usbMidiInput = UsbMidiInput(
        onUnavailable = { message -> println("MIDI 輸入不可用: $message") },
    )
    val startupInfo = SoundFontLocator.locate(configuredPath = null)
    val projectRoot = File(startupInfo.projectRoot ?: System.getProperty("user.dir"))
    val localMidiSource = LocalMidiDirectorySongSource(File(projectRoot, "source/midi"))
    val audioEngine = FluidSynthPianoAudioEngine()
    val audioConfig = PianoAudioConfig(soundFontPath = startupInfo.selectedSoundFontPath)
    println(startupInfo.toDebugLog())
    println("可用 MIDI 裝置: ${UsbMidiInput.availableDevices()}")

    singleWindowApplication(
        title = "KermitPiano",
        onKeyEvent = keyboardInput::handle,
    ) {
        KermitPianoApp(
            keyboardInput = keyboardInput,
            midiInput = usbMidiInput,
            openMidiFile = ::openMidiFile,
            localSongSource = localMidiSource,
            audioEngine = audioEngine,
            audioConfig = audioConfig,
            audioStartupInfo = startupInfo,
        )
    }
}

private fun com.ruxor.kermitpiano.feature.audio.AudioStartupInfo.toDebugLog(): String = buildString {
    appendLine("KermitPiano SoundFont discovery")
    appendLine("user.dir=$userDirectory")
    appendLine("projectRoot=$projectRoot")
    appendLine("configuredSoundFontPath=$configuredSoundFontPath")
    candidates.forEach { candidate ->
        appendLine("candidate[${candidate.source}]=${candidate.absolutePath} exists=${candidate.exists} regular=${candidate.regularFile} readable=${candidate.readable} size=${candidate.sizeBytes} valid=${candidate.valid}")
    }
    appendLine("selectedSoundFontPath=$selectedSoundFontPath")
    append("failureReason=$discoveryFailureReason")
}

private fun openMidiFile(): MidiFileSelection? {
    val dialog = FileDialog(null as Frame?, "Open MIDI", FileDialog.LOAD).apply {
        filenameFilter = java.io.FilenameFilter { _, name -> name.endsWith(".mid", true) || name.endsWith(".midi", true) }
        isVisible = true
    }
    val name = dialog.file ?: return null
    val file = File(dialog.directory, name)
    return MidiFileSelection(file.name) {
        withContext(Dispatchers.IO) {
            SelectedMidiFile(file.name, file.readMidiBytes())
        }
    }
}
