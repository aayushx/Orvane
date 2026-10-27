package com.orvane.app.core.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Plays looping white-noise assets during focus sessions.
 * Bundled loops in assets/whitenoise: rain, waves, forest, cafe, brown.
 */
class WhiteNoisePlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private var current: String? = null

    val available: List<String> = listOf(
        NOISE_RAIN,
        NOISE_WAVES,
        NOISE_FOREST,
        NOISE_CAFE,
        NOISE_BROWN,
    )

    fun play(noise: String, volume: Float = 0.8f) {
        stop()
        val asset = "whitenoise/$noise.wav"
        current = noise
        try {
            player = MediaPlayer().apply {
                context.assets.openFd(asset).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            android.util.Log.w("OrvaneNoise", "White noise failed to play", e)
            player = null
        }
    }

    fun setVolume(volume: Float) {
        player?.setVolume(volume, volume)
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    fun resume() {
        runCatching { player?.start() }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        current = null
    }

    fun currentNoise(): String? = current

    companion object {
        const val NOISE_RAIN = "rain"
        const val NOISE_WAVES = "waves"
        const val NOISE_FOREST = "forest"
        const val NOISE_CAFE = "cafe"
        const val NOISE_BROWN = "brown"
    }
}
