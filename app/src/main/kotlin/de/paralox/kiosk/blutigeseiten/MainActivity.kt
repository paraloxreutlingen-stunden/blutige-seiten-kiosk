package de.paralox.kiosk.blutigeseiten

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import de.paralox.kiosk.blutigeseiten.databinding.ActivityMainBinding

/**
 * Hauptmenue. Das ist der einzige Bildschirm, den Spieler*innen nach dem Entsperren
 * des Geraets (Android-Sperrmuster) zu Gesicht bekommen. Von hier aus geht es nur
 * in die vier erlaubten Funktionen - sonst nirgendwo hin.
 *
 * Beim allerersten Start fuehrt die App durch die Einrichtung (siehe KioskSetup):
 * alle Berechtigungen auf einmal, danach "Als Standard-Telefon-App festlegen".
 * Erst wenn beides erledigt ist, fixiert sich die App.
 */
class MainActivity : KioskBaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private var permissionsAsked = false
    private var dialerAsked = false
    private var setupRequestRunning = false
    private var setupDialog: AlertDialog? = null

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        setupRequestRunning = false
        continueSetup()
    }

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        setupRequestRunning = false
        continueSetup()
    }

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
        // SMS voruebergehend abgeschaltet (siehe KioskSetup.SMS_ENABLED). Das Dock ist
        // zentriert und passt sich an - die drei uebrigen Icons verteilen sich automatisch.
        binding.btnSms.visibility = if (KioskSetup.SMS_ENABLED) View.VISIBLE else View.GONE
        binding.btnSms.setOnClickListener {
            if (KioskSetup.SMS_ENABLED) startActivity(Intent(this, SmsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        continueSetup()
    }

    private fun continueSetup() {
        if (setupRequestRunning) return
        setupDialog?.dismiss()
        setupDialog = null

        val missing = KioskSetup.missingPermissions(this)
        if (missing.isNotEmpty()) {
            if (!permissionsAsked) {
                permissionsAsked = true
                setupRequestRunning = true
                permissionsLauncher.launch(missing.toTypedArray())
            } else {
                // Abgelehnt (evtl. dauerhaft) -> Android fragt nicht mehr, also App-Einstellungen.
                showSetupDialog(R.string.setup_permissions_message) {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                }
            }
            return
        }

        if (!KioskSetup.isDefaultDialer(this)) {
            if (!dialerAsked) {
                dialerAsked = true
                setupRequestRunning = true
                try {
                    defaultDialerLauncher.launch(KioskSetup.defaultDialerRequestIntent(this))
                } catch (_: Exception) {
                    setupRequestRunning = false
                    continueSetup()
                }
            } else {
                showSetupDialog(R.string.setup_dialer_message) {
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    } catch (_: Exception) {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
            return
        }

        // Alles eingerichtet -> jetzt fixieren.
        startPinningIfNeeded()
        // ... und einmalig das Entsperrmuster abfragen (siehe AppLock).
        showPatternLockIfNeeded()
    }

    private fun showSetupDialog(messageRes: Int, openSettings: () -> Unit) {
        setupDialog = AlertDialog.Builder(this)
            .setTitle(R.string.setup_title)
            .setMessage(messageRes)
            .setCancelable(false)
            .setPositiveButton(R.string.setup_open_settings) { _, _ -> openSettings() }
            .show()
    }

    // Zurueck-Taste auf dem Hauptmenue tut nichts - es gibt nichts "davor".
    // Verhindert, dass Spieler*innen aus Versehen die App verlassen.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // bewusst leer
    }
}
