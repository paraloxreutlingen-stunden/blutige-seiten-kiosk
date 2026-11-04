package de.paralox.kiosk.blutigeseiten

/**
 * Eigene Muster-Sperre der App.
 *
 * Hintergrund: Waehrend die App fixiert ist, verlangt Android beim Entsperren des
 * Bildschirms kein Muster mehr. Deshalb fragt die App selbst einmal nach dem Start
 * (nach der Einrichtung) das Muster ab. Danach nicht mehr, bis die App neu gestartet wird.
 *
 * Rasterpunkte sind so nummeriert:
 *   1 2 3
 *   4 5 6
 *   7 8 9
 */
object AppLock {

    // Loesung fuer "Blutige Seiten". Bei einem neuen Spiel hier anpassen.
    const val PATTERN = "124758963"

    // Gilt fuer die Laufzeit der App: Nach einem Neustart der App ist sie wieder gesperrt.
    var unlocked = false

    // Verhindert, dass der Sperrbildschirm doppelt geoeffnet wird.
    var lockShowing = false
}
