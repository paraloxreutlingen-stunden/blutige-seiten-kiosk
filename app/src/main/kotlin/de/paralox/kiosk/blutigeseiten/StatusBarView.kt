package de.paralox.kiosk.blutigeseiten

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.Date

/**
 * Nachgebaute Statusleiste wie bei einem normalen Handy: links die Uhrzeit, rechts
 * Mobilfunkempfang, WLAN, Akku-Symbol und Akkustand in Prozent.
 *
 * Auf dem Startbildschirm ist die Uhrzeit ausgeblendet (app:showTime="false"), weil sie dort
 * schon gross in der Mitte steht.
 *
 * Eigener Nachbau statt der echten Android-Statusleiste, weil diese auf den Spiel-Handys
 * nicht erscheint (und sich ohnehin nicht herunterziehen lassen soll). Die echte Leiste
 * blendet KioskBaseActivity aus, damit nie zwei Leisten uebereinander liegen.
 *
 * Alle Symbole werden gezeichnet - kein Text ausser Uhrzeit und Prozentzahl.
 */
class StatusBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dp = resources.displayMetrics.density
    private val uiHandler = Handler(Looper.getMainLooper())

    private var timeText = ""
    private val showTime: Boolean
    private var batteryPercent = -1
    private var charging = false
    private var wifiLevel = -1   // -1 = kein WLAN, sonst 0..3
    private var cellLevel = -1   // -1 = kein Netz/keine SIM, sonst 0..4

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * resources.displayMetrics.scaledDensity
        setShadowLayer(3f * dp, 0f, 1f * dp, 0x99000000.toInt())
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(3f * dp, 0f, 1f * dp, 0x66000000)
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * dp
        setShadowLayer(3f * dp, 0f, 1f * dp, 0x66000000)
    }
    private val chargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }
    private val rect = RectF()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = updateBattery(intent)
    }

    private val updater = object : Runnable {
        override fun run() {
            updateTime()
            updateConnections()
            invalidate()
            uiHandler.postDelayed(this, 5_000L)
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.StatusBarView)
        showTime = a.getBoolean(R.styleable.StatusBarView_showTime, true)
        a.recycle()
        setLayerType(LAYER_TYPE_SOFTWARE, null) // fuer die Schatten unter den Symbolen
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ContextCompat.registerReceiver(
            context, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )?.let { updateBattery(it) }
        uiHandler.post(updater)
    }

    override fun onDetachedFromWindow() {
        uiHandler.removeCallbacks(updater)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        super.onDetachedFromWindow()
    }

    private fun updateTime() {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        timeText = DateFormat.format(pattern, Date()).toString()
    }

    private fun updateBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        invalidate()
    }

    @Suppress("DEPRECATION")
    private fun updateConnections() {
        // WLAN
        wifiLevel = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    caps.signalStrength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED
                ) {
                    WifiManager.calculateSignalLevel(caps.signalStrength, 4)
                } else 3
            } else -1
        } catch (_: Exception) {
            -1
        }

        // Mobilfunk
        cellLevel = try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            when {
                tm.simState != TelephonyManager.SIM_STATE_READY -> -1
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> tm.signalStrength?.level ?: 0
                else -> 3
            }
        } catch (_: Exception) {
            -1
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val padding = 16f * dp
        val iconH = 12f * dp
        val cy = h / 2f
        val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f

        // Uhrzeit links
        if (showTime) canvas.drawText(timeText, padding, baseline, textPaint)

        // Rechts, von aussen nach innen: Prozent, Akku, WLAN, Mobilfunk
        var x = width - padding
        if (batteryPercent >= 0) {
            val pct = "$batteryPercent %"
            x -= textPaint.measureText(pct)
            canvas.drawText(pct, x, baseline, textPaint)
            x -= 5f * dp
        }
        x = drawBattery(canvas, x, cy, iconH) - 7f * dp
        if (wifiLevel >= 0) x = drawWifi(canvas, x, cy, iconH) - 7f * dp
        drawSignal(canvas, x, cy, iconH)
    }

    /** Akku liegend, rechts mit kleinem Pol. Gibt die linke Kante zurueck. */
    private fun drawBattery(canvas: Canvas, right: Float, cy: Float, iconH: Float): Float {
        val bodyW = 20f * dp
        val bodyH = iconH * 0.85f
        val nubW = 2f * dp
        val left = right - nubW - bodyW
        rect.set(left, cy - bodyH / 2f, left + bodyW, cy + bodyH / 2f)
        canvas.drawRoundRect(rect, 2.5f * dp, 2.5f * dp, strokePaint)
        // Pol
        rect.set(left + bodyW + 0.5f * dp, cy - bodyH / 5f, right, cy + bodyH / 5f)
        canvas.drawRoundRect(rect, 1f * dp, 1f * dp, fillPaint)
        // Fuellstand
        if (batteryPercent >= 0) {
            val inset = 2.2f * dp
            val innerW = (bodyW - 2 * inset) * batteryPercent / 100f
            rect.set(left + inset, cy - bodyH / 2f + inset, left + inset + innerW, cy + bodyH / 2f - inset)
            canvas.drawRoundRect(rect, 1f * dp, 1f * dp, if (charging) chargePaint else fillPaint)
        }
        return left
    }

    /** WLAN-Faecher (Viertelkreis nach oben): blass = Maximum, weiss = aktuelle Staerke. */
    private fun drawWifi(canvas: Canvas, right: Float, cy: Float, iconH: Float): Float {
        val r = iconH * 1.15f
        val w = r * 2f * 0.72f
        val cx = right - w / 2f
        val bottom = cy + iconH / 2f + 1f * dp
        rect.set(cx - r, bottom - r, cx + r, bottom + r)
        canvas.drawArc(rect, 225f, 90f, true, dimPaint)
        val lr = r * (wifiLevel + 1) / 4f
        rect.set(cx - lr, bottom - lr, cx + lr, bottom + lr)
        canvas.drawArc(rect, 225f, 90f, true, fillPaint)
        return right - w
    }

    /** Vier Empfangsbalken, steigend. Gibt die linke Kante zurueck. */
    private fun drawSignal(canvas: Canvas, right: Float, cy: Float, iconH: Float): Float {
        val barW = 3f * dp
        val gap = 1.5f * dp
        val bottom = cy + iconH / 2f
        val left = right - 4 * barW - 3 * gap
        for (i in 0 until 4) {
            val barH = iconH * (i + 1) / 4f
            val bx = left + i * (barW + gap)
            rect.set(bx, bottom - barH, bx + barW, bottom)
            canvas.drawRoundRect(rect, 0.8f * dp, 0.8f * dp, if (i < cellLevel) fillPaint else dimPaint)
        }
        return left
    }
}
