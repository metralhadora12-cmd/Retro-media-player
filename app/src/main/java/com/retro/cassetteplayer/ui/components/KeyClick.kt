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
 * click of filtered noise over a short low thump) and played through a SoundPool on the
 * media stream, so it follows the music volume and isn't muted by Do Not Disturb.
 */
object KeyClick {

    private var pool: SoundPool? = null
    private var clickId = 0
    private var flipId = 0
    private val loaded = HashSet<Int>()
    /** Sound requested before it finished loading: played as soon as it is ready. */
    private var pending = 0

    /** Plays the key sound if the option is on. */
    fun play(context: Context) {
        if (!AppSettings.keyClicks.value) return
        ensureLoaded(context)
        playOrQueue(clickId, CLICK_VOLUME)
    }

    /** Longer double clunk used when the tape flips sides. */
    fun playFlip(context: Context) {
        if (!AppSettings.keyClicks.value) return
        ensureLoaded(context)
        playOrQueue(flipId, FLIP_VOLUME)
    }

    /** Loads the sounds ahead of time (SoundPool loads asynchronously). */
    fun preload(context: Context) {
        if (AppSettings.keyClicks.value) ensureLoaded(context)
    }

    private fun playOrQueue(id: Int, volume: Float) {
        if (id == 0) return
        synchronized(loaded) {
            if (id !in loaded) {
                pending = id
                return
            }
        }
        pool?.play(id, volume, volume, 1, 0, 1f)
    }

    private fun ensureLoaded(context: Context) {
        if (pool != null) return
        val soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        soundPool.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            val play = synchronized(loaded) {
                loaded += sampleId
                (pending == sampleId).also { if (it) pending = 0 }
            }
            if (play) sp.play(sampleId, CLICK_VOLUME, CLICK_VOLUME, 1, 0, 1f)
        }
        pool = soundPool
        val dir = File(context.applicationContext.cacheDir, "sounds").apply { mkdirs() }
        val click = File(dir, "key_click_v2.wav")
        val flip = File(dir, "tape_flip_v2.wav")
        runCatching {
            if (!click.exists() || click.length() < 100) writeWav(click, synthClick(seed = 7))
            if (!flip.exists() || flip.length() < 100) {
                writeWav(flip, synthClick(seed = 3) + ShortArray(SAMPLE_RATE / 8) + synthClick(seed = 11, thump = 1f))
            }
            clickId = soundPool.load(click.path, 1)
            flipId = soundPool.load(flip.path, 1)
        }
    }

    private fun synthClick(seed: Int, thump: Float = 0.8f): ShortArray {
        val random = Random(seed)
        val length = SAMPLE_RATE * 70 / 1000
        var low = 0f
        return ShortArray(length) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val white = random.nextFloat() * 2 - 1
            low += (white - low) * 0.3f
            // Sharp plastic tick, then the key bottoming out a few ms later
            val tick = (white - low) * exp(-t * 180f)
            val bottom = if (t > 0.012f) (white - low) * 0.6f * exp(-(t - 0.012f) * 220f) else 0f
            val body = sin(2 * PI * 120 * t).toFloat() * exp(-t * 45f) * thump
            ((tick + bottom + body * 0.7f) * 22_000f).coerceIn(-32768f, 32767f).toInt().toShort()
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
    private const val CLICK_VOLUME = 1f
    private const val FLIP_VOLUME = 1f
}
