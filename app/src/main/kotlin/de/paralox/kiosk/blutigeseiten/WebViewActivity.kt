package de.paralox.kiosk.blutigeseiten

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import de.paralox.kiosk.blutigeseiten.databinding.ActivityWebviewBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Zeigt das Ziel eines gescannten QR-Codes.
 *
 * - Ist das Ziel ein Bild (so bei den "blutige-seiten-qr-code-N"-Adressen), wird es direkt in
 *   der App angezeigt: Zoomen mit zwei Fingern, Verschieben, Doppeltippen, und die Ansicht
 *   dreht sich mit dem Handy. Das Bild wird nur im Arbeitsspeicher gehalten, nichts wird
 *   gespeichert. Grosse Bilder werden passend verkleinert geladen - eine Webansicht kann
 *   bei sehr grossen Bildern auf einfachen Handys abstuerzen.
 * - Ist es eine Webseite, wird sie in einer eingeschraenkten Webansicht gezeigt
 *   (keine Adressleiste, kein Verlauf, kein Wechsel auf eine andere Domain).
 */
class WebViewActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityWebviewBinding

    @Volatile
    private var destroyed = false

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val MAX_IMAGE_BYTES = 40 * 1024 * 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        loadTarget(url)
    }

    private fun loadTarget(url: String) {
        binding.progress.visibility = View.VISIBLE
        val maxSide = maxOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels) * 2

        thread {
            var bitmap: Bitmap? = null
            var isImage = false
            var failed = false
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    useCaches = false
                    instanceFollowRedirects = true
                }
                try {
                    isImage = connection.contentType?.startsWith("image/") == true
                    if (isImage) {
                        val bytes = connection.inputStream.use { it.readBytes() }
                        if (bytes.size <= MAX_IMAGE_BYTES) bitmap = decodeScaled(bytes, maxSide)
                        if (bitmap == null) failed = true
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                failed = true
            }

            runOnUiThread {
                if (destroyed) return@runOnUiThread
                binding.progress.visibility = View.GONE
                when {
                    bitmap != null -> showImage(bitmap!!)
                    failed && isImage -> {
                        Toast.makeText(this, getString(R.string.qr_load_failed), Toast.LENGTH_LONG).show()
                    }
                    else -> showWebPage(url)
                }
            }
        }
    }

    /** Laedt das Bild hoechstens so gross wie noetig (spart Speicher auf einfachen Handys). */
    private fun decodeScaled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun showImage(bitmap: Bitmap) {
        binding.webView.visibility = View.GONE
        binding.zoomImage.visibility = View.VISIBLE
        binding.zoomImage.setImageBitmap(bitmap)
    }

    private fun showWebPage(url: String) {
        val allowedHost = Uri.parse(url).host
        binding.zoomImage.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            clearCache(true)
            clearHistory()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                    // Nur Links auf derselben Domain erlauben - alles andere blockieren.
                    return request.url.host != allowedHost
                }

                // Ohne diese Methode beendet Android die ganze App, wenn die Webansicht
                // abstuerzt (z. B. Speicher voll). So geht es nur zurueck zum Menue.
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    finish()
                    return true
                }
            }
            loadUrl(url)
        }
    }

    override fun onDestroy() {
        destroyed = true
        binding.webView.apply {
            clearCache(true)
            clearHistory()
            loadUrl("about:blank")
        }
        super.onDestroy()
    }
}
