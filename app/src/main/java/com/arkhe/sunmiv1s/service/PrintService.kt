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

    fun printSampleReceipt(): Boolean {
        if (!isConnected()) return false

        val receiptData = PrinterUtils.generateSampleReceiptData()
        return printReceipt(receiptData)
    }

    @SuppressLint("DefaultLocale")
    fun printReceipt(receiptData: ReceiptData): Boolean {
        if (!isConnected()) return false

        try {
            // Header
            printerManager.printTextWithFont("${receiptData.storeName}\n", 24f, 1)
            printerManager.printText("${receiptData.storeAddress}\n", 1)
            printerManager.printText("${receiptData.storePhone}\n", 1)
            printerManager.printText("================================\n")

            // Items
            printerManager.printText("Item                 Qty  Price\n")
            printerManager.printText("--------------------------------\n")

            receiptData.items.forEach { item ->
                val line = String.format(
                    "%-20s %2d %7d\n",
                    item.name.take(20),
                    item.quantity,
                    item.price
                )
                printerManager.printText(line)
            }

            printerManager.printText("--------------------------------\n")

            // Total
            printerManager.printTextWithFont(
                "Total: Rp ${receiptData.total.formatCurrency()}\n",
                20f,
                2
            )
            printerManager.printText("Cash: Rp ${receiptData.cash.formatCurrency()}\n", 2)
            printerManager.printText("Change: Rp ${receiptData.change.formatCurrency()}\n", 2)

            // Footer
            printerManager.printText("================================\n", 1)
            printerManager.printText("Thank You\n", 1)
            printerManager.printText("Please Come Again\n", 1)
            printerManager.printText("${receiptData.dateTime}\n", 1)
            printerManager.printText("\n\n\n")

            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun printQRCode(qrData: String): Boolean {
        if (!isConnected()) return false

        val success = printerManager.printQRCode(qrData, 8)
        if (success) {
            printerManager.printText("\n=== QR CODE ===\n", 1)
            printerManager.printText("Generated: ${PrinterUtils.getCurrentTime()}\n", 1)
            printerManager.printText("================\n\n", 1)
        }
        return success
    }

    fun printScanResult(scanResult: String): Boolean {
        if (!isConnected()) return false

        return try {
            printerManager.printText("\n=== QR SCAN RESULT ===\n", 1)
            printerManager.printText("$scanResult\n", 0)
            printerManager.printText("Time: ${PrinterUtils.getCurrentDateTime()}\n", 1)
            printerManager.printText("======================\n\n", 1)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun checkPrinterStatus(): Pair<Int, String> {
        if (!isConnected()) return Pair(-1, "Service not connected")

        val status = printerManager.getPrinterStatus()
        val message = PrinterUtils.formatPrinterStatus(status)

        // Print status if printer is normal
        if (status == 0) {
            try {
                printerManager.printText("=== PRINTER STATUS ===\n", 1)
                printerManager.printText("$message\n", 1)
                printerManager.printText("Time: ${PrinterUtils.getCurrentDateTime()}\n", 1)
                printerManager.printText("======================\n\n", 1)
            } catch (_: Exception) {
                // Ignore print error for status
            }
        }

        return Pair(status, message)
    }

    @SuppressLint("DefaultLocale")
    private fun Int.formatCurrency(): String {
        return String.format("%,d", this).replace(",", ".")
    }
}