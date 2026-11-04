package de.paralox.kiosk.blutigeseiten

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * 3x3-Muster-Eingabe wie auf dem Android-Sperrbildschirm.
 *
 * Punkte sind 1..9 nummeriert (oben links = 1, unten rechts = 9). Wie bei Android wird ein
 * uebersprungener Punkt in der Mitte (z. B. von 1 nach 3 ueber die 2) automatisch mitgenommen.
 * Nach dem Loslassen meldet [onPatternEntered] die Ziffernfolge, z. B. "124758963".
 */
class PatternView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onPatternEntered: ((String) -> Unit)? = null

    private val dp = resources.displayMetrics.density
    private val normalColor = Color.WHITE
    private val errorColor = Color.parseColor("#E53935")

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = normalColor
        setShadowLayer(3f * dp, 0f, 1f * dp, 0x66000000)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x40FFFFFF
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xCCFFFFFF.toInt()
    }

    private val centersX = FloatArray(9)
    private val centersY = FloatArray(9)
    private var cell = 0f

    private val path = mutableListOf<Int>()   // Indizes 0..8
    private var fingerX = 0f
    private var fingerY = 0f
    private var drawing = false
    private var error = false
    private val linePath = Path()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // fuer den Schatten unter den Punkten
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cell = minOf(w, h) / 3f
        val left = (w - cell * 3) / 2f
        val top = (h - cell * 3) / 2f
        for (i in 0 until 9) {
            centersX[i] = left + cell * (i % 3) + cell / 2f
            centersY[i] = top + cell * (i / 3) + cell / 2f
        }
    }

    /** Falsches Muster: kurz rot anzeigen, dann zuruecksetzen. */
    fun showError() {
        error = true
        invalidate()
        postDelayed({
            error = false
            path.clear()
            invalidate()
        }, 1_000L)
    }

    fun clear() {
        path.clear()
        invalidate()
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (error) return true
        fingerX = event.x
        fingerY = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path.clear()
                drawing = true
                hitTest(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                // Auch Zwischenpositionen pruefen, damit schnelle Wischbewegungen keine Punkte verlieren
                for (h in 0 until event.historySize) hitTest(event.getHistoricalX(h), event.getHistoricalY(h))
                hitTest(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawing = false
                if (path.isNotEmpty() && event.actionMasked == MotionEvent.ACTION_UP) {
                    onPatternEntered?.invoke(path.joinToString("") { (it + 1).toString() })
                } else {
                    path.clear()
                }
            }
        }
        invalidate()
        return true
    }

    private fun hitTest(x: Float, y: Float) {
        val radius = cell * 0.32f
        for (i in 0 until 9) {
            if (i in path) continue
            if (hypot(x - centersX[i], y - centersY[i]) <= radius) {
                // Uebersprungenen Mittelpunkt mitnehmen (wie bei Android)
                path.lastOrNull()?.let { last ->
                    val r1 = last / 3; val c1 = last % 3
                    val r2 = i / 3; val c2 = i % 3
                    if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
                        val mid = ((r1 + r2) / 2) * 3 + (c1 + c2) / 2
                        if (mid != last && mid != i && mid !in path) path.add(mid)
                    }
                }
                path.add(i)
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                return
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val color = if (error) errorColor else normalColor
        linePaint.color = if (error) errorColor else 0xCCFFFFFF.toInt()
        ringPaint.color = if (error) 0x55E53935 else 0x40FFFFFF

        // Linien
        if (path.isNotEmpty()) {
            linePath.reset()
            linePath.moveTo(centersX[path[0]], centersY[path[0]])
            for (k in 1 until path.size) linePath.lineTo(centersX[path[k]], centersY[path[k]])
            if (drawing) linePath.lineTo(fingerX, fingerY)
            canvas.drawPath(linePath, linePaint)
        }

        // Punkte
        for (i in 0 until 9) {
            val selected = i in path
            if (selected) canvas.drawCircle(centersX[i], centersY[i], 22f * dp, ringPaint)
            dotPaint.color = if (selected) color else normalColor
            canvas.drawCircle(centersX[i], centersY[i], (if (selected) 7f else 5f) * dp, dotPaint)
        }
    }
}
