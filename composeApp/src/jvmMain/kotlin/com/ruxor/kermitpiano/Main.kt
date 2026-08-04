package com.ruxor.kermitpiano

import androidx.compose.ui.window.singleWindowApplication
import com.ruxor.kermitpiano.app.KermitPianoApp
import com.ruxor.kermitpiano.feature.keyboardinput.KeyboardInput
import com.ruxor.kermitpiano.feature.keyboardinput.handle
import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import com.ruxor.kermitpiano.feature.audio.FluidSynthPianoAudioEngine
import com.ruxor.kermitpiano.feature.audio.PianoAudioConfig
import com.ruxor.kermitpiano.feature.audio.SoundFontDiscovery
import com.ruxor.kermitpiano.feature.songlibrary.LocalMidiDirectorySongSource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() {
    val keyboardInput = KeyboardInput()
    val projectRoot = File(System.getProperty("user.dir"))
    val localMidiSource = LocalMidiDirectorySongSource(File(projectRoot, "source/midi"))
    val soundFontDirectory = File(projectRoot, "source/soundfonts")
    val audioEngine = FluidSynthPianoAudioEngine()
    val audioConfig = PianoAudioConfig(soundFontPath = SoundFontDiscovery.find(null, soundFontDirectory))

    singleWindowApplication(
        title = "KermitPiano",
        onKeyEvent = keyboardInput::handle,
    ) {
        KermitPianoApp(
            keyboardInput = keyboardInput,
            openMidiFile = ::openMidiFile,
            localSongSource = localMidiSource,
            audioEngine = audioEngine,
            audioConfig = audioConfig,
        )
    }
}

private fun openMidiFile(): SelectedMidiFile? {
    val dialog = FileDialog(null as Frame?, "Open MIDI", FileDialog.LOAD).apply {
        filenameFilter = java.io.FilenameFilter { _, name -> name.endsWith(".mid", true) || name.endsWith(".midi", true) }
        isVisible = true
    }
    val name = dialog.file ?: return null
    val file = File(dialog.directory, name)
    return SelectedMidiFile(file.name, file.readBytes())
}
