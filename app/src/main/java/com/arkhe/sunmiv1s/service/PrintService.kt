package com.arkhe.sunmiv1s.service

import android.annotation.SuppressLint
import android.content.Context
import com.arkhe.sunmiv1s.SunmiPrinterManager
import com.arkhe.sunmiv1s.utils.PrinterUtils
import com.arkhe.sunmiv1s.utils.ReceiptData

class PrintService(context: Context) {

    private val printerManager = SunmiPrinterManager(context)

    fun initialize(connectionListener: (Boolean) -> Unit) {
        printerManager.setConnectionListener(connectionListener)
        printerManager.connectService()
    }

    fun disconnect() {
        printerManager.disconnectService()
    }

    fun isConnected(): Boolean {
        return printerManager.isConnected()
    }

    // --- Printing Methods ---
    // These methods now implicitly handle queuing via SunmiPrinterManager
    // The boolean return type might now indicate "command queued/sent" rather than "printed successfully immediately"
    // Consider changing to Unit or providing callbacks if detailed success/failure of each print operation is needed.

    fun printSampleReceipt() { // Changed to Unit, as success is handled by callbacks in manager or assumed queued
        if (!isConnected()) {
            // SunmiPrinterManager will queue this if connectService() is called later
            // Or, you can add a specific log or user feedback here.
            println("PrintService: Not connected, printSampleReceipt will be queued by SunmiPrinterManager if connection is established.")
            // To ensure it gets queued even if connectService wasn't called yet:
            printerManager.connectService() // Ensure a connection attempt is active
        }
        val receiptData = PrinterUtils.generateSampleReceiptData()
        printReceipt(receiptData)
    }

    @SuppressLint("DefaultLocale")
    fun printReceipt(receiptData: ReceiptData) { // Changed to Unit
        if (!isConnected()) {
            println("PrintService: Not connected, printReceipt will be queued.")
            printerManager.connectService()
        }

        // The SunmiPrinterManager now handles queuing, so try-catch here is less about connection
        // and more about issues forming the print data itself, which is unlikely here.
        // The actual print execution and its success/failure will be handled by the ICallback in SunmiPrinterManager.

        // Header
        printerManager.printTextWithFont("${receiptData.storeName}\n", "default", 24f, 1) // ALIGN_CENTER = 1
        printerManager.printText("${receiptData.storeAddress}\n", 1)
        printerManager.printText("${receiptData.storePhone}\n", 1)
        printerManager.printText("================================\n", 1)

        // Items
        printerManager.printText("Item Qty Price\n", 1) // Assuming this is a header, centered
        printerManager.printText("--------------------------------\n", 1)

        receiptData.items.forEach { item ->
            // Adjusted formatting for potentially better alignment. Test this on the device.
            // Using a fixed-width approach.
            val itemName = item.name.padEnd(20).take(20) // Pad and truncate item name
            val quantity = item.quantity.toString().padStart(3)
            val price = item.price.formatCurrency().padStart(8) // Ensure formatCurrency is robust
            val line = "$itemName $quantity $price\n"
            printerManager.printText(line, 0) // ALIGN_LEFT = 0
        }

        printerManager.printText("--------------------------------\n", 1)

        // Total
        printerManager.printTextWithFont(
            "Total: Rp ${receiptData.total.formatCurrency()}\n",
            "default",
            20f,
            2 // ALIGN_RIGHT = 2
        )
        printerManager.printText("Cash: Rp ${receiptData.cash.formatCurrency()}\n", 2)
        printerManager.printText("Change: Rp ${receiptData.change.formatCurrency()}\n", 2)

        // Footer
        printerManager.printText("================================\n", 1)
        printerManager.printText("Thank You\n", 1)
        printerManager.printText("Please Come Again\n", 1)
        printerManager.printText("${receiptData.dateTime}\n", 1)

        // Feed lines
        printerManager.lineWrap(3) // Use lineWrap for multiple blank lines
    }

    fun printQRCode(qrData: String) { // Changed to Unit
        if (!isConnected()) {
            println("PrintService: Not connected, printQRCode will be queued.")
            printerManager.connectService()
        }

        // Again, SunmiPrinterManager handles queuing and actual print execution.
        printerManager.printQRCode(qrData, 8, 3) // 8 for size, 3 for errorLevel (H)
        printerManager.lineWrap(1) // Add some space after QR
        printerManager.printText("=== QR CODE ===\n", 1)
        printerManager.printText("Generated: ${PrinterUtils.getCurrentTime()}\n", 1)
        printerManager.printText("================\n", 1)
        printerManager.lineWrap(3)
    }

    fun printScanResult(scanResult: String) { // Changed to Unit
        if (!isConnected()) {
            println("PrintService: Not connected, printScanResult will be queued.")
            printerManager.connectService()
        }
        printerManager.lineWrap(1)
        printerManager.printText("=== QR SCAN RESULT ===\n", 1)
        printerManager.printText(scanResult + "\n", 0) // Ensure newline if not in scanResult
        printerManager.printText("Time: ${PrinterUtils.getCurrentDateTime()}\n", 1)
        printerManager.printText("======================\n", 1)
        printerManager.lineWrap(3)
    }

    // --- Printer Status ---
    // This method now needs a callback to return the status asynchronously.
    fun checkPrinterStatus(callback: (status: Int, message: String) -> Unit) {
        if (!printerManager.isConnected()) {
            callback(-1, "Service not connected") // Or specific code for not connected
            // Optionally, try to connect if not connected
            // printerManager.connectService()
            return
        }

        printerManager.getPrinterStatus { status ->
            val message = PrinterUtils.formatPrinterStatus(status)
            println("PrintService: checkPrinterStatus received status: $status, message: '$message'")

            // Print status if printer is normal - this itself is an async operation
            if (status == 0) { // STATUS_NORMAL = 0
                try {
                    printerManager.printText("=== PRINTER STATUS ===\n", 1)
                    printerManager.printText("$message\n", 1)
                    printerManager.printText("Time: ${PrinterUtils.getCurrentDateTime()}\n", 1)
                    printerManager.printText("======================\n", 1)
                    printerManager.lineWrap(3)
                } catch (e: Exception) {
                    // This catch is for exceptions during the *preparation* of print commands,
                    // not the print execution itself.
                    println("PrintService: Error preparing printer status text: ${e.message}")
                }
            }
            // Invoke the callback with the retrieved status and message
            callback(status, message)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun Int.formatCurrency(): String {
        // Consider locale-specific formatting if this app is used in different regions.
        // For Indonesian Rupiah, removing grouping separators for printing might be clearer
        // or using a specific format that works well on thermal printers.
        // The current String.format("%,d", this).replace(",", ".") is okay for display
        // but for printing, just the number might be better e.g. item.price.toString()
        // Or ensure your receipt format handles this well.
        return String.format("%,d", this).replace(",", ".")
    }
}
