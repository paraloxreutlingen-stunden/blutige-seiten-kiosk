package de.paralox.kiosk.blutigeseiten

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * Bildansicht mit den ueblichen Gesten:
 * - Zwei Finger auseinander/zusammen: hinein-/herauszoomen (1x bis 6x)
 * - Ein Finger: verschieben, wenn hineingezoomt
 * - Doppeltippen: 2.5x an der Stelle hineinzoomen bzw. zurueck auf ganze Ansicht
 * Beim Drehen des Handys (neue Groesse) wird das Bild wieder vollstaendig eingepasst.
 */
class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private companion object {
        const val MAX_ZOOM = 6f
        const val DOUBLE_TAP_ZOOM = 2.5f
    }

    private val imageMatrixInternal = Matrix()
    private var baseScale = 1f      // Skalierung, bei der das ganze Bild sichtbar ist
    private var currentZoom = 1f    // zusaetzlicher Zoom relativ dazu

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (currentZoom > 1f) {
                    imageMatrixInternal.postTranslate(-dx, -dy)
                    applyMatrix()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (currentZoom > 1.05f) {
                    fitToView()
                } else {
                    zoomBy(DOUBLE_TAP_ZOOM / currentZoom, e.x, e.y)
                }
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        fitToView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitToView()
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun fitToView() {
        val d = drawable ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (vw <= 0 || vh <= 0 || dw <= 0 || dh <= 0) return

        baseScale = minOf(vw / dw, vh / dh)
        currentZoom = 1f
        imageMatrixInternal.reset()
        imageMatrixInternal.postScale(baseScale, baseScale)
        imageMatrixInternal.postTranslate((vw - dw * baseScale) / 2f, (vh - dh * baseScale) / 2f)
        imageMatrix = imageMatrixInternal
    }

    private fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
        val newZoom = (currentZoom * factor).coerceIn(1f, MAX_ZOOM)
        val actual = newZoom / currentZoom
        if (actual == 1f) return
        currentZoom = newZoom
        imageMatrixInternal.postScale(actual, actual, focusX, focusY)
        applyMatrix()
    }

    /** Haelt das Bild im sichtbaren Bereich (zentriert, wenn kleiner als die Ansicht). */
    private fun applyMatrix() {
        val d = drawable ?: return
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        imageMatrixInternal.mapRect(rect)
        val vw = width.toFloat()
        val vh = height.toFloat()

        val dx = when {
            rect.width() <= vw -> (vw - rect.width()) / 2f - rect.left
            rect.left > 0 -> -rect.left
            rect.right < vw -> vw - rect.right
            else -> 0f
        }
        val dy = when {
            rect.height() <= vh -> (vh - rect.height()) / 2f - rect.top
            rect.top > 0 -> -rect.top
            rect.bottom < vh -> vh - rect.bottom
            else -> 0f
        }
        imageMatrixInternal.postTranslate(dx, dy)
        imageMatrix = imageMatrixInternal
    }
}
