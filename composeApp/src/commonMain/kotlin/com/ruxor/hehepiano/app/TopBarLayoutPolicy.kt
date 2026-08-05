package com.ruxor.hehepiano.app

internal enum class TopBarLayoutMode {
    Wide,
    Compact,
    Narrow,
}

internal data class TopBarLayoutFeatures(
    val showsOpenMidi: Boolean,
    val showsSongTitle: Boolean,
    val exposesLocalLibrary: Boolean,
)

/** Keeps responsive breakpoints out of the Compose tree. */
internal object TopBarLayoutPolicy {
    // Wide mode contains eight control groups, including segmented speed and piano views.
    const val wideMinimumWidthDp = 1_600
    const val compactMinimumWidthDp = 1_000

    fun modeFor(widthDp: Int): TopBarLayoutMode = when {
        widthDp >= wideMinimumWidthDp -> TopBarLayoutMode.Wide
        widthDp >= compactMinimumWidthDp -> TopBarLayoutMode.Compact
        else -> TopBarLayoutMode.Narrow
    }

    fun songTitleLimit(mode: TopBarLayoutMode): Int = when (mode) {
        TopBarLayoutMode.Wide -> 280
        TopBarLayoutMode.Compact -> 0
        TopBarLayoutMode.Narrow -> 0
    }

    fun featuresFor(mode: TopBarLayoutMode): TopBarLayoutFeatures = when (mode) {
        TopBarLayoutMode.Wide -> TopBarLayoutFeatures(showsOpenMidi = true, showsSongTitle = true, exposesLocalLibrary = true)
        TopBarLayoutMode.Compact -> TopBarLayoutFeatures(showsOpenMidi = true, showsSongTitle = false, exposesLocalLibrary = true)
        TopBarLayoutMode.Narrow -> TopBarLayoutFeatures(showsOpenMidi = true, showsSongTitle = false, exposesLocalLibrary = true)
    }
}
