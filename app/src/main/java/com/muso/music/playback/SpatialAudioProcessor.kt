package com.muso.music.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Spatial audio (Echo Player and Audio): a real stereo-widening DSP inserted into the
 * audio processor chain. It converts the signal to mid/side, widens the side channel
 * and converts back - the classic "wider sound stage" effect - with hard clamping so
 * it can never clip. Stereo 16-bit PCM only; anything else passes through untouched.
 */
class SpatialAudioProcessor(
    private val width: Float = 1.35f,
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            inputAudioFormat
        } else {
            AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        val frameBytes = 4 // two 16-bit samples
        val frames = remaining / frameBytes
        val output = replaceOutputBuffer(frames * frameBytes)

        for (frame in 0 until frames) {
            val left = inputBuffer.short.toInt()
            val right = inputBuffer.short.toInt()

            val mid = (left + right) / 2
            val side = (left - right) / 2

            val widenedLeft = (mid + width * side).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            val widenedRight = (mid - width * side).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            output.putShort(widenedLeft.toShort())
            output.putShort(widenedRight.toShort())
        }

        output.flip()
    }
}
