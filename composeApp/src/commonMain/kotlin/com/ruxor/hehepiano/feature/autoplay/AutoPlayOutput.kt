package com.ruxor.hehepiano.feature.autoplay

internal interface AutoPlayOutput {
    fun submit(effects: List<AutoPlayEffect>)

    fun stop()
}

internal object NoAutoPlayOutput : AutoPlayOutput {
    override fun submit(effects: List<AutoPlayEffect>) = Unit

    override fun stop() = Unit
}
