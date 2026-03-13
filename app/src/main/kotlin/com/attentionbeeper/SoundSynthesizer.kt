package com.attentionbeeper

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.*

object SoundSynthesizer {

    private const val TAG = "SoundSynthesizer"
    private const val SAMPLE_RATE = 44100

    fun play(soundId: String) {
        val samples = generate(soundId)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Log.d(TAG, "Playing sound: $soundId")
        // Release after playback in a background thread
        Thread {
            val durationMs = (samples.size.toLong() * 1000L) / SAMPLE_RATE
            Thread.sleep(durationMs + 100)
            track.stop()
            track.release()
        }.start()
    }

    fun generate(soundId: String): ShortArray = when (soundId) {
        "digital" -> digital()
        "ping"    -> ping()
        "pluck"   -> pluck()
        "ding"    -> ding()
        "danger"  -> danger()
        "chime"   -> chime()
        "bell"    -> bell()
        "alert"   -> alert()
        "drop"    -> drop()
        "bubble"  -> bubble()
        "woodblock" -> woodblock()
        "chord"   -> chord()
        "blip"    -> blip()
        "whoosh"  -> whoosh()
        "click"   -> click()
        else      -> digital()
    }

    // --- Helpers ---

    private fun sine(freq: Double, durationMs: Int, ampFraction: Double = 0.8): ShortArray {
        val n = (SAMPLE_RATE * durationMs / 1000.0).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (sin(2.0 * PI * freq * t) * ampFraction * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun envelope(samples: ShortArray, attackMs: Int = 5, releaseMs: Int = 50): ShortArray {
        val attackSamples = (SAMPLE_RATE * attackMs / 1000.0).toInt()
        val releaseSamples = (SAMPLE_RATE * releaseMs / 1000.0).toInt()
        return ShortArray(samples.size) { i ->
            val gain = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i >= samples.size - releaseSamples ->
                    (samples.size - i).toDouble() / releaseSamples
                else -> 1.0
            }
            (samples[i] * gain).toInt().toShort()
        }
    }

    private fun mix(vararg arrays: ShortArray): ShortArray {
        val len = arrays.maxOf { it.size }
        val scale = 1.0 / arrays.size
        return ShortArray(len) { i ->
            var v = 0.0
            for (a in arrays) {
                if (i < a.size) v += a[i] * scale
            }
            v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun silence(ms: Int) = ShortArray((SAMPLE_RATE * ms / 1000.0).toInt())

    private fun concat(vararg arrays: ShortArray): ShortArray {
        val total = arrays.sumOf { it.size }
        val out = ShortArray(total)
        var pos = 0
        for (a in arrays) { a.copyInto(out, pos); pos += a.size }
        return out
    }

    // --- Sound generators ---

    /** Square-wave beep at 880 Hz, 150ms */
    private fun digital(): ShortArray {
        val n = (SAMPLE_RATE * 150 / 1000.0).toInt()
        return envelope(ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val v = if (sin(2.0 * PI * 880.0 * t) >= 0) 1 else -1
            (v * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }, releaseMs = 30)
    }

    /** Pure sine at 1200 Hz, 80ms, quick fade */
    private fun ping(): ShortArray =
        envelope(sine(1200.0, 80), attackMs = 2, releaseMs = 60)

    /** Plucked string simulation: decaying noise filtered by a sine */
    private fun pluck(): ShortArray {
        val n = (SAMPLE_RATE * 300 / 1000.0).toInt()
        val freq = 440.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 10.0)
            val v = sin(2.0 * PI * freq * t) * decay
            (v * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Bell-like tone at 523 Hz (C5) with long decay */
    private fun ding(): ShortArray {
        val n = (SAMPLE_RATE * 600 / 1000.0).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 4.0)
            val v = (sin(2.0 * PI * 523.0 * t) + 0.3 * sin(2.0 * PI * 1046.0 * t)) * decay
            (v * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Low-pitched descending square wave */
    private fun danger(): ShortArray {
        val n = (SAMPLE_RATE * 400 / 1000.0).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 400.0 - t * 500.0  // descend from 400 to 200 Hz
            val v = if (sin(2.0 * PI * freq.coerceAtLeast(100.0) * t) >= 0) 1 else -1
            (v * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Three-tone ascending chime */
    private fun chime(): ShortArray {
        val t1 = envelope(sine(523.0, 200), releaseMs = 80)
        val t2 = envelope(sine(659.0, 200), releaseMs = 80)
        val t3 = envelope(sine(784.0, 300), releaseMs = 150)
        return concat(t1, t2, t3)
    }

    /** Sine at 440 Hz with slow exponential decay, 500ms */
    private fun bell(): ShortArray {
        val n = (SAMPLE_RATE * 500 / 1000.0).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 5.0)
            val v = sin(2.0 * PI * 440.0 * t) * decay
            (v * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Two rapid beeps (alerting pattern) */
    private fun alert(): ShortArray {
        val beep = envelope(sine(1000.0, 80), releaseMs = 20)
        return concat(beep, silence(60), beep)
    }

    /** Pitch drops from 600 Hz to 150 Hz */
    private fun drop(): ShortArray {
        val n = (SAMPLE_RATE * 300 / 1000.0).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 600.0 * exp(-t * 5.0) + 150.0
            val v = sin(2.0 * PI * freq * t)
            (v * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Rising frequency bubble pop */
    private fun bubble(): ShortArray {
        val n = (SAMPLE_RATE * 150 / 1000.0).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 200.0 + 1000.0 * t
            val decay = exp(-t * 20.0)
            val v = sin(2.0 * PI * freq * t) * decay
            (v * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Short noise burst — simulates a wooden hit */
    private fun woodblock(): ShortArray {
        val n = (SAMPLE_RATE * 60 / 1000.0).toInt()
        val random = java.util.Random(42)
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 60.0)
            val noise = random.nextGaussian()
            val tone = sin(2.0 * PI * 900.0 * t)
            val v = (noise * 0.3 + tone * 0.7) * decay
            (v * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Major chord: root + major third + fifth */
    private fun chord(): ShortArray {
        val n = (SAMPLE_RATE * 400 / 1000.0).toInt()
        val samples = ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 3.0)
            val v = (sin(2.0 * PI * 261.63 * t) +
                     sin(2.0 * PI * 329.63 * t) +
                     sin(2.0 * PI * 392.00 * t)) / 3.0 * decay
            (v * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
        return envelope(samples, attackMs = 10, releaseMs = 100)
    }

    /** Very short high-frequency blip (30ms) */
    private fun blip(): ShortArray =
        envelope(sine(2000.0, 30), attackMs = 2, releaseMs = 20)

    /** Bandpass noise sweeping upward */
    private fun whoosh(): ShortArray {
        val n = (SAMPLE_RATE * 350 / 1000.0).toInt()
        val random = java.util.Random(7)
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = sin(PI * t / (n.toDouble() / SAMPLE_RATE))
            val freq = 100.0 + 4000.0 * t
            val v = sin(2.0 * PI * freq * t + random.nextGaussian() * 0.1) * env
            (v * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Very short transient click (10ms) */
    private fun click(): ShortArray {
        val n = (SAMPLE_RATE * 10 / 1000.0).toInt()
        val random = java.util.Random(13)
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 500.0)
            val v = random.nextGaussian() * decay
            (v * 0.9 * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
