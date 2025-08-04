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
import com.arkhe.sunmiv1s.utils.ServiceChecker
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CONNECTION_RETRY_DELAY = 2000L
        private const val MAX_CONNECTION_RETRIES = 3
    }

    private lateinit var printService: PrintService
    private var isServiceConnected by mutableStateOf(false)
    private var showQRDialog by mutableStateOf(false)
    private var lastScanResult by mutableStateOf("")
    private var connectionStatusMessage by mutableStateOf("Initializing...")
    private var connectionRetryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity onCreate started")

        // Initialize print service with detailed logging
        initializePrintService()

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
                        onCheckServices = { checkSunmiServices() },
                        lastScanResult = lastScanResult,
                        showQRDialog = showQRDialog,
                        onShowQRDialog = { showQRDialog = it },
                        onPrintCustomQR = { qrData -> printCustomQRCode(qrData) }
                    )
                }
            }
        }
    }

    private fun initializePrintService() {
        // Check if Sunmi services are available first
        val serviceCheck = ServiceChecker.checkSunmiServices(this)
        Log.d(TAG, "Service check result:\n${serviceCheck.getReport()}")

        if (!serviceCheck.hasAnyService) {
            Log.w(TAG, "No Sunmi services found on this device")
            connectionStatusMessage = "No Sunmi services found ✗"
            showToast("Device tidak memiliki service printer Sunmi")
            return
        }

        printService = PrintService(this)
        connectionStatusMessage = "Connecting to printer service..."

        printService.initialize { connected ->
            Log.d(TAG, "Service connection status changed: $connected")
            isServiceConnected = connected

            if (connected) {
                connectionStatusMessage = "Printer service connected ✓"
                connectionRetryCount = 0

                // Show success toast
                runOnUiThread {
                    showToast(connectionStatusMessage)
                }

                // Test printer after successful connection
                lifecycleScope.launch {
                    delay(1500) // Wait for service to fully initialize
                    testPrinterConnection()
                }
            } else {
                connectionStatusMessage = "Printer service disconnected ✗"

                // Show error toast
                runOnUiThread {
                    showToast(connectionStatusMessage)
                }

                // Retry connection if not exceeded max retries
                if (connectionRetryCount < MAX_CONNECTION_RETRIES) {
                    connectionRetryCount++
                    Log.d(TAG, "Retrying connection... Attempt $connectionRetryCount")

                    lifecycleScope.launch {
                        delay(CONNECTION_RETRY_DELAY)
                        retryConnection()
                    }
                } else {
                    Log.w(TAG, "Max connection retries reached")
                    runOnUiThread {
                        showToast("Failed to connect after $MAX_CONNECTION_RETRIES attempts")
                    }
                }
            }
        }
    }

    private fun retryConnection() {
        Log.d(TAG, "Retrying printer service connection...")
        connectionStatusMessage = "Retrying connection... ($connectionRetryCount/$MAX_CONNECTION_RETRIES)"

        runOnUiThread {
            showToast(connectionStatusMessage)
        }

        printService.disconnect()

        // Wait a bit before reconnecting
        lifecycleScope.launch {
            delay(1000)
            initializePrintService()
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
            showToast("Printer Status: $message")
        }
    }

    private fun printReceipt() {
        Log.d(TAG, "Print receipt button clicked")

        if (!isServiceConnected) {
            Log.w(TAG, "Service not connected for receipt printing")
            showToast("Printer Service Not Available")
            return
        }

        if (!printService.isConnected()) {
            Log.w(TAG, "Print service not properly connected")
            showToast("Printer Not Ready")
            return
        }

        Log.d(TAG, "Attempting to print receipt...")
        showToast("Printing receipt...")

        // Use coroutine to prevent blocking UI
        lifecycleScope.launch {
            try {
                val success = printService.printSampleReceipt()
                Log.d(TAG, "Print receipt result: $success")

                runOnUiThread {
                    if (success) {
                        showToast("Receipt printed successfully ✓")
                    } else {
                        showToast("Failed to print receipt ✗")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during receipt printing", e)
                runOnUiThread {
                    showToast("Print error: ${e.message}")
                }
            }
        }
    }

    private fun printCustomQRCode(qrData: String) {
        Log.d(TAG, "Print QR code: $qrData")

        if (!printService.isConnected()) {
            showToast("Printer service not available")
            return
        }

        showToast("Printing QR code...")

        lifecycleScope.launch {
            try {
                val success = printService.printQRCode(qrData)
                Log.d(TAG, "Print QR result: $success")

                runOnUiThread {
                    if (success) {
                        showToast("QR code printed successfully ✓")
                    } else {
                        showToast("Failed to print QR code ✗")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during QR printing", e)
                runOnUiThread {
                    showToast("QR print error: ${e.message}")
                }
            }
        }
    }

    private fun checkPrinterStatus() {
        Log.d(TAG, "Check printer status clicked")

        if (!printService.isConnected()) {
            showToast("Printer service not available")
            return
        }

        lifecycleScope.launch {
            try {
                val (status, message) = printService.checkPrinterStatus()
                Log.d(TAG, "Printer status check - Status: $status, Message: $message")

                runOnUiThread {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during status check", e)
                runOnUiThread {
                    showToast("Status check error: ${e.message}")
                }
            }
        }
    }

    private fun checkSunmiServices() {
        Log.d(TAG, "Check Sunmi services clicked")

        lifecycleScope.launch {
            try {
                val serviceCheck = ServiceChecker.checkSunmiServices(this@MainActivity)
                val report = serviceCheck.getReport()

                Log.d(TAG, "Service check report:\n$report")

                runOnUiThread {
                    // Show detailed report in a toast (truncated) and log full report
                    val shortReport = if (serviceCheck.hasAnyService) {
                        "✅ Sunmi services found"
                    } else {
                        "❌ No Sunmi services found"
                    }

                    Toast.makeText(this@MainActivity, shortReport, Toast.LENGTH_LONG).show()

                    // Log full report for debugging
                    Log.i(TAG, "Full service report:\n$report")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during service check", e)
                runOnUiThread {
                    showToast("Service check error: ${e.message}")
                }
            }
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
            showToast("QR scan error: ${e.message}")
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
                showToast("Scan cancelled")
                lastScanResult = ""
            } else {
                Log.d(TAG, "QR scan result: ${result.contents}")
                lastScanResult = result.contents
                showToast("Scanned: ${result.contents}")

                // Print scan result if connected
                if (printService.isConnected()) {
                    Log.d(TAG, "Printing scan result")

                    lifecycleScope.launch {
                        val success = printService.printScanResult(result.contents)
                        Log.d(TAG, "Print scan result success: $success")

                        runOnUiThread {
                            if (success) {
                                showToast("Scan result printed ✓")
                            } else {
                                showToast("Failed to print scan result ✗")
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Cannot print scan result - service not connected")
                    showToast("Scanned but printer not ready")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception processing scan result", e)
            showToast("Scan processing error: ${e.message}")
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

        // Check connection status on resume
        if (!isServiceConnected && connectionRetryCount < MAX_CONNECTION_RETRIES) {
            Log.d(TAG, "Service not connected on resume, attempting reconnection...")
            lifecycleScope.launch {
                delay(1000)
                initializePrintService()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity onPause")
    }
}