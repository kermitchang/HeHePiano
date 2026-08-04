package com.ruxor.kermitpiano.feature.audio

import java.io.File

internal object SoundFontDiscovery {
    fun find(configuredPath: String?, soundFontDirectory: File): String? {
        val configured = configuredPath?.let(::File)
        if (configured?.isFile == true && configured.extension.equals("sf2", true)) return configured.absolutePath
        val default = File(soundFontDirectory, "piano.sf2")
        if (default.isFile) return default.absolutePath
        return soundFontDirectory.listFiles()
            .orEmpty()
            .firstOrNull { it.isFile && it.extension.equals("sf2", true) }
            ?.absolutePath
    }
}
