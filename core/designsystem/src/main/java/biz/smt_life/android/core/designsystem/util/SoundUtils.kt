package biz.smt_life.android.core.designsystem.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundUtils {

    private var toneGenerator: ToneGenerator? = null
    private var soundPool: SoundPool? = null
    private var successSoundId: Int = 0
    private var errorSoundId: Int = 0
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
        successSoundId = soundPool!!.load(context, biz.smt_life.android.core.designsystem.R.raw.success, 1)
        errorSoundId = soundPool!!.load(context, biz.smt_life.android.core.designsystem.R.raw.error, 1)
    }

    fun playBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (e: Exception) {}
    }

    fun playTick() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 30)
        } catch (e: Exception) {}
    }

    fun playSuccess() {
        soundPool?.play(successSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playError() {
        soundPool?.play(errorSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playErrorWithVibration(context: Context) {
        playError()
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        soundPool?.release()
        soundPool = null
        initialized = false
    }
}
