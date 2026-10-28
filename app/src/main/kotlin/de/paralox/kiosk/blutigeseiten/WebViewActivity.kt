package de.paralox.kiosk.blutigeseiten

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import de.paralox.kiosk.blutigeseiten.databinding.ActivityWebviewBinding

/**
 * Zeigt ausschliesslich die eine erlaubte Zielseite - keine Adressleiste, kein Verlauf,
 * kein Wechsel auf eine andere Domain. Cache und Verlauf werden bei jedem Aufruf geleert,
 * damit auch hier nichts von Spiel zu Spiel haengen bleibt.
 */
class WebViewActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityWebviewBinding

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL) ?: return
        val allowedHost = Uri.parse(url).host

        binding.webView.apply {
            settings.javaScriptEnabled = true
            clearCache(true)
            clearHistory()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                    // Nur Links auf derselben Domain erlauben - alles andere blockieren.
                    return request.url.host != allowedHost
                }
            }
            loadUrl(url)
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        binding.webView.apply {
            clearCache(true)
            clearHistory()
            loadUrl("about:blank")
        }
        super.onDestroy()
    }
}
