package de.paralox.kiosk.blutigeseiten

import android.content.Intent
import android.os.Bundle
import de.paralox.kiosk.blutigeseiten.databinding.ActivityMainBinding

/**
 * Hauptmenue. Das ist der einzige Bildschirm, den Spieler*innen nach dem Entsperren
 * des Geraets (Android-Sperrmuster) zu Gesicht bekommen. Von hier aus geht es nur
 * in die vier erlaubten Funktionen - sonst nirgendwo hin.
 */
class MainActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCall.setOnClickListener {
            startActivity(Intent(this, CallActivity::class.java))
        }
        binding.btnQr.setOnClickListener {
            startActivity(Intent(this, QrActivity::class.java))
        }
        binding.btnVideo.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java))
        }
        binding.btnSms.setOnClickListener {
            startActivity(Intent(this, SmsActivity::class.java))
        }
    }

    // Zurueck-Taste auf dem Hauptmenue tut nichts - es gibt nichts "davor".
    // Verhindert, dass Spieler*innen aus Versehen die App verlassen.
    override fun onBackPressed() {
        // bewusst leer
    }
}
