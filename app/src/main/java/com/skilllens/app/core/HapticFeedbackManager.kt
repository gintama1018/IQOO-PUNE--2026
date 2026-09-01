package com.skilllens.app.core

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.skilllens.app.taskengine.HapticPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HapticFeedbackManager — triggers tactile feedback for physical task events.
 *
 * Patterns:
 * - TICK: Subtle tap when a component is registered
 * - SUCCESS: Double-pulse confirmation when a step is verified
 * - ERROR: Warning buzz when an incorrect action is detected
 * - COMPLETION: Extended rhythmic celebration when full task is finished
 */
@Singleton
class HapticFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(pattern: HapticPattern) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            when (pattern) {
                HapticPattern.NONE -> { /* No-op */ }

                HapticPattern.TICK -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(20L)
                    }
                }

                HapticPattern.SUCCESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 50, 60, 80)
                        val amplitudes = intArrayOf(0, 180, 0, 255)
                        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 50, 60, 80), -1)
                    }
                }

                HapticPattern.ERROR -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 120, 80, 160)
                        val amplitudes = intArrayOf(0, 255, 0, 255)
                        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 120, 80, 160), -1)
                    }
                }

                HapticPattern.COMPLETION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 80, 50, 80, 50, 150)
                        val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 80, 50, 80, 50, 150), -1)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "HapticFeedbackManager: Failed to play vibration")
        }
    }
}
