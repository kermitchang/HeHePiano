package com.ruxor.kermitpiano.feature.midi

import com.ruxor.kermitpiano.core.music.MidiNote
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.Transmitter

/**
 * USB MIDI keyboard input for JVM desktops.
 *
 * Scans the system for a device matching [deviceNameContains] (default AK490),
 * opens it, and forwards NoteOn / NoteOff messages to the given callbacks.
 * Safe to create even when no MIDI device is present — [start] then reports
 * the failure via [onUnavailable] instead of throwing.
 */
internal class UsbMidiInput(
    private val deviceNameContains: String = "AK490",
    private val onUnavailable: (String) -> Unit = {},
    private val deviceFinder: (() -> MidiDevice?)? = null,
) : MidiInput {

    private val running = AtomicBoolean(false)
    private val lifecycleLock = Any()
    private var device: MidiDevice? = null
    private var transmitter: Transmitter? = null

    override fun start(
        onNoteOn: (MidiNote, Int) -> Unit,
        onNoteOff: (MidiNote) -> Unit,
        onPitchBend: (Int) -> Unit,
        onControlChange: (Int, Int) -> Unit,
    ) {
        if (!running.compareAndSet(false, true)) return
        synchronized(lifecycleLock) {
            if (!running.get()) return
            val matched = runCatching { deviceFinder?.invoke() ?: findDevice() }.getOrNull()
            if (matched == null) {
                val message = "未找到 USB MIDI 琴（搜尋: $deviceNameContains）。請確認琴已連接。"
                println("[UsbMidiInput] $message")
                onUnavailable(message)
                running.set(false)
                return
            }
            try {
                device = matched
                matched.open()

                val inputReceiver = object : Receiver {
                    override fun send(message: MidiMessage, timeStamp: Long) {
                        val short = message as? ShortMessage ?: return
                        when (short.command) {
                            ShortMessage.NOTE_ON -> {
                                val velocity = short.data2
                                if (velocity == 0) {
                                    onNoteOff(MidiNote(short.data1))
                                } else {
                                    onNoteOn(MidiNote(short.data1), velocity)
                                }
                            }
                            ShortMessage.NOTE_OFF -> onNoteOff(MidiNote(short.data1))
                            ShortMessage.PITCH_BEND -> {
                                val value = (short.data2 shl 7) or short.data1
                                onPitchBend(value)
                            }
                            ShortMessage.CONTROL_CHANGE -> onControlChange(short.data1, short.data2)
                        }
                    }

                    override fun close() = Unit
                }

                // A source device exposes a Transmitter; its receiver is our input hook.
                val tx = runCatching { matched.transmitter }.getOrNull()
                if (tx == null) {
                    val message = "MIDI 裝置 ${matched.deviceInfo.name} 沒有輸入端。"
                    println("[UsbMidiInput] $message")
                    cleanupConnection()
                    onUnavailable(message)
                    running.set(false)
                    return
                }
                tx.receiver = inputReceiver
                transmitter = tx
                val friendlyName = matched.deviceInfo.description
                    ?.substringBefore(',')
                    ?.trim()
                    ?.ifBlank { matched.deviceInfo.name }
                    ?: matched.deviceInfo.name
                println("[UsbMidiInput] 已連接 $friendlyName (${matched.deviceInfo.name})")
            } catch (e: Exception) {
                cleanupConnection()
                println("[UsbMidiInput] 開啟失敗: ${e.message}")
                onUnavailable("無法開啟 MIDI 裝置: ${e.message}")
                running.set(false)
            }
        }
    }

    override fun stop() {
        synchronized(lifecycleLock) {
            if (!running.compareAndSet(true, false)) return
            cleanupConnection()
            println("[UsbMidiInput] 已停止")
        }
    }

    private fun cleanupConnection() {
        runCatching { transmitter?.receiver = null }
        runCatching { transmitter?.close() }
        runCatching { device?.close() }
        transmitter = null
        device = null
    }

    private fun findDevice(): MidiDevice? {
        val infos = MidiSystem.getMidiDeviceInfo()
        return infos
            .asSequence()
            .mapNotNull { info ->
                val device = runCatching { MidiSystem.getMidiDevice(info) }.getOrNull() ?: return@mapNotNull null
                device to info
            }
            .filter { (device, info) ->
                val isInput = runCatching { device.maxTransmitters }.getOrElse { 0 } != 0
                val full = "${info.name} ${info.vendor ?: ""} ${info.description ?: ""}"
                isInput && full.contains(deviceNameContains, ignoreCase = true)
            }
            .map { it.first }
            .firstOrNull()
    }

    companion object {
        /** Lists all available MIDI input devices for diagnostics. */
        fun availableDevices(): List<String> =
            MidiSystem.getMidiDeviceInfo()
                .map { "${it.name} (${it.vendor ?: "unknown"})" }
    }
}
