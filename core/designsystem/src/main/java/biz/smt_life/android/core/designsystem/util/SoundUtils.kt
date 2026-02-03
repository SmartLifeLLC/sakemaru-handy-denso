package biz.smt_life.android.core.designsystem.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Utility object for playing sounds in the app.
 */
object SoundUtils {

    private var toneGenerator: ToneGenerator? = null

    /**
     * Plays a beep sound when a button is pressed.
     */
    fun playBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (e: Exception) {
            // Ignore if tone generation fails
        }
    }

    /**
     * Releases the tone generator resources.
     * Call this when the app is being destroyed.
     */
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
