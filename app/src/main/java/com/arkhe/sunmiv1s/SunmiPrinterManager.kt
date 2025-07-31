package com.arkhe.sunmiv1s

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

/**
 * Sunmi Printer Manager
 * Connection and operation Sunmi V1s printer
 */
class SunmiPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "SunmiPrinterManager"
        private const val SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
        private const val SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
    }

    private var woyouService: Any? = null
    private var isServiceConnected = false
    private var connectionListener: ((Boolean) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val serviceClass = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService\$Stub")
                val asInterfaceMethod = serviceClass.getMethod("asInterface", IBinder::class.java)
                woyouService = asInterfaceMethod.invoke(null, service)

                isServiceConnected = true
                connectionListener?.invoke(true)
                Log.d(TAG, "Sunmi printer service connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Sunmi service: ${e.message}")
                isServiceConnected = false
                connectionListener?.invoke(false)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            woyouService = null
            isServiceConnected = false
            connectionListener?.invoke(false)
            Log.d(TAG, "Sunmi printer service disconnected")
        }
    }

    fun setConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    fun connectService(): Boolean {
        return try {
            val intent = Intent().apply {
                setPackage(SERVICE_PACKAGE)
                action = SERVICE_ACTION
            }
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind Sunmi service: ${e.message}")
            false
        }
    }

    fun disconnectService() {
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
            connectionListener?.invoke(false)
        }
    }

    fun isConnected(): Boolean = isServiceConnected

    /**
     * Print text with alignment
     */
    fun printText(text: String, alignment: Int = 0): Boolean {
        return try {
            if (!isServiceConnected || woyouService == null) {
                Log.w(TAG, "Service not connected")
                return false
            }

            // Set alignment: 0=left, 1=center, 2=right
            invokeMethod("setAlignment", alignment, null)

            // Print text
            invokeMethod("printText", text, null)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing text: ${e.message}")
            false
        }
    }

    /**
     * Print text with font size
     */
    fun printTextWithFont(text: String, fontSize: Float, alignment: Int = 0): Boolean {
        return try {
            if (!isServiceConnected || woyouService == null) {
                Log.w(TAG, "Service not connected")
                return false
            }

            invokeMethod("setAlignment", alignment, null)

            invokeMethod("printTextWithFont", text, null, fontSize, null)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing text with font: ${e.message}")
            false
        }
    }

    /**
     * Print QR Code
     */
    fun printQRCode(data: String, moduleSize: Int = 8): Boolean {
        return try {
            if (!isServiceConnected || woyouService == null) {
                Log.w(TAG, "Service not connected")
                return false
            }

            // Print QR Code (data, module size, error level)
            invokeMethod("printQRCode", data, moduleSize, 0, null)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing QR code: ${e.message}")
            false
        }
    }

    /**
     * Get printer status
     */
    fun getPrinterStatus(): Int {
        return try {
            if (!isServiceConnected || woyouService == null) {
                return -1
            }

            val method = woyouService!!.javaClass.getMethod("getPrinterStatus")
            method.invoke(woyouService) as Int
        } catch (e: Exception) {
            Log.e(TAG, "Error getting printer status: ${e.message}")
            -1
        }
    }

    /**
     * Print sample receipt
     */
    fun printSampleReceipt(): Boolean {
        return try {
            if (!isServiceConnected) {
                return false
            }

            // Header
            printTextWithFont("GAENTA\n", 24f, 1)
            printText("Jl. Indonesia No. 123\n", 1)
            printText("Phone: 021-12345678\n", 1)
            printText("================================\n")

            // Items
            printText("Item                 Qty  Price\n")
            printText("--------------------------------\n")
            printText("Black Coffee          1   15000\n")
            printText("Roti Bakar            2   10000\n")
            printText("Air Mineral           1    5000\n")
            printText("--------------------------------\n")

            // Total
            printTextWithFont("Total: Rp 40,000\n", 20f, 2)
            printText("Cash: Rp 50,000\n", 2)
            printText("Change: Rp 10,000\n", 2)

            // Footer
            printText("================================\n", 1)
            printText("Thank You\n", 1)
            printText("Have a Good Time\n", 1)
            printText("\n\n\n")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing sample receipt: ${e.message}")
            false
        }
    }

    /**
     * Helper method for invoke method with reflection
     */
    private fun invokeMethod(methodName: String, vararg args: Any?): Any? {
        return try {
            val service = woyouService ?: return null
            val parameterTypes = args.map { arg ->
                when (arg) {
                    is String -> String::class.java
                    is Int -> Int::class.javaPrimitiveType
                    is Float -> Float::class.javaPrimitiveType
                    is Boolean -> Boolean::class.javaPrimitiveType
                    null -> Class.forName("woyou.aidlservice.jiuiv5.ICallback")
                    else -> arg.javaClass
                }
            }.toTypedArray()

            val method = service.javaClass.getMethod(methodName, *parameterTypes)
            method.invoke(service, *args)
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking method $methodName: ${e.message}")
            null
        }
    }
}