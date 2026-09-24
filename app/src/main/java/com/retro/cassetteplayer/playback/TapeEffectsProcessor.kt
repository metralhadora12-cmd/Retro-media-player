package com.retro.cassetteplayer.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.retro.cassetteplayer.data.AppSettings
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.sin

/**
 * Optional "worn tape" sound on 16-bit PCM: soft tape hiss and wow & flutter (slow and
 * fast pitch wobble, made with a modulated delay line). Both fade in and out smoothly
 * when toggled. Hi-res tracks sent to the float output skip audio processors entirely.
 */
@UnstableApi
class TapeEffectsProcessor : BaseAudioProcessor() {

    private var ring = FloatArray(0)
    private var writePos = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0
    private var wowMix = 0f
    private var hissMix = 0f
    private var noiseState = 0x2545F491
    private var hissLow = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val channels = inputAudioFormat.channelCount
        val rate = inputAudioFormat.sampleRate.toDouble()
        if (ring.size != channels * RING) {
            ring = FloatArray(channels * RING)
            hissLow = FloatArray(channels)
            writePos = 0
        }
        val output = replaceOutputBuffer(size)
        val wowTarget = if (AppSettings.wowFlutter.value) 1f else 0f
        val hissTarget = if (AppSettings.tapeHiss.value) 1f else 0f
        // Pitch deviation ≈ 2π·f·A / rate: about 0.25% wow at 0.55 Hz, 0.06% flutter at 6.5 Hz
        val wowDepth = 0.0025 * rate / (2 * PI * WOW_HZ)
        val flutterDepth = 0.0006 * rate / (2 * PI * FLUTTER_HZ)
        val wowStep = 2 * PI * WOW_HZ / rate
        val flutterStep = 2 * PI * FLUTTER_HZ / rate
        val frameBytes = 2 * channels

        while (inputBuffer.remaining() >= frameBytes) {
            wowMix += (wowTarget - wowMix) * SMOOTHING
            hissMix += (hissTarget - hissMix) * SMOOTHING
            for (c in 0 until channels) ring[writePos * channels + c] = inputBuffer.short.toFloat()

            val delay = BASE_DELAY + wowMix * (wowDepth * sin(wowPhase) + flutterDepth * sin(flutterPhase))
            var readPos = writePos - delay
            while (readPos < 0) readPos += RING
            val i0 = readPos.toInt() % RING
            val i1 = (i0 + 1) % RING
            val frac = (readPos - readPos.toInt()).toFloat()

            for (c in 0 until channels) {
                var sample = ring[i0 * channels + c] * (1 - frac) + ring[i1 * channels + c] * frac
                if (hissMix > 0.001f) {
                    // High-passed white noise sounds like tape hiss
                    val white = nextNoise()
                    val high = white - hissLow[c]
                    hissLow[c] += (white - hissLow[c]) * 0.35f
                    sample += high * HISS_LEVEL * hissMix
                }
                output.putShort(sample.coerceIn(-32768f, 32767f).toInt().toShort())
            }
            writePos = (writePos + 1) % RING
            wowPhase = (wowPhase + wowStep) % (2 * PI)
            flutterPhase = (flutterPhase + flutterStep) % (2 * PI)
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    override fun onFlush() {
        ring.fill(0f)
        hissLow.fill(0f)
        writePos = 0
    }

    override fun onReset() {
        ring = FloatArray(0)
        hissLow = FloatArray(0)
        writePos = 0
    }

    /** xorshift noise in -1..1 */
    private fun nextNoise(): Float {
        var x = noiseState
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        noiseState = x
        return x / 2_147_483_648f
    }

    private companion object {
        const val RING = 1024
        const val BASE_DELAY = 200.0
        const val WOW_HZ = 0.55
        const val FLUTTER_HZ = 6.5
        const val HISS_LEVEL = 260f
        const val SMOOTHING = 0.0005f
    }
}
