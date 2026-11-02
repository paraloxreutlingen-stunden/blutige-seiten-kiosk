package de.paralox.kiosk.blutigeseiten

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/**
 * Einmalige Einrichtung pro Geraet (macht die Spielleitung, nicht die Spielenden):
 * 1. Berechtigungen Telefon, SMS und Kamera
 * 2. App als Standard-Telefon-App, damit der eigene Anrufbildschirm (mit Tasten fuer
 *    das easybell-Menue) verwendet wird
 *
 * Solange das nicht erledigt ist, fixiert sich die App NICHT - sonst koennten die
 * System-Dialoge fuer Berechtigungen und Standard-App gar nicht erscheinen.
 */
object KioskSetup {

    /**
     * SMS-Funktion voruebergehend abgeschaltet: Icon ausgeblendet, keine SMS-Berechtigung
     * noetig. Zum Wiedereinschalten einfach auf true setzen - der Code (SmsActivity) ist
     * unveraendert vorhanden.
     */
    const val SMS_ENABLED = false

    val requiredPermissions: Array<String> = listOfNotNull(
        Manifest.permission.CALL_PHONE,
        if (SMS_ENABLED) Manifest.permission.SEND_SMS else null,
        Manifest.permission.CAMERA
    ).toTypedArray()

    fun missingPermissions(context: Context): List<String> =
        requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun isDefaultDialer(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.defaultDialerPackage == context.packageName
        }
    }

    fun isComplete(context: Context): Boolean =
        missingPermissions(context).isEmpty() && isDefaultDialer(context)

    /** System-Dialog "Als Standard-Telefon-App festlegen?" */
    fun defaultDialerRequestIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                .createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }
}
