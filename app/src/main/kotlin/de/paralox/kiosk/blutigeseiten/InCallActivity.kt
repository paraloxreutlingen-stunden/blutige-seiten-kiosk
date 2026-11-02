package de.paralox.kiosk.blutigeseiten

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import de.paralox.kiosk.blutigeseiten.databinding.ActivityInCallBinding

/**
 * Eigener Anrufbildschirm (statt Samsungs Telefon-App, die waehrend der App-Fixierung
 * blockiert ist).
 *
 * Kann bewusst nur das, was im Spiel gebraucht wird:
 * - Status anzeigen (waehlt / Gespraechsdauer / beendet)
 * - Ziffernblock fuer Tastentoene, z. B. "1" (Deutsch) oder "2" (Englisch) im easybell-Menue
 * - Lautsprecher an/aus
 * - Auflegen
 * - Bildschirm aus, solange das Handy am Ohr ist (Naeherungssensor), damit die Wange
 *   keine Tasten drueckt
 */
class InCallActivity : KioskBaseActivity() {

    companion object {
        const val EXTRA_NUMBER = "number"

        // Taucht nach dem Waehlen so lange kein Anruf auf, war etwas schief (z. B. App nicht
        // Standard-Telefon-App) - dann zurueck zum Ziffernblock statt ewig zu warten.
        private const val NO_CALL_TIMEOUT_MS = 15_000L
        private const val DTMF_TONE_MS = 200L
        private const val CLOSE_AFTER_END_MS = 1_500L
    }

    private lateinit var binding: ActivityInCallBinding
    private val handler = Handler(Looper.getMainLooper())

    private var call: Call? = null
    private var callSeen = false
    private var ended = false
    private var connectedAtMillis = 0L
    private var speakerOn = false
    private var proximityLock: PowerManager.WakeLock? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handler.post { render() }
        }
    }

    private val callObserver: () -> Unit = {
        handler.post { attachCall(CallManager.call) }
    }

    private val ticker = object : Runnable {
        override fun run() {
            render()
            if (!ended) handler.postDelayed(this, 1_000L)
        }
    }

    private val noCallTimeout = Runnable {
        if (!callSeen) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.numberDisplay.text = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
        binding.dtmfDisplay.text = ""

        DialPad.build(this, binding.dialPad) { digit -> sendDtmf(digit) }

        binding.btnHangUp.setOnClickListener { hangUp() }
        binding.btnSpeaker.setOnClickListener { toggleSpeaker() }

        CallManager.addObserver(callObserver)
        attachCall(CallManager.call)
        if (!callSeen) handler.postDelayed(noCallTimeout, NO_CALL_TIMEOUT_MS)
        handler.post(ticker)
    }

    private fun attachCall(newCall: Call?) {
        if (newCall === call) return
        if (newCall == null) {
            // Anruf wurde vom System entfernt -> als beendet behandeln.
            if (callSeen) onCallEnded()
            return
        }
        call?.unregisterCallback(callCallback)
        call = newCall
        callSeen = true
        handler.removeCallbacks(noCallTimeout)
        newCall.registerCallback(callCallback)
        acquireProximityLock()
        render()
    }

    private fun render() {
        if (ended) return
        val c = call
        when (c?.state) {
            Call.STATE_ACTIVE -> {
                if (connectedAtMillis == 0L) {
                    val connectTime = c.details.connectTimeMillis
                    connectedAtMillis = if (connectTime > 0) connectTime else System.currentTimeMillis()
                }
                val seconds = ((System.currentTimeMillis() - connectedAtMillis) / 1000).coerceAtLeast(0)
                binding.statusDisplay.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }
            Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> onCallEnded()
            else -> binding.statusDisplay.text = getString(R.string.incall_calling)
        }
    }

    private fun onCallEnded() {
        if (ended) return
        ended = true
        binding.statusDisplay.text = getString(R.string.incall_ended)
        releaseProximityLock()
        handler.postDelayed({ finish() }, CLOSE_AFTER_END_MS)
    }

    private fun sendDtmf(digit: String) {
        val c = call ?: return
        if (c.state != Call.STATE_ACTIVE) return
        binding.dtmfDisplay.append(digit)
        c.playDtmfTone(digit[0])
        handler.postDelayed({ c.stopDtmfTone() }, DTMF_TONE_MS)
    }

    private fun hangUp() {
        val c = call
        if (c == null) {
            finish()
        } else {
            c.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun toggleSpeaker() {
        val service = CallManager.service ?: return
        speakerOn = !speakerOn
        service.setAudioRoute(
            if (speakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
        )
        binding.btnSpeaker.isActivated = speakerOn
        // Mit Lautsprecher haelt niemand das Handy ans Ohr -> Bildschirm soll anbleiben.
        if (speakerOn) releaseProximityLock() else acquireProximityLock()
    }

    private fun acquireProximityLock() {
        if (speakerOn || ended) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
        val lock = proximityLock ?: powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "blutigeseiten:incall"
        ).also { proximityLock = it }
        if (!lock.isHeld) lock.acquire(60 * 60 * 1000L)
    }

    private fun releaseProximityLock() {
        proximityLock?.let { if (it.isHeld) it.release() }
    }

    // Zurueck-Taste tut waehrend des Anrufs nichts - beenden nur ueber den roten Knopf.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // bewusst leer
    }

    override fun onDestroy() {
        CallManager.removeObserver(callObserver)
        call?.unregisterCallback(callCallback)
        handler.removeCallbacksAndMessages(null)
        releaseProximityLock()
        super.onDestroy()
    }
}
