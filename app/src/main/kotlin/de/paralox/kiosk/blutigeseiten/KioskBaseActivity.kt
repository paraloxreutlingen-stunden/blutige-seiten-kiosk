package de.paralox.kiosk.blutigeseiten

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

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
    }

    override fun onResume() {
        super.onResume()
        startPinningIfNeeded()
    }

    private fun startPinningIfNeeded() {
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
