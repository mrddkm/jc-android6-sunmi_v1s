package com.arkhe.sunmiv1s.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrinterUtils {

    fun generateSampleReceiptData(): ReceiptData {
        val items = listOf(
            ReceiptItem("Nasi Gudeg", 2, 15000),
            ReceiptItem("Es Teh Manis", 2, 5000),
            ReceiptItem("Kerupuk", 1, 3000)
        )

        val total = items.sumOf { it.price * it.quantity }
        val cash = 50000
        val change = cash - total

        return ReceiptData(
            storeName = "Warung Makan Pak Budi",
            storeAddress = "Jl. Malioboro No. 123, Yogyakarta",
            storePhone = "Telp: (0274) 123456",
            items = items,
            total = total,
            cash = cash,
            change = change,
            dateTime = getCurrentDateTime()
        )
    }

    fun getCurrentDateTime(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun getCurrentTime(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    fun formatPrinterStatus(status: Int): String {
        return when (status) {
            0 -> "Printer Normal ✅"
            1 -> "Printer sedang mempersiapkan ⏳"
            2 -> "Printer mengalami kesalahan komunikasi ❌"
            3 -> "Kertas habis 📄"
            4 -> "Printer terlalu panas 🔥"
            5 -> "Cover printer terbuka 📂"
            6 -> "Cutter bermasalah ✂️"
            7 -> "Cutter habis ✂️❌"
            8 -> "Black mark tidak terdeteksi ⚫"
            9 -> "Printer sedang upgrade firmware 🔄"
            -1 -> "Gagal mengecek status printer ❌"
            else -> "Status tidak dikenal: $status ❓"
        }
    }
}