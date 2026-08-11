package com.appathy.kingstack.data

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator

class Feedback(context: Context) {

    private val vibrator: Vibrator? = try {
        context.getSystemService(Vibrator::class.java)
    } catch (e: Exception) {
        null
    }

    private var tone: ToneGenerator? = null

    private fun tone(): ToneGenerator? {
        if (tone == null) {
            tone = try {
                ToneGenerator(AudioManager.STREAM_SYSTEM, 40)
            } catch (e: Exception) {
                null
            }
        }
        return tone
    }

    fun move(settings: Settings) {
        if (settings.vibration) vibrate(12)
        if (settings.sound) beep(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    fun deal(settings: Settings) {
        if (settings.vibration) vibrate(20)
        if (settings.sound) beep(ToneGenerator.TONE_PROP_BEEP2, 60)
    }

    fun complete(settings: Settings) {
        if (settings.vibration) vibrate(60)
        if (settings.sound) beep(ToneGenerator.TONE_PROP_ACK, 150)
    }

    fun reject(settings: Settings) {
        if (settings.vibration) vibrate(8)
    }

    private fun vibrate(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // 端末によっては振動不可。無視する。
        }
    }

    private fun beep(type: Int, duration: Int) {
        try {
            tone()?.startTone(type, duration)
        } catch (e: Exception) {
            // 無視する。
        }
    }
}
