@file:Suppress("DEPRECATION")

package com.arkhe.sunmiv1s

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkhe.sunmiv1s.presentation.MainScreen
import com.arkhe.sunmiv1s.presentation.SunmiAppTheme
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class MainActivity : ComponentActivity() {
    private lateinit var printerManager: SunmiPrinterManager
    private var isServiceConnected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize printer manager
        printerManager = SunmiPrinterManager(this)
        printerManager.setConnectionListener { connected ->
            isServiceConnected = connected
        }

        // Connect to printer service
        printerManager.connectService()

        setContent {
            SunmiAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceConnected = isServiceConnected,
                        onPrintReceipt = { printReceipt() },
                        onScanQR = { scanQRCode() }
                    )
                }
            }
        }
    }

    private fun printReceipt() {
        if (!isServiceConnected) {
            Toast.makeText(this, "Printer Service Not Found", Toast.LENGTH_SHORT).show()
            return
        }

        val success = printerManager.printSampleReceipt()
        if (success) {
            Toast.makeText(this, "Print Struck Failed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Print Struck Success", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun scanQRCode() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR Code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents == null) {
            Toast.makeText(this, "Scan Cancellation", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Scan Result: ${result.contents}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        printerManager.disconnectService()
    }
}