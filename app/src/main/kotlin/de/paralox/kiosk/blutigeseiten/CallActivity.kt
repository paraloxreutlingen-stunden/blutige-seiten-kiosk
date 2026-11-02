package de.paralox.kiosk.blutigeseiten

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import de.paralox.kiosk.blutigeseiten.databinding.ActivityCallBinding

/**
 * Eigener, einfacher Ziffernblock statt der System-Telefon-App.
 *
 * Bewusst so gebaut: kein Zugriff auf Kontakte, keine Anrufliste, keine Vorschlaege.
 * Die Rufnummern muessen die Spieler*innen im Spiel selbst herausfinden und eintippen -
 * nichts ist hier gespeichert oder vorausgefuellt. Nach jedem Oeffnen ist das Feld leer.
 *
 * Zusaetzlich: Es koennen NUR die drei im Spiel "Blutige Seiten" vorgesehenen Nummern
 * tatsaechlich angerufen werden. Jede andere Nummer wird abgelehnt. Die Nummern stehen
 * dafuer unten als reiner Vergleichswert im Code - sie werden nirgends angezeigt,
 * vorausgefuellt oder vorgeschlagen, sie dienen ausschliesslich der Pruefung.
 */
class CallActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityCallBinding
    private val typedNumber = StringBuilder()

    // Nur diese drei Nummern duerfen tatsaechlich angerufen werden (Spiel "Blutige Seiten").
    // Bei einem neuen Spiel hier anpassen.
    private val allowedNumbers = setOf(
        "+49 7121 1594185",
        "+49 7121 1594318",
        "+49 7121 2634003"
    ).map { normalize(it) }.toSet()

    /**
     * Normalisiert eine Rufnummer auf eine vergleichbare Form, unabhaengig davon, ob
     * sie mit +49, 0049 oder 0 eingegeben wurde, und egal ob mit Leerzeichen/Bindestrichen.
     */
    private fun normalize(number: String): String {
        var digits = number.filter { it.isDigit() }
        digits = when {
            number.trim().startsWith("+49") -> digits.removePrefix("49")
            digits.startsWith("0049") -> digits.removePrefix("0049")
            digits.startsWith("49") && digits.length > 10 -> digits.removePrefix("49")
            digits.startsWith("0") -> digits.removePrefix("0")
            else -> digits
        }
        return digits
    }

    private fun isAllowedNumber(input: String): Boolean {
        return allowedNumbers.contains(normalize(input))
    }

    private val callPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            placeCall()
        } else {
            Feedback.error(binding.numberDisplay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DialPad.build(this, binding.dialPad) { digit ->
            typedNumber.append(digit)
            updateDisplay()
        }

        binding.btnClear.setOnClickListener {
            if (typedNumber.isNotEmpty()) {
                typedNumber.deleteCharAt(typedNumber.length - 1)
                updateDisplay()
            }
        }

        binding.btnDial.setOnClickListener {
            if (typedNumber.isEmpty()) return@setOnClickListener

            if (!isAllowedNumber(typedNumber.toString())) {
                // Ohne Text: Nummer kurz rot, schuetteln, vibrieren
                showNumberRejected()
                return@setOnClickListener
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                placeCall()
            } else {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    // Aktiviert automatisch "Jungfraeulich": Zahlenfeld wird nie ueber Activity-Grenzen
    // hinweg gespeichert. Sobald der Bildschirm neu aufgebaut wird, ist er leer.
    override fun onStart() {
        super.onStart()
        typedNumber.clear()
        updateDisplay()
    }

    private fun showNumberRejected() {
        val display = binding.numberDisplay
        display.setTextColor(0xFFE53935.toInt())
        Feedback.error(display)
        display.postDelayed({ display.setTextColor(0xFFFFFFFF.toInt()) }, 900L)
    }

    private fun updateDisplay() {
        binding.numberDisplay.text = typedNumber.toString()
    }

    /**
     * Waehlt ueber TelecomManager statt per ACTION_CALL-Intent: Damit wird keine fremde
     * Activity gestartet (das blockiert die App-Fixierung). Ist die App Standard-Telefon-App,
     * landet der Anruf in KioskInCallService und wird im eigenen Anrufbildschirm angezeigt.
     */
    @SuppressLint("MissingPermission")
    private fun placeCall() {
        val number = typedNumber.toString()
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        try {
            telecomManager.placeCall(Uri.fromParts("tel", number, null), Bundle())
        } catch (_: SecurityException) {
            Feedback.error(binding.numberDisplay)
            return
        }
        startActivity(
            Intent(this, InCallActivity::class.java)
                .putExtra(InCallActivity.EXTRA_NUMBER, number)
        )
    }
}
