package de.paralox.kiosk.blutigeseiten

import android.telecom.Call
import android.telecom.InCallService

/**
 * Verbindet den InCallService (bekommt vom System den laufenden Anruf) mit dem
 * Anrufbildschirm (InCallActivity). Es gibt in diesem Spiel immer hoechstens einen
 * ausgehenden Anruf gleichzeitig - eingehende Anrufe werden sofort abgelehnt.
 */
object CallManager {

    var service: InCallService? = null

    var call: Call? = null
        private set

    private val observers = mutableListOf<() -> Unit>()

    fun setCall(newCall: Call?) {
        call = newCall
        observers.toList().forEach { it() }
    }

    fun addObserver(observer: () -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: () -> Unit) {
        observers.remove(observer)
    }
}
