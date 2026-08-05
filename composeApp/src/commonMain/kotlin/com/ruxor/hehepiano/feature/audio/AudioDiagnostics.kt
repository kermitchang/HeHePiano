package com.ruxor.hehepiano.feature.audio

import kotlinx.coroutines.flow.StateFlow

internal data class SoundFontCandidateInfo(
    val source: String,
    val absolutePath: String,
    val exists: Boolean,
    val regularFile: Boolean,
    val readable: Boolean,
    val sizeBytes: Long?,
    val valid: Boolean,
)

internal data class AudioStartupInfo(
    val userDirectory: String,
    val projectRoot: String?,
    val configuredSoundFontPath: String?,
    val candidates: List<SoundFontCandidateInfo>,
    val selectedSoundFontPath: String?,
    val discoveryFailureReason: String?,
)

internal data class AudioEngineDiagnostics(
    val backend: String = "NoAudio",
    val executablePath: String? = null,
    val soundFontPath: String? = null,
    val command: List<String> = emptyList(),
    val processId: Long? = null,
    val processExitCode: Int? = null,
    val stderr: String? = null,
    val lastError: String? = null,
)

internal interface AudioDiagnosticsProvider {
    val diagnostics: StateFlow<AudioEngineDiagnostics>
}
