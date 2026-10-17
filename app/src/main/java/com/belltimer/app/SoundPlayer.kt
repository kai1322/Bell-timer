package com.belltimer.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlin.math.sin

/**
 * Generates a short "ปิ๊ง" (ping) sound using AudioTrack sine wave.
 * Plays the requested number of times (1–3) with a short gap.
 */
class SoundPlayer(private val context: Context) {

    fun play(beepCount: Int, volumePercent: Int) {
        val count = beepCount.coerceIn(1, 3)
        val volume = (volumePercent.coerceIn(0, 100) / 100f)

        Thread {
            try {
                for (i in 0 until count) {
                    playSinglePing(volume)
                    if (i < count - 1) {
                        Thread.sleep(180) // gap between pings
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to ToneGenerator
                fallbackTone(count, volumePercent)
            }
        }.start()
    }

    private fun playSinglePing(volume: Float) {
        val sampleRate = 44100
        val durationMs = 120
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val frequency = 880.0 // A5 note – bright "ping"

        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Envelope: quick attack + exponential decay for "ping" character
            val envelope = Math.exp(-t * 18.0)
            val sample = (sin(2.0 * Math.PI * frequency * t) * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.setVolume(volume)
        audioTrack.play()

        // Wait for playback to finish
        Thread.sleep(durationMs + 30L)
        audioTrack.stop()
        audioTrack.release()
    }

    private fun fallbackTone(count: Int, volumePercent: Int) {
        try {
            val stream = AudioManager.STREAM_ALARM
            val tg = ToneGenerator(stream, volumePercent.coerceIn(0, 100))
            for (i in 0 until count) {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                Thread.sleep(200)
            }
            tg.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
