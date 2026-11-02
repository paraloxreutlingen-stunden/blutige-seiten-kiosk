package de.paralox.kiosk.blutigeseiten

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Sucher-Rahmen fuer den QR-Scanner - ganz ohne Text, damit die App
 * unabhaengig von der Geraetesprache funktioniert:
 * quadratischer Rahmen mit blauen Eckwinkeln, duenner heller Umrandung und einer
 * roten Scan-Linie, die langsam auf und ab laeuft.
 *
 * flashError() laesst den Rahmen kurz rot aufleuchten (z. B. wenn die Kamera nicht startet).
 */
class QrFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private val cornerColor = Color.parseColor("#4285F4")
    private val errorColor = Color.parseColor("#E53935")

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#B3FFFFFF")
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * density
        strokeCap = Paint.Cap.SQUARE
        color = cornerColor
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.parseColor("#E53935")
    }

    private val frame = RectF()
    private var lineProgress = 0.5f

    private val lineAnimator = ValueAnimator.ofFloat(0.08f, 0.92f).apply {
        duration = 2_200L
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            lineProgress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lineAnimator.start()
    }

    override fun onDetachedFromWindow() {
        lineAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Quadrat: 75 % der kuerzeren Seite, mittig
        val side = minOf(w, h) * 0.75f
        val left = (w - side) / 2f
        val top = (h - side) / 2f
        frame.set(left, top, left + side, top + side)
    }

    fun flashError() {
        cornerPaint.color = errorColor
        invalidate()
        postDelayed({
            cornerPaint.color = cornerColor
            invalidate()
        }, 1_200L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (frame.isEmpty) return

        canvas.drawRect(frame, borderPaint)

        // Scan-Linie
        val y = frame.top + frame.height() * lineProgress
        canvas.drawLine(frame.left, y, frame.right, y, linePaint)

        // Eckwinkel, leicht ausserhalb der Umrandung wie im Vorbild
        val len = 22f * density
        val o = cornerPaint.strokeWidth / 2f
        val l = frame.left - o
        val t = frame.top - o
        val r = frame.right + o
        val b = frame.bottom + o
        // oben links
        canvas.drawLine(l, t, l + len, t, cornerPaint)
        canvas.drawLine(l, t, l, t + len, cornerPaint)
        // oben rechts
        canvas.drawLine(r, t, r - len, t, cornerPaint)
        canvas.drawLine(r, t, r, t + len, cornerPaint)
        // unten links
        canvas.drawLine(l, b, l + len, b, cornerPaint)
        canvas.drawLine(l, b, l, b - len, cornerPaint)
        // unten rechts
        canvas.drawLine(r, b, r - len, b, cornerPaint)
        canvas.drawLine(r, b, r, b - len, cornerPaint)
    }
}
