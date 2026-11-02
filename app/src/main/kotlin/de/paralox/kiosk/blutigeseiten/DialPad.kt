package de.paralox.kiosk.blutigeseiten

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Baut den 3x4-Ziffernblock (Ziffer + Buchstaben wie auf einem echten Telefon).
 * Wird sowohl beim Waehlen (CallActivity) als auch waehrend des Gespraechs
 * (InCallActivity, fuer Tastentoene wie "1" oder "2") verwendet.
 *
 * Bewusst LinearLayout-Zeilen mit je 3 gleich breiten Zellen statt GridLayout mit
 * Gewichtung - siehe Kommentar in activity_call.xml (GridLayout wirkte nach links verschoben).
 */
object DialPad {

    // Ziffer -> Buchstaben, wie auf einem echten Telefon-Dialpad (Reihenfolge = Anzeige).
    private val letterMap = linkedMapOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

    fun build(context: Context, container: LinearLayout, onDigit: (String) -> Unit) {
        container.removeAllViews()
        letterMap.entries.chunked(3).forEach { rowEntries ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for ((digit, letters) in rowEntries) {
                val cell = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    val outValue = TypedValue()
                    context.theme.resolveAttribute(
                        android.R.attr.selectableItemBackgroundBorderless, outValue, true
                    )
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

                    setOnClickListener { onDigit(digit) }
                }
                row.addView(cell)
            }
            container.addView(row)
        }
    }
}
