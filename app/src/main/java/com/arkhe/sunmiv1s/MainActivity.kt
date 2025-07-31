@file:Suppress("DEPRECATION")

package com.arkhe.sunmiv1s

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var printService: PrintService
    private var isServiceConnected by mutableStateOf(false)
    private var showQRDialog by mutableStateOf(false)
    private var lastScanResult by mutableStateOf("")
    private var connectionStatusMessage by mutableStateOf("Initializing...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity onCreate started")

        // Initialize print service with detailed logging
        printService = PrintService(this)

        // Show connection status
        connectionStatusMessage = "Connecting to printer service..."

        printService.initialize { connected ->
            Log.d(TAG, "Service connection status changed: $connected")
            isServiceConnected = connected
            connectionStatusMessage = if (connected) {
                "Printer service connected ✓"
            } else {
                "Printer service disconnected ✗"
            }

            // Show toast for connection status
            runOnUiThread {
                showToast(connectionStatusMessage)
            }

            // If connected, test printer immediately
            if (connected) {
                lifecycleScope.launch {
                    delay(1000) // Wait a bit for service to fully initialize
                    testPrinterConnection()
                }
            }
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

    private fun testPrinterConnection() {
        Log.d(TAG, "Testing printer connection...")
        if (!printService.isConnected()) {
            Log.w(TAG, "Print service not connected for test")
            showToast("Print service not connected")
            return
        }

        // Test basic printer status
        val (status, message) = printService.checkPrinterStatus()
        Log.d(TAG, "Printer test result - Status: $status, Message: $message")

        runOnUiThread {
            showToast("Printer Test: $message")
        }
    }

    private fun printReceipt() {
        Log.d(TAG, "Print receipt button clicked")

        if (!isServiceConnected) {
            Log.w(TAG, "Service not connected for receipt printing")
            showToast("Printer Service Not Available")
            return
        }

        Log.d(TAG, "Attempting to print receipt...")
        showToast("Printing receipt...")

        try {
            val success = printService.printSampleReceipt()
            Log.d(TAG, "Print receipt result: $success")

            if (success) {
                showToast("Print Receipt Success ✓")
            } else {
                showToast("Print Receipt Failed ✗")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during receipt printing", e)
            showToast("Print Receipt Error: ${e.message}")
        }
    }

    private fun printCustomQRCode(qrData: String) {
        Log.d(TAG, "Print QR code: $qrData")

        if (!printService.isConnected()) {
            showToast("Printer service Not Available")
            return
        }

        showToast("Printing QR code...")

        try {
            val success = printService.printQRCode(qrData)
            Log.d(TAG, "Print QR result: $success")

            if (success) {
                showToast("QR Code Successfully Printed ✓")
            } else {
                showToast("QR Code Print Failed ✗")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during QR printing", e)
            showToast("QR Print Error: ${e.message}")
        }
    }

    private fun checkPrinterStatus() {
        Log.d(TAG, "Check printer status clicked")

        if (!printService.isConnected()) {
            showToast("Printer service Not Available")
            return
        }

        try {
            val (status, message) = printService.checkPrinterStatus()
            Log.d(TAG, "Printer status check - Status: $status, Message: $message")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during status check", e)
            showToast("Status Check Error: ${e.message}")
        }
    }

    private fun scanQRCode() {
        Log.d(TAG, "Scan QR code clicked")

        try {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan QR Code")
            integrator.setCameraId(0)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(false)
            integrator.setOrientationLocked(true)
            integrator.initiateScan()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during QR scan initialization", e)
            showToast("QR Scan Error: ${e.message}")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            val result: IntentResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

            if (result.contents == null) {
                Log.d(TAG, "QR scan cancelled")
                showToast("Scan Cancelled")
                lastScanResult = ""
            } else {
                Log.d(TAG, "QR scan result: ${result.contents}")
                lastScanResult = result.contents
                showToast("Scan Result: ${result.contents}")

                // Print scan result if connected
                if (printService.isConnected()) {
                    Log.d(TAG, "Printing scan result")
                    val success = printService.printScanResult(result.contents)
                    Log.d(TAG, "Print scan result success: $success")
                } else {
                    Log.w(TAG, "Cannot print scan result - service not connected")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception processing scan result", e)
            showToast("Scan Processing Error: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Toast: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        Log.d(TAG, "MainActivity onDestroy")
        super.onDestroy()
        printService.disconnect()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume - Service connected: $isServiceConnected")
    }
}