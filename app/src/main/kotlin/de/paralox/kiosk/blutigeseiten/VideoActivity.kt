package de.paralox.kiosk.blutigeseiten

import android.net.Uri
import android.os.Bundle
import android.view.View
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
}
