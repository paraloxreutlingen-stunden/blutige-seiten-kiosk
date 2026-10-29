package de.paralox.kiosk.blutigeseiten

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import de.paralox.kiosk.blutigeseiten.databinding.ActivityVideoBinding

/**
 * Spielt das im Projekt hinterlegte Video (res/raw/blutige_seiten_loop.mp4) in Endlosschleife ab.
 * Das Video liegt fest in der App - kein Internet, kein externer Speicher noetig,
 * laeuft also auch zuverlaessig ohne WLAN/Mobilfunk.
 */
class VideoActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bildschirm bleibt an, solange das Video laeuft.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        hideSystemBars()

        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.blutige_seiten_loop}")
        binding.videoView.setVideoURI(videoUri)
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            binding.loadingLabel.visibility = View.GONE
            binding.videoView.start()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Status-/Navigationsleiste kommt nach einem Fokuswechsel (z.B. Drehung) manchmal
        // kurz zurueck - hier wieder ausblenden, damit das Video echtes Vollbild bleibt.
        if (hasFocus) hideSystemBars()
    }

    // Echtes Vollbild: Status- und Navigationsleiste ausblenden, per Wischen vom Rand
    // vorübergehend wieder einblendbar (immersive sticky).
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }
}
