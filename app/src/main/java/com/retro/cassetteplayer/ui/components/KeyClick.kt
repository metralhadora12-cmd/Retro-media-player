package com.retro.cassetteplayer.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.retro.cassetteplayer.data.AppSettings
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mechanical "clack" of cassette-deck keys, synthesised once into a small WAV (a sharp
 * click of filtered noise over a short low thump) and played through a SoundPool.
 */
object KeyClick {

    private var pool: SoundPool? = null
    private var clickId = 0
    private var flipId = 0

    /** Plays the key sound if the option is on. */
    fun play(context: Context) {
        if (!AppSettings.keyClicks.value) return
        ensureLoaded(context)
        if (clickId != 0) pool?.play(clickId, 0.7f, 0.7f, 1, 0, 1f)
    }

    /** Longer double clunk used when the tape flips sides. */
    fun playFlip(context: Context) {
        if (!AppSettings.keyClicks.value) return
        ensureLoaded(context)
        if (flipId != 0) pool?.play(flipId, 0.8f, 0.8f, 1, 0, 1f)
    }

    /** Loads the sounds ahead of time (SoundPool loads asynchronously). */
    fun preload(context: Context) {
        if (AppSettings.keyClicks.value) ensureLoaded(context)
    }

    private fun ensureLoaded(context: Context) {
        if (pool != null) return
        val soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool = soundPool
        val dir = File(context.cacheDir, "sounds").apply { mkdirs() }
        val click = File(dir, "key_click.wav")
        val flip = File(dir, "tape_flip.wav")
        runCatching {
            if (!click.exists()) writeWav(click, synthClick(seed = 7))
            if (!flip.exists()) writeWav(flip, synthClick(seed = 3) + ShortArray(SAMPLE_RATE / 8) + synthClick(seed = 11, thump = 0.9f))
            clickId = soundPool.load(click.path, 1)
            flipId = soundPool.load(flip.path, 1)
        }
    }

    private fun synthClick(seed: Int, thump: Float = 0.6f): ShortArray {
        val random = Random(seed)
        val length = SAMPLE_RATE * 45 / 1000
        var low = 0f
        return ShortArray(length) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val white = random.nextFloat() * 2 - 1
            low += (white - low) * 0.45f
            val click = (white - low) * exp(-t * 260f)
            val body = sin(2 * PI * 140 * t).toFloat() * exp(-t * 70f) * thump
            ((click * 0.8f + body * 0.5f) * 26_000f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }

    private fun writeWav(file: File, samples: ShortArray) {
        RandomAccessFile(file, "rw").use { out ->
            out.setLength(0)
            val dataBytes = samples.size * 2
            fun intLE(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
            fun shortLE(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
            out.writeBytes("RIFF"); intLE(36 + dataBytes); out.writeBytes("WAVE")
            out.writeBytes("fmt "); intLE(16); shortLE(1); shortLE(1); intLE(SAMPLE_RATE); intLE(SAMPLE_RATE * 2); shortLE(2); shortLE(16)
            out.writeBytes("data"); intLE(dataBytes)
            val bytes = ByteArray(dataBytes)
            samples.forEachIndexed { i, s ->
                bytes[i * 2] = s.toInt().toByte()
                bytes[i * 2 + 1] = (s.toInt() shr 8).toByte()
            }
            out.write(bytes)
        }
    }

    private const val SAMPLE_RATE = 44_100
}
