package com.ruxor.kermitpiano.app

internal enum class TopBarLayoutMode {
    Wide,
    Compact,
    Narrow,
}

/** Keeps responsive breakpoints out of the Compose tree. */
internal object TopBarLayoutPolicy {
    const val wideMinimumWidthDp = 1_120
    const val compactMinimumWidthDp = 760

    fun modeFor(widthDp: Int): TopBarLayoutMode = when {
        widthDp >= wideMinimumWidthDp -> TopBarLayoutMode.Wide
        widthDp >= compactMinimumWidthDp -> TopBarLayoutMode.Compact
        else -> TopBarLayoutMode.Narrow
    }

    fun songTitleLimit(mode: TopBarLayoutMode): Int = when (mode) {
        TopBarLayoutMode.Wide -> 280
        TopBarLayoutMode.Compact -> 150
        TopBarLayoutMode.Narrow -> 0
    }
}
