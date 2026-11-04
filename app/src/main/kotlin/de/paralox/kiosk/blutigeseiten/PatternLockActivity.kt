package de.paralox.kiosk.blutigeseiten

import android.os.Bundle
import de.paralox.kiosk.blutigeseiten.databinding.ActivityPatternLockBinding

/**
 * Sperrbildschirm der App (siehe AppLock). Erscheint einmal nach dem Start der App,
 * sobald die Einrichtung erledigt ist. Richtiges Muster -> weiter zum Startbildschirm,
 * falsches Muster -> rot, Vibration, nochmal. Kein Text, kein Zurueck.
 */
class PatternLockActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityPatternLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatternLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.patternView.onPatternEntered = { entered ->
            if (entered == AppLock.PATTERN) {
                AppLock.unlocked = true
                binding.patternView.clear()
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                binding.patternView.showError()
                Feedback.error(binding.patternView)
            }
        }
    }

    override fun onDestroy() {
        AppLock.lockShowing = false
        super.onDestroy()
    }

    // Zurueck-Taste umgeht die Sperre nicht.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // bewusst leer
    }
}
