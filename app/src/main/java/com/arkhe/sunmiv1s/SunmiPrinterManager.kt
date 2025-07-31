package com.arkhe.sunmiv1s

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

/**
 * Sunmi V1s Printer Manager
 * Updated for proper V1s printer connection
 */
class SunmiPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "SunmiV1sPrinterManager"

        // Correct service info for Sunmi V1s
        private const val SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
        private const val SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"

        // Alternative service info if the above doesn't work
        private const val ALT_SERVICE_PACKAGE = "com.sunmi.printerservice"
        private const val ALT_SERVICE_ACTION = "com.sunmi.peripheral.printer.InnerPrinterService"
    }

    private var woyouService: Any? = null
    private var isServiceConnected = false
    private var connectionListener: ((Boolean) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                Log.d(TAG, "Service connected: ${name?.className}")

                // Try to get IWoyouService using reflection
                woyouService = try {
                    val serviceClass = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService\$Stub")
                    val asInterfaceMethod = serviceClass.getMethod("asInterface", IBinder::class.java)
                    asInterfaceMethod.invoke(null, service)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get IWoyouService, trying alternative approach: ${e.message}")
                    service // Use IBinder directly as fallback
                }

                isServiceConnected = true
                connectionListener?.invoke(true)
                Log.d(TAG, "Sunmi V1s printer service connected successfully")

                // Test printer status immediately after connection
                testConnection()

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
            Log.d(TAG, "Attempting to connect to Sunmi V1s printer service...")

            // Try primary service first
            val intent = Intent().apply {
                setPackage(SERVICE_PACKAGE)
                action = SERVICE_ACTION
            }

            val success = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            if (!success) {
                Log.w(TAG, "Primary service connection failed, trying alternative...")
                // Try alternative service
                val altIntent = Intent().apply {
                    setPackage(ALT_SERVICE_PACKAGE)
                    action = ALT_SERVICE_ACTION
                }
                context.bindService(altIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            } else {
                Log.d(TAG, "Service binding initiated successfully")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind Sunmi service: ${e.message}")
            false
        }
    }

    private fun testConnection() {
        try {
            Log.d(TAG, "Testing printer connection...")
            val status = getPrinterStatus()
            Log.d(TAG, "Printer status: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}")
        }
    }

    fun disconnectService() {
        if (isServiceConnected) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service: ${e.message}")
            }
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

            Log.d(TAG, "Printing text: $text")

            // Set alignment: 0=left, 1=center, 2=right
            invokeMethod("setAlignment", alignment, null)

            // Print text
            invokeMethod("printText", text, null)

            Log.d(TAG, "Text print command sent successfully")
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

            Log.d(TAG, "Printing text with font - Text: $text, Size: $fontSize")

            // Set alignment
            invokeMethod("setAlignment", alignment, null)

            // Print text with font size
            invokeMethod("printTextWithFont", text, null, fontSize, null)

            Log.d(TAG, "Font text print command sent successfully")
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

            Log.d(TAG, "Printing QR Code: $data")

            // Print QR Code (data, module size, error level)
            invokeMethod("printQRCode", data, moduleSize, 0, null)

            Log.d(TAG, "QR Code print command sent successfully")
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
                Log.w(TAG, "Service not connected for status check")
                return -1
            }

            val method = woyouService!!.javaClass.getMethod("getPrinterStatus")
            val status = method.invoke(woyouService) as Int
            Log.d(TAG, "Printer status retrieved: $status")
            status
        } catch (e: Exception) {
            Log.e(TAG, "Error getting printer status: ${e.message}")
            -1
        }
    }

    /**
     * Print sample receipt - simplified version for V1s
     */
    fun printSampleReceipt(): Boolean {
        return try {
            if (!isServiceConnected) {
                Log.w(TAG, "Service not connected for receipt printing")
                return false
            }

            Log.d(TAG, "Starting sample receipt print...")

            // Simple receipt for testing
            printTextWithFont("SAMPLE RECEIPT\n", 24f, 1)
            printText("=======================\n")
            printText("Item 1               15000\n")
            printText("Item 2               10000\n")
            printText ("-----------------------\n")
            printTextWithFont("Total: Rp 25,000\n", 20f, 2)
            printText("=======================\n")
            printText("Thank You!\n", 1)
            printText("\n\n\n")

            Log.d(TAG, "Sample receipt print completed")
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

            Log.d(TAG, "Invoking method: $methodName with args: ${args.contentToString()}")

            val parameterTypes = args.map { arg ->
                when (arg) {
                    is String -> String::class.java
                    is Int -> Int::class.javaPrimitiveType
                    is Float -> Float::class.javaPrimitiveType
                    is Boolean -> Boolean::class.javaPrimitiveType
                    null -> {
                        // For callback parameter
                        try {
                            Class.forName("woyou.aidlservice.jiuiv5.ICallback")
                        } catch (_: Exception) {
                            // Fallback if ICallback class not found
                            Any::class.java
                        }
                    }
                    else -> arg.javaClass
                }
            }.toTypedArray()

            val method = service.javaClass.getMethod(methodName, *parameterTypes)
            val result = method.invoke(service, *args)

            Log.d(TAG, "Method $methodName invoked successfully")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking method $methodName: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}