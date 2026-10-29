package de.paralox.kiosk.blutigeseiten

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
            Toast.makeText(this, getString(R.string.call_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildDialPad()

        binding.btnClear.setOnClickListener {
            if (typedNumber.isNotEmpty()) {
                typedNumber.deleteCharAt(typedNumber.length - 1)
                updateDisplay()
            }
        }

        binding.btnDial.setOnClickListener {
            if (typedNumber.isEmpty()) return@setOnClickListener

            if (!isAllowedNumber(typedNumber.toString())) {
                Toast.makeText(this, getString(R.string.call_number_not_allowed), Toast.LENGTH_LONG).show()
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

    // Ziffer -> Buchstaben, wie auf einem echten Telefon-Dialpad.
    private val letterMap = mapOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

    // 4 Zeilen mit je 3 gleich breiten Zellen (statt GridLayout mit Gewichtung - siehe
    // Kommentar in activity_call.xml, warum das zu einem nach links verschobenen Feld fuehrte).
    private fun buildDialPad() {
        val container: LinearLayout = binding.dialPad
        letterMap.entries.chunked(3).forEach { rowEntries ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for ((digit, letters) in rowEntries) {
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    val outValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                    setBackgroundResource(outValue.resourceId)

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { setMargins(4, 14, 4, 14) }

                    addView(TextView(context).apply {
                        text = digit
                        setTextAppearance(R.style.DialPadDigit)
                        gravity = Gravity.CENTER
                    })
                    addView(TextView(context).apply {
                        text = letters
                        setTextAppearance(R.style.DialPadLetters)
                        gravity = Gravity.CENTER
                    })

                    setOnClickListener {
                        typedNumber.append(digit)
                        updateDisplay()
                    }
                }
                row.addView(cell)
            }
            container.addView(row)
        }
    }

    private fun updateDisplay() {
        binding.numberDisplay.text = typedNumber.toString()
    }

    private fun placeCall() {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${typedNumber}"))
        startActivity(intent)
    }
}
