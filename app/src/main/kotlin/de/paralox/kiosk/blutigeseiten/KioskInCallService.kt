package de.paralox.kiosk.blutigeseiten

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService

/**
 * Wird vom System gebunden, sobald diese App Standard-Telefon-App ist. Dann laufen alle
 * Anrufe ueber diese App statt ueber Samsungs Telefon-App - und der Anrufbildschirm gehoert
 * zur Kiosk-App. Dadurch blockiert die App-Fixierung ihn nicht mehr, und die Spielenden
 * koennen waehrend des Anrufs Tasten (z. B. "1" oder "2" im easybell-Menue) druecken.
 *
 * Bewusst minimal gehalten:
 * - Eingehende Anrufe werden sofort abgelehnt (im Spiel gibt es keine).
 * - Kein Halten, kein zweiter Anruf, keine Konferenz, kein Bluetooth-Umschalten.
 * Den eigentlichen Bildschirm oeffnet CallActivity direkt nach dem Waehlen.
 */
class KioskInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.service = this

        if (isIncoming(call)) {
            call.reject(false, null)
            return
        }
        CallManager.setCall(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (CallManager.call === call) {
            CallManager.setCall(null)
        }
    }

    override fun onDestroy() {
        if (CallManager.service === this) {
            CallManager.service = null
        }
        super.onDestroy()
    }

    private fun isIncoming(call: Call): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            call.details.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            call.state == Call.STATE_RINGING
        }
    }
}
