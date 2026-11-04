package de.paralox.kiosk.blutigeseiten

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Gemeinsame Basis fuer alle Bildschirme dieser App.
 *
 * Startet automatisch die Android-Bildschirmfixierung (Screen Pinning / App-Fixierung),
 * sobald eine Activity in den Vordergrund kommt. Dadurch koennen Spieler*innen das Geraet
 * nicht verlassen (kein Homescreen, keine andere App, keine Benachrichtigungen oeffnen).
 *
 * Wichtig: Screen Pinning ist eine Bordmittel-Funktion von Android (seit Android 5.0),
 * es wird KEINE zusaetzliche Software (MDM, Geraeteverwaltung) benoetigt.
 *
 * Damit das Loesen der Fixierung spaeter wieder das Entsperrmuster verlangt, muss einmalig
 * auf dem Geraet aktiviert werden:
 * Einstellungen > Sicherheit > Weitere Sicherheitseinstellungen > App-Fixierung >
 * "Beim Loesen der Fixierung nach Displaysperre fragen" (Formulierung je nach Android-Version
 * leicht abweichend, siehe Begleittext).
 */
abstract class KioskBaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inhalt darf neben die Kamera-Aussparung oben reichen - so sitzt die eigene
        // Statusleiste links und rechts der Kamera, wie bei der echten Leiste.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideRealStatusBar()
        startPinningIfNeeded()
        showPatternLockIfNeeded()
    }

    /**
     * Einmal nach dem Start der App (sobald die Einrichtung erledigt ist) muss das
     * Entsperrmuster eingegeben werden - siehe AppLock. Nie waehrend eines Anrufs.
     */
    protected fun showPatternLockIfNeeded() {
        if (AppLock.unlocked || AppLock.lockShowing) return
        if (this is PatternLockActivity || this is InCallActivity) return
        if (!KioskSetup.isComplete(this)) return
        AppLock.lockShowing = true
        startActivity(Intent(this, PatternLockActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    /**
     * Die echte Android-Statusleiste wird ausgeblendet - die App zeigt ihre eigene
     * (StatusBarView), damit es nie zwei Leisten gibt und sie auf allen Geraeten gleich aussieht.
     */
    private fun hideRealStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    protected fun startPinningIfNeeded() {
        // Erst fixieren, wenn die Einrichtung (Berechtigungen + Standard-Telefon-App) fertig
        // ist - sonst koennten die System-Dialoge dafuer nicht erscheinen. Siehe KioskSetup.
        if (!KioskSetup.isComplete(this)) return

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // Nur anstossen, wenn die App nicht schon fixiert ist - vermeidet unnoetige Systemdialoge.
        if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            try {
                startLockTask()
            } catch (_: Exception) {
                // Falls Screen Pinning auf diesem Geraet/Android-Stand nicht verfuegbar ist,
                // laeuft die App trotzdem normal weiter - nur eben ohne Fixierung.
            }
        }
    }
}
