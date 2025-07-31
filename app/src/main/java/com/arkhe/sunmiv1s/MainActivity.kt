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
import com.arkhe.sunmiv1s.service.PrintService
import com.arkhe.sunmiv1s.ui.screen.MainScreen
import com.arkhe.sunmiv1s.ui.theme.SunmiV1sTheme
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class MainActivity : ComponentActivity() {
    private lateinit var printService: PrintService
    private var isServiceConnected by mutableStateOf(false)
    private var showQRDialog by mutableStateOf(false)
    private var lastScanResult by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize print service
        printService = PrintService(this)
        printService.initialize { connected ->
            isServiceConnected = connected
        }

        setContent {
            SunmiV1sTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceConnected = isServiceConnected,
                        onPrintReceipt = { printReceipt() },
                        onScanQR = { scanQRCode() },
                        onPrintQR = { showQRDialog = true },
                        onCheckStatus = { checkPrinterStatus() },
                        lastScanResult = lastScanResult,
                        showQRDialog = showQRDialog,
                        onShowQRDialog = { showQRDialog = it },
                        onPrintCustomQR = { qrData -> printCustomQRCode(qrData) }
                    )
                }
            }
        }
    }

    private fun printReceipt() {
        if (!isServiceConnected) {
            showToast("Printer Service Not Available")
            return
        }

        val success = printService.printSampleReceipt()
        if (success) {
            showToast("Print Receipt Success")
        } else {
            showToast("Print Receipt Failed")
        }
    }

    private fun printCustomQRCode(qrData: String) {
        if (!printService.isConnected()) {
            showToast("Printer service Not Available")
            return
        }

        val success = printService.printQRCode(qrData)
        if (success) {
            showToast("QR Code Successfully Printed")
        } else {
            showToast("QR Code Print Failed")
        }
    }

    private fun checkPrinterStatus() {
        if (!printService.isConnected()) {
            showToast("Printer service Not Available")
            return
        }

        val (_, message) = printService.checkPrinterStatus()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result: IntentResult =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result.contents == null) {
            showToast("Scan Ignored")
            lastScanResult = ""
        } else {
            lastScanResult = result.contents
            showToast("Scan Result: ${result.contents}")

            // Print scan result if connected
            if (printService.isConnected()) {
                printService.printScanResult(result.contents)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        printService.disconnect()
    }
}