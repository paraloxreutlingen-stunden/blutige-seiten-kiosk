package de.paralox.kiosk.blutigeseiten

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View

/**
 * Rueckmeldung ohne Text (die App soll sprachunabhaengig sein):
 * kurzes Schuetteln der betroffenen Anzeige plus kurze Vibration.
 */
object Feedback {

    fun error(view: View) {
        val d = view.resources.displayMetrics.density * 10f
        view.animate().cancel()
        view.translationX = 0f
        view.animate().translationX(d).setDuration(50).withEndAction {
            view.animate().translationX(-d).setDuration(80).withEndAction {
                view.animate().translationX(d * 0.6f).setDuration(70).withEndAction {
                    view.animate().translationX(0f).setDuration(60).start()
                }.start()
            }.start()
        }.start()
        vibrate(view.context)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
            // Vibration ist nur ein Extra
        }
    }
}
