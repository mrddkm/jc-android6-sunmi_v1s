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
        private const val CONNECTION_RETRY_DELAY = 2000L // Milliseconds
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

        // Initialize PrintService instance
        printService = PrintService(this) // Initialize here

        // Attempt to connect to the printer service
        initializePrintServiceConnection() // Renamed for clarity

        setContent {
            SunmiV1sTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceConnected = isServiceConnected,
                        connectionStatusMessage = connectionStatusMessage, // Pass status message
                        onPrintReceipt = { printReceipt() },
                        onScanQR = { scanQRCode() },
                        onPrintQR = { showQRDialog = true }, // To show the dialog for custom QR
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

    private fun initializePrintServiceConnection() {
        Log.d(TAG, "Attempting to initialize printer service connection...")
        connectionStatusMessage = "Checking for Sunmi services..."

        val serviceCheck = ServiceChecker.checkSunmiServices(this)
        Log.d(TAG, "Sunmi service check report:\n${serviceCheck.getReport()}")

        if (!serviceCheck.hasAnyService) {
            Log.w(TAG, "No Sunmi printer services found on this device.")
            connectionStatusMessage = "No Sunmi printer services found ✗"
            showToast("This device does not have Sunmi printer services.")
            isServiceConnected = false // Ensure state reflects this
            return
        }

        // printService is already initialized in onCreate
        connectionStatusMessage = "Connecting to printer service..."
        showToast(connectionStatusMessage) // Give initial feedback

        printService.initialize { connected ->
            runOnUiThread { // Ensure UI updates are on the main thread
                isServiceConnected = connected
                if (connected) {
                    connectionStatusMessage = "Printer service connected ✓"
                    connectionRetryCount = 0 // Reset retries on successful connection
                    Log.i(TAG, connectionStatusMessage)
                    showToast(connectionStatusMessage)

                    // Test printer connection automatically after a slight delay
                    lifecycleScope.launch {
                        delay(1500) // Wait for service to fully stabilize
                        testPrinterAfterConnection()
                    }
                } else {
                    connectionStatusMessage = "Printer service disconnected ✗"
                    Log.w(TAG, connectionStatusMessage)
                    showToast(connectionStatusMessage)
                    // No automatic retry from here; onResume or manual action will trigger retries
                }
            }
        }
    }

    private fun attemptReconnection() {
        if (connectionRetryCount < MAX_CONNECTION_RETRIES) {
            connectionRetryCount++
            Log.i(TAG, "Attempting to reconnect... (Attempt $connectionRetryCount/$MAX_CONNECTION_RETRIES)")
            connectionStatusMessage = "Reconnecting... (Attempt $connectionRetryCount)"
            showToast(connectionStatusMessage)

            // Disconnect first to ensure a clean state, then re-initialize
            printService.disconnect()
            lifecycleScope.launch {
                delay(CONNECTION_RETRY_DELAY) // Wait before retrying
                initializePrintServiceConnection()
            }
        } else {
            Log.w(TAG, "Max connection retries reached. Please check printer or restart app.")
            connectionStatusMessage = "Connection failed after $MAX_CONNECTION_RETRIES attempts ✗"
            showToast("Failed to connect after $MAX_CONNECTION_RETRIES attempts. Check printer.")
        }
    }


    private fun testPrinterAfterConnection() {
        Log.d(TAG, "Testing printer connection after successful service bind...")
        if (!isServiceConnected) { // Double check, should be true here
            Log.w(TAG, "Service not connected for automated test. This is unexpected.")
            return
        }
        // Use the new callback version of checkPrinterStatus
        printService.checkPrinterStatus { status, message ->
            runOnUiThread {
                Log.d(TAG, "Automated printer test result - Status: $status, Message: $message")
                showToast("Printer Status (Auto-Test): $message")
                // Optionally update connectionStatusMessage based on this detailed status
                // if (status != 0) connectionStatusMessage = "Printer: $message"
            }
        }
    }

    private fun printReceipt() {
        Log.d(TAG, "Print receipt button clicked")
        if (!ensureServiceConnectedWithMessage()) return

        showToast("Printing sample receipt...")
        // PrintService.printSampleReceipt now returns Unit
        // Success/failure is handled by ICallback within SunmiPrinterManager
        // For MainActivity, we assume the command is sent if isServiceConnected.
        printService.printSampleReceipt()
        // We can add a generic "Command sent" message, but specific success is asynchronous
        // showToast("Print command sent. Check printer.")
    }

    private fun printCustomQRCode(qrData: String) {
        Log.d(TAG, "Print custom QR code: $qrData")
        if (qrData.isBlank()) {
            showToast("QR data cannot be empty.")
            return
        }
        if (!ensureServiceConnectedWithMessage()) return

        showToast("Printing QR code: $qrData")
        printService.printQRCode(qrData)
        // showToast("QR print command sent.")
    }

    private fun checkPrinterStatus() {
        Log.d(TAG, "Check printer status button clicked")
        if (!ensureServiceConnectedWithMessage(promptReconnect = true)) return

        showToast("Checking printer status...") // Feedback that action started

        // Use the new callback mechanism
        printService.checkPrinterStatus { status, message ->
            runOnUiThread { // Ensure UI updates (Toast) are on the main thread
                Log.d(TAG, "Manual printer status check - Status: $status, Message: $message")
                // Display with a longer duration for statuses
                Toast.makeText(this@MainActivity, "Printer Status: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkSunmiServices() {
        Log.d(TAG, "Check Sunmi services button clicked")
        // This is a synchronous check from ServiceChecker
        val serviceCheck = ServiceChecker.checkSunmiServices(this@MainActivity)
        val report = serviceCheck.getReport()
        Log.d(TAG, "Service check report:\n$report")

        val shortReport = if (serviceCheck.hasAnyService) {
            "✅ Sunmi services found. ${serviceCheck.getReport()}"
        } else {
            "❌ No Sunmi services found on this device."
        }
        Toast.makeText(this@MainActivity, shortReport, Toast.LENGTH_LONG).show()
        // Log full report for debugging
        Log.i(TAG, "Full service report by manual check:\n$report")
    }


    private fun scanQRCode() {
        Log.d(TAG, "Scan QR code button clicked")
        try {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan a QR Code")
            integrator.setCameraId(0) // Default camera
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(false) // Not needed for just content
            integrator.setOrientationLocked(false) // Allow user to rotate
            integrator.initiateScan()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during QR scan initialization", e)
            showToast("Could not start QR scanner: ${e.message}")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Log.d(TAG, "QR scan cancelled by user.")
                showToast("Scan cancelled")
                lastScanResult = "" // Clear last scan
            } else {
                Log.i(TAG, "QR scan successful. Contents: ${result.contents}")
                lastScanResult = result.contents
                showToast("Scanned: ${result.contents.take(100)}") // Show a snippet

                // Optionally print the scanned QR code content
                if (ensureServiceConnectedWithMessage(promptReconnect = false)) { // Don't prompt aggressively here
                    Log.d(TAG, "Printing scanned QR content: ${result.contents}")
                    showToast("Printing scanned result...")
                    printService.printScanResult(result.contents)
                    // showToast("Print command for scanned result sent.")
                } else {
                    Log.w(TAG, "Scanned successfully, but printer not connected to print the result.")
                    // showToast("Scanned. Printer not ready to print result.") // Already handled by ensureServiceConnected
                }
            }
        } else {
            // Fallback for other activity results if any
            Log.d(TAG, "onActivityResult: Result not handled by QR scanner integration.")
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Log.d(TAG, "Toast: $message") // Log all toasts for debugging
        Toast.makeText(this, message, duration).show()
    }

    // Helper to check service connection and show appropriate messages
    private fun ensureServiceConnectedWithMessage(promptReconnect: Boolean = true): Boolean {
        if (!isServiceConnected) {
            Log.w(TAG, "Action requires printer service, but it's not connected.")
            var message = "Printer service not connected. Please wait or check connection."
            if (promptReconnect && connectionRetryCount >= MAX_CONNECTION_RETRIES) {
                message += "\nMax retries reached. Please check printer & restart app."
            }
            showToast(message, Toast.LENGTH_LONG)

            // If not connected and max retries not reached, and prompt is true, attempt reconnect
            if (promptReconnect && connectionRetryCount < MAX_CONNECTION_RETRIES) {
                Log.d(TAG, "Prompting reconnection from ensureServiceConnectedWithMessage.")
                attemptReconnection() // Try to reconnect if appropriate
            }
            return false
        }
        return true
    }


    override fun onResume() {
        super.onResume()
        Log.i(TAG, "MainActivity onResume. Service connected: $isServiceConnected, Retry count: $connectionRetryCount")
        // If not connected and haven't exhausted retries, try to connect.
        // This handles cases where the service might have disconnected while paused,
        // or initial connection failed and user brings app to foreground.
        if (!isServiceConnected && connectionRetryCount < MAX_CONNECTION_RETRIES) {
            Log.d(TAG, "Service not connected on resume, attempting to initialize/reconnect...")
            // Delay slightly to allow system to settle if app is just starting
            lifecycleScope.launch {
                delay(1000) // 1 second delay
                initializePrintServiceConnection()
            }
        } else if (isServiceConnected) {
            // If already connected, maybe a quick status check or ensure UI reflects connected state
            connectionStatusMessage = "Printer service connected ✓"
        } else {
            connectionStatusMessage = "Connection failed after $MAX_CONNECTION_RETRIES attempts ✗"
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity onPause")
        // Note: We are not disconnecting the service onPause by default
        // to allow printing to continue if it was initiated and the app is briefly paused.
        // The service connection is managed by SunmiPrinterManager's lifecycle.
    }

    override fun onDestroy() {
        Log.i(TAG, "MainActivity onDestroy. Disconnecting print service.")
        super.onDestroy()
        // It's important to disconnect the service when the activity is destroyed
        // to release resources and unbind the service.
        if (::printService.isInitialized) { // Check if printService was initialized
            printService.disconnect()
        }
    }
}
