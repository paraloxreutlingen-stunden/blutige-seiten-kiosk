package de.paralox.kiosk.blutigeseiten

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import de.paralox.kiosk.blutigeseiten.databinding.ActivitySmsBinding

/**
 * Schickt die Nachricht direkt per SmsManager an die Spielleitung - OHNE die System-SMS-App
 * zu oeffnen. Dadurch gibt es gar keinen sichtbaren Nachrichtenverlauf auf dem Geraet, den man
 * nach dem Spiel loeschen muesste: die App selbst speichert nichts, "jungfraeulich" ist hier
 * also der Normalzustand statt ein Reset-Schritt.
 */
class SmsActivity : KioskBaseActivity() {

    private lateinit var binding: ActivitySmsBinding

    // Rufnummer der Spielleitung (0176 80297001, mit Laendervorwahl)
    private val gameMasterNumber = "+4917680297001"

    private val smsPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendMessage()
        } else {
            Toast.makeText(this, getString(R.string.sms_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSend.setOnClickListener {
            if (binding.messageInput.text.isBlank()) return@setOnClickListener
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                sendMessage()
            } else {
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        binding.messageInput.setText("")
    }

    private fun sendMessage() {
        val text = binding.messageInput.text.toString()
        try {
            // getDefault() statt getSystemService(), damit es auch auf aelteren
            // Android-Versionen (ab minSdk 26) zuverlaessig funktioniert.
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(gameMasterNumber, null, text, null, null)
            Toast.makeText(this, getString(R.string.sms_sent), Toast.LENGTH_LONG).show()
            binding.messageInput.setText("")
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.sms_failed), Toast.LENGTH_LONG).show()
        }
    }
}
