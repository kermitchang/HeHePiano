package com.ruxor.hehepiano.feature.audio

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal object FluidSynthExecutableLocator {
    fun locate(command: String = "fluidsynth", pathVariable: String? = System.getenv("PATH")): String? {
        val direct = Path.of(command)
        if (direct.isAbsolute && Files.isExecutable(direct)) return direct.toAbsolutePath().normalize().absolutePathString()
        return pathVariable.orEmpty()
            .split(java.io.File.pathSeparator)
            .asSequence()
            .filter(String::isNotBlank)
            .map { Path.of(it).resolve(command) }
            .firstOrNull(Files::isExecutable)
            ?.toAbsolutePath()
            ?.normalize()
            ?.absolutePathString()
    }
}
