package com.ruxor.hehepiano.feature.midi

import com.ruxor.hehepiano.core.music.MidiNote
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.Transmitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsbMidiInputTest {
    @Test
    fun `forwards MIDI velocity and closes connection on stop`() {
        val transmitter = FakeTransmitter()
        val device = FakeMidiDevice(transmitter)
        val noteOns = mutableListOf<Pair<MidiNote, Int>>()
        val input = UsbMidiInput(deviceFinder = { device })

        input.start(
            onNoteOn = { note, velocity -> noteOns += note to velocity },
            onNoteOff = {},
        )
        transmitter.send(ShortMessage().apply { setMessage(ShortMessage.NOTE_ON, 0, 60, 37) })

        input.stop()

        assertEquals(listOf(MidiNote(60) to 37), noteOns)
        assertTrue(device.closed)
        assertTrue(transmitter.closed)
        input.stop()
    }

    @Test
    fun `closes an opened device when no transmitter is available`() {
        val device = FakeMidiDevice(transmitter = null)
        var unavailable = 0
        val input = UsbMidiInput(
            onUnavailable = { unavailable += 1 },
            deviceFinder = { device },
        )

        input.start(onNoteOn = { _, _ -> }, onNoteOff = {})

        assertEquals(1, unavailable)
        assertTrue(device.closed)
    }
}

private class FakeMidiDevice(
    private val transmitter: Transmitter?,
) : MidiDevice {
    private val info = TestMidiInfo()
    var closed: Boolean = false
        private set

    override fun getDeviceInfo(): MidiDevice.Info = info
    override fun open() = Unit
    override fun close() {
        closed = true
    }
    override fun isOpen(): Boolean = !closed
    override fun getMicrosecondPosition(): Long = 0
    override fun getMaxReceivers(): Int = 0
    override fun getMaxTransmitters(): Int = 1
    override fun getReceiver(): Receiver = error("Not an output device")
    override fun getReceivers(): List<Receiver> = emptyList()
    override fun getTransmitter(): Transmitter = transmitter ?: error("No transmitter")
    override fun getTransmitters(): List<Transmitter> = listOfNotNull(transmitter)
}

private class FakeTransmitter : Transmitter {
    private var receiver: Receiver? = null
    var closed: Boolean = false
        private set

    override fun setReceiver(receiver: Receiver?) {
        this.receiver = receiver
    }

    override fun getReceiver(): Receiver? = receiver

    override fun close() {
        closed = true
    }

    fun send(message: MidiMessage) {
        receiver?.send(message, -1)
    }
}

private class TestMidiInfo : MidiDevice.Info("Test MIDI", "HeHePiano", "Test input", "1.0")
