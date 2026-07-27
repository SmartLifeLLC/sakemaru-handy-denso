package biz.smt_life.android.core.designsystem.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
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
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(soundAttributes()).build()
        successSoundId = soundPool!!.load(context, biz.smt_life.android.core.designsystem.R.raw.success, 1)
        errorSoundId = soundPool!!.load(context, biz.smt_life.android.core.designsystem.R.raw.error, 1)
    }

    fun playBeep() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    fun playTick() {
        playTone(ToneGenerator.TONE_PROP_ACK, 30)
    }

    fun playSuccess() {
        val streamId = soundPool?.play(successSoundId, 1f, 1f, 1, 0, 1f) ?: 0
        if (streamId == 0) {
            playTone(ToneGenerator.TONE_PROP_ACK, 100)
        }
    }

    fun playError() {
        val streamId = soundPool?.play(errorSoundId, 1f, 1f, 1, 0, 1f) ?: 0
        if (streamId == 0) {
            playTone(ToneGenerator.TONE_SUP_ERROR, 200)
        }
    }

    fun playErrorWithVibration(context: Context) {
        init(context.applicationContext)
        if (!playRaw(context.applicationContext, biz.smt_life.android.core.designsystem.R.raw.error)) {
            playError()
        }
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

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {}
    }

    private fun playRaw(context: Context, resId: Int): Boolean {
        var player: MediaPlayer? = null
        return try {
            val descriptor = context.resources.openRawResourceFd(resId) ?: return false
            descriptor.use {
                player = MediaPlayer().apply {
                    setAudioAttributes(soundAttributes())
                    setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    setOnCompletionListener { mediaPlayer -> mediaPlayer.release() }
                    setOnErrorListener { mediaPlayer, _, _ ->
                        mediaPlayer.release()
                        true
                    }
                    prepare()
                    start()
                }
            }
            true
        } catch (e: Exception) {
            player?.release()
            false
        }
    }

    private fun soundAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        soundPool?.release()
        soundPool = null
        initialized = false
    }
}
