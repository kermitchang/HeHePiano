package com.ruxor.kermitpiano.feature.audio

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

internal object SoundFontLocator {
    private const val systemPropertyName = "kermitpiano.soundfont"
    private const val environmentVariableName = "KERMITPIANO_SOUNDFONT"

    fun locate(
        configuredPath: String?,
        userDirectory: Path = Path.of(System.getProperty("user.dir")),
        systemPropertyPath: String? = System.getProperty(systemPropertyName),
        environmentPath: String? = System.getenv(environmentVariableName),
    ): AudioStartupInfo {
        val normalizedUserDirectory = userDirectory.toAbsolutePath().normalize()
        val projectRoot = findProjectRoot(normalizedUserDirectory)
        val candidates = buildList {
            configuredPath?.let { add(candidate("Configured", Path.of(it), normalizedUserDirectory)) }
            systemPropertyPath?.let { add(candidate("System property", Path.of(it), normalizedUserDirectory)) }
            environmentPath?.let { add(candidate("Environment", Path.of(it), normalizedUserDirectory)) }
            projectRoot?.let { root ->
                val directory = root.resolve("source/soundfonts")
                add(candidate("Project default", directory.resolve("piano.sf2"), normalizedUserDirectory))
                if (Files.isDirectory(directory)) {
                    Files.list(directory).use { files ->
                        files.filter { it.isRegularFile() && it.extension.equals("sf2", ignoreCase = true) }
                            .sorted()
                            .forEach { add(candidate("Project fallback", it, normalizedUserDirectory)) }
                    }
                }
            }
        }
        val selected = candidates.firstOrNull { it.valid }?.absolutePath
        return AudioStartupInfo(
            userDirectory = normalizedUserDirectory.absolutePathString(),
            projectRoot = projectRoot?.absolutePathString(),
            configuredSoundFontPath = configuredPath,
            candidates = candidates,
            selectedSoundFontPath = selected,
            discoveryFailureReason = if (selected == null) failureReason(projectRoot, candidates) else null,
        )
    }

    private fun candidate(source: String, requestedPath: Path, userDirectory: Path): SoundFontCandidateInfo {
        val path = if (requestedPath.isAbsolute) requestedPath else userDirectory.resolve(requestedPath)
        val normalized = path.toAbsolutePath().normalize()
        val exists = Files.exists(normalized)
        val regularFile = Files.isRegularFile(normalized)
        val readable = Files.isReadable(normalized)
        val size = if (regularFile) runCatching { Files.size(normalized) }.getOrNull() else null
        return SoundFontCandidateInfo(
            source = source,
            absolutePath = normalized.absolutePathString(),
            exists = exists,
            regularFile = regularFile,
            readable = readable,
            sizeBytes = size,
            valid = regularFile && readable && normalized.name.endsWith(".sf2", ignoreCase = true) && (size ?: 0) > 0,
        )
    }

    private fun findProjectRoot(start: Path): Path? {
        var candidate: Path? = start
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle.kts")) || Files.isRegularFile(candidate.resolve("settings.gradle"))) return candidate
            candidate = candidate.parent
        }
        return null
    }

    private fun failureReason(projectRoot: Path?, candidates: List<SoundFontCandidateInfo>): String = when {
        projectRoot == null -> "No project root containing settings.gradle(.kts) was found from user.dir."
        candidates.isEmpty() -> "No configured, system, environment, or project SoundFont candidates were found."
        else -> "No SoundFont candidate was a readable non-empty .sf2 file."
    }
}
