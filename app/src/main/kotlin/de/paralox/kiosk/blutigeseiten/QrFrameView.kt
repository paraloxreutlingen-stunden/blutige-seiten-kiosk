package de.paralox.kiosk.blutigeseiten

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Sucher-Rahmen fuer den QR-Scanner - ganz ohne Text, damit die App
 * unabhaengig von der Geraetesprache funktioniert:
 * quadratischer Rahmen mit weissen Eckwinkeln, duenner heller Umrandung und einer
 * roten Scan-Linie, die langsam auf und ab laeuft.
 *
 * flashError() laesst den Rahmen kurz rot aufleuchten (z. B. wenn die Kamera nicht startet).
 */
class QrFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private val cornerColor = Color.WHITE
    private val errorColor = Color.parseColor("#E53935")

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#80FFFFFF")
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * density
        // Jede Ecke ist EIN zusammenhaengender Pfad mit spitzer Verbindung (Miter) und
        // geraden Enden - so entsteht an der Ecke keine Stufe wie bei zwei Einzellinien.
        strokeJoin = Paint.Join.MITER
        strokeCap = Paint.Cap.BUTT
        color = cornerColor
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.parseColor("#E53935")
    }

    private val frame = RectF()
    private val cornerPath = Path()
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

        // Eckwinkel: Innenkante liegt genau auf der duennen Umrandung
        val len = 22f * density
        val o = cornerPaint.strokeWidth / 2f
        val l = frame.left - o
        val t = frame.top - o
        val r = frame.right + o
        val b = frame.bottom + o
        cornerPath.reset()
        // oben links
        cornerPath.moveTo(l, t + len); cornerPath.lineTo(l, t); cornerPath.lineTo(l + len, t)
        // oben rechts
        cornerPath.moveTo(r - len, t); cornerPath.lineTo(r, t); cornerPath.lineTo(r, t + len)
        // unten rechts
        cornerPath.moveTo(r, b - len); cornerPath.lineTo(r, b); cornerPath.lineTo(r - len, b)
        // unten links
        cornerPath.moveTo(l + len, b); cornerPath.lineTo(l, b); cornerPath.lineTo(l, b - len)
        canvas.drawPath(cornerPath, cornerPaint)
    }
}
