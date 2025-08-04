package com.arkhe.sunmiv1s

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import woyou.aidlservice.jiuiv5.IWoyouService
import java.lang.reflect.Proxy

/**
 * Enhanced Sunmi V1s Printer Manager with Multiple Connection Strategies
 * Handles Android 11+ package visibility and multiple fallback approaches
 */
class SunmiPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "SunmiV1sPrinterManager"

        // Primary service configuration for V1s
        private const val PRIMARY_SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
        private const val PRIMARY_SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"

        // Alternative service configurations
        private val ALTERNATIVE_SERVICES = listOf(
            ServiceConfig("com.sunmi.printerservice", "com.sunmi.printerservice.IWoyouService"),
            ServiceConfig(
                "woyou.aidlservice.jiuiv5.main",
                "woyou.aidlservice.jiuiv5.IWoyouService"
            ),
            ServiceConfig("com.sunmi.sunmiservice", "woyou.aidlservice.jiuiv5.IWoyouService")
        )
    }

    data class ServiceConfig(
        val packageName: String,
        val action: String
    )

    // This will hold the actual AIDL proxy instance
    private var iWoyouService: IWoyouService? = null

    // This will be used by invokeMethod, can be kept as Any if you prefer late binding style
    private var woyouService: Any? = null
    private var isServiceConnected = false
    private var connectionListener: ((Boolean) -> Unit)? = null
    private var currentServiceConfig: ServiceConfig? = null
    private var connectionAttempts = 0
    private val maxConnectionAttempts = ALTERNATIVE_SERVICES.size + 1


    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "🔌 Service connected: ${name?.className}")
            // Critical: Get the IWoyouService interface
            val serviceInterface = IWoyouService.Stub.asInterface(service)

            if (serviceInterface != null) {
                iWoyouService = serviceInterface // Store the typed interface
                woyouService = serviceInterface  // Store for reflection in invokeMethod
                isServiceConnected = true
                Log.i(TAG, "✅ IWoyouService interface obtained successfully.")
                connectionListener?.invoke(true)
            } else {
                Log.e(
                    TAG,
                    "❌ IWoyouService is null after asInterface. This should not happen if AIDL is correct."
                )
                iWoyouService = null
                woyouService = null
                isServiceConnected = false // Ensure this is false
                connectionListener?.invoke(false)
                // Optionally, trigger fallback here if primary connection fails at this stage
                // connectServiceWithFallback()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "🔌 Service disconnected: ${name?.className}")
            isServiceConnected = false
            woyouService = null
            iWoyouService = null
            connectionListener?.invoke(false)
        }
    }

    fun setConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    fun connectService() {
        if (isServiceConnected && iWoyouService != null) { // Check iWoyouService too
            Log.d(TAG, "Service already connected.")
            connectionListener?.invoke(true)
            return
        }
        isServiceConnected = false // Reset status before new attempt
        iWoyouService = null
        woyouService = null
        connectionAttempts = 0
        connectServiceWithFallback()
    }

    private fun connectServiceWithFallback() {
        if (connectionAttempts >= maxConnectionAttempts) {
            Log.e(TAG, "❌ Max connection attempts reached. Failed to connect to any Sunmi service.")
            connectionListener?.invoke(false)
            return
        }

        val servicesToTry = listOf(
            ServiceConfig(PRIMARY_SERVICE_PACKAGE, PRIMARY_SERVICE_ACTION)
        ) + ALTERNATIVE_SERVICES

        currentServiceConfig = servicesToTry.getOrNull(connectionAttempts)
        connectionAttempts++


        if (currentServiceConfig == null) {
            Log.e(TAG, "❌ No more service configurations to try.")
            connectionListener?.invoke(false)
            return
        }

        Log.d(
            TAG,
            "Attempt #${connectionAttempts}: Trying to connect to service ${currentServiceConfig?.packageName}..."
        )

        val intent = Intent()
        intent.setPackage(currentServiceConfig?.packageName)
        intent.action = currentServiceConfig?.action

        try {
            val isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (isBound) {
                Log.i(TAG, "Binding service to ${currentServiceConfig?.packageName}...")
            } else {
                Log.w(
                    TAG,
                    "bindService returned false for ${currentServiceConfig?.packageName}. Retrying with next..."
                )
                connectServiceWithFallback()
            }
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "SecurityException during bindService for ${currentServiceConfig?.packageName}: ${e.message}. " +
                        "Ensure <queries> tag for package visibility is in AndroidManifest.xml for Android 11+.",
                e
            )
            connectServiceWithFallback() // Try next
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Exception during bindService for ${currentServiceConfig?.packageName}: ${e.message}. Retrying with next...",
                e
            )
            connectServiceWithFallback()
        }
    }

    fun disconnectService() {
        if (isServiceConnected) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Service was not registered or already unbound: ${e.message}")
            }
            isServiceConnected = false
            woyouService = null
            iWoyouService = null
            Log.d(TAG, "Printer service disconnected by manager.")
        }
    }

    fun isConnected(): Boolean {
        // More robust check: also ensure the AIDL interface object is not null
        return isServiceConnected && iWoyouService != null && woyouService != null
    }

    //
    // PRINTER METHODS
    //

    fun printText(text: String, align: Int) {
        if (!isConnected()) {
            Log.w(TAG, "Not connected, cannot print text.")
            return
        }
        // Assuming printText in AIDL also takes ICallback based on setAlignment
        // If not, remove the callback argument
        val callback = createCallback("printText")
        if (callback == null) {
            Log.e(TAG, "Failed to create callback for printText. Aborting.")
            return
        }
        // First set alignment, then print text
        // Note: AIDL calls are often asynchronous. Ensure alignment is set before printing.
        // This might require a more complex handling if setAlignment itself is async
        // and doesn't block. For now, assuming it's quick enough or synchronous.
        setAlignment(align) // This will now use the corrected call with a callback
        invokeMethod("printText", text, callback) // Assuming printText needs a callback
    }

    /**
     * Prints text with a specified font size and typeface.
     * Assumes alignment is handled separately if needed.
     *
     * @param text The text to print.
     * @param typeface The name of the typeface/font to use (e.g., "default", "fangsong").
     *                 This depends on the printer's supported fonts.
     * @param fontSize The font size.
     * @param align The alignment for the text (0-left, 1-center, 2-right).
     *              This will call setAlignment before printing.
     */
    fun printTextWithFont(text: String, typeface: String, fontSize: Float, align: Int) {
        if (!isConnected()) {
            Log.w(TAG, "Not connected, cannot print text with font.")
            return
        }

        // 1. Create the callback
        val callback = createCallback("printTextWithFont")
        if (callback == null) {
            Log.e(TAG, "Failed to create callback for printTextWithFont. Aborting.")
            return
        }

        // 2. Set alignment (if your printing logic requires setting alignment before each print)
        // This call to setAlignment itself must be correct (i.e., provide its own callback)
        setAlignment(align) // Assuming setAlignment is already fixed to accept its callback

        // 3. Invoke printTextWithFont with ALL FOUR required arguments
        Log.d(TAG, "Attempting to invoke printTextWithFont with: text='$text', typeface='$typeface', fontSize=$fontSize, callback=$callback")
        invokeMethod(
            "printTextWithFont", // Method name
            text,                // Argument 1: String text
            typeface,            // Argument 2: String typeface
            fontSize,            // Argument 3: float font size
            callback             // Argument 4: ICallback callback
        )
    }

    /**
     * Sets the alignment for subsequent printing operations.
     * AIDL definition: void setAlignment(int alignment, in ICallback callback);
     */
    fun setAlignment(align: Int) {
        if (!isConnected()) {
            Log.w(TAG, "Not connected, cannot set alignment.")
            return
        }
        val callback = createCallback("setAlignment")
        if (callback == null) {
            Log.e(TAG, "Failed to create callback for setAlignment. Aborting.")
            return
        }
        // Pass 'align' and the created 'callback' object to invokeMethod
        invokeMethod("setAlignment", align, callback)
    }

    @Suppress("UNUSED")
    fun setFontSize(fontSize: Float) {
        if (!isConnected()) return
        val callback = createCallback("setFontSize") // Assuming it also needs a callback
        if (callback == null) { /* ... handle error ... */ return
        }
        invokeMethod("setFontSize", fontSize, callback)
    }

    @Suppress("UNUSED")
    fun lineWrap(count: Int) { // Assuming lineWrap might take a count, and a callback
        if (!isConnected()) return
        val callback = createCallback("lineWrap")
        if (callback == null) { /* ... handle error ... */ return
        }
        invokeMethod("lineWrap", count, callback) // Check AIDL for exact signature
    }

    fun printQRCode(text: String, size: Int, errorLevel: Int) {
        if (!isConnected()) return
        val callback = createCallback("printQRCode")
        if (callback == null) { /* ... handle error ... */ return
        }
        // Assuming the primary version of printQRCode also takes a callback
        invokeMethod("printQRCode", text, size, errorLevel, callback)
        // Note: The reflection for printQRCode trying different signatures in the original code
        // might need adjustment if all versions actually require a callback.
        // For simplicity, this example assumes the 4-arg version is primary.
    }

    fun getPrinterStatus(): Int {
        if (!isConnected()) {
            Log.w(TAG, "Not connected, cannot get printer status.")
            return -1
        }

        val result = invokeMethod("getPrinterStatus")

        return when (result) {
            is Int -> {
                Log.d(TAG, "Printer status: $result")
                result
            }
            null -> {
                Log.e(TAG, "invokeMethod for getPrinterStatus returned null.")
                -1
            }
            else -> {
                Log.e(TAG, "invokeMethod for getPrinterStatus returned unexpected type: ${result.javaClass.name}")
                -1
            }
        }
    }

    /**
     * Helper method to invoke methods using reflection.
     * It now correctly handles parameters including ICallback proxies.
     */
    private fun invokeMethod(methodName: String, vararg args: Any?): Any? {
        val service = woyouService // Use the stored woyouService
        if (service == null) {
            Log.e(TAG, "Service is null, cannot invoke method $methodName.")
            return null
        }

        try {
            Log.d(
                TAG,
                "Invoking method: $methodName with args: ${args.joinToString { it?.javaClass?.simpleName ?: "null" }} (count: ${args.size})"
            )

            val parameterTypes = args.map { arg ->
                when {
                    arg is String -> String::class.java
                    arg is Int -> Int::class.javaPrimitiveType
                    arg is Float -> Float::class.javaPrimitiveType
                    arg is Boolean -> Boolean::class.javaPrimitiveType
                    // Check if the argument is a Proxy and implements ICallback
                    arg != null && Proxy.isProxyClass(arg.javaClass) && arg.javaClass.interfaces.any { it.name == "woyou.aidlservice.jiuiv5.ICallback" } -> {
                        Class.forName("woyou.aidlservice.jiuiv5.ICallback")
                    }

                    arg == null -> Any::class.java // Or handle nulls more specifically if needed
                    else -> arg.javaClass
                }
            }.toTypedArray()

            Log.d(
                TAG,
                "Parameter types for $methodName: ${parameterTypes.joinToString { it?.name ?: "" }}"
            )

            val method = service.javaClass.getMethod(methodName, *parameterTypes)
            val result = method.invoke(service, *args)

            Log.d(TAG, "✅ Method $methodName invoked successfully. Result: $result")
            return result
        } catch (e: NoSuchMethodException) {
            Log.e(
                TAG,
                "❌ NoSuchMethodException for $methodName. Check method signature and arguments in AIDL vs. invocation.",
                e
            )
            // Log expected vs actual for easier debugging
            val argClasses = args.map { it?.javaClass?.name ?: "null" }.toTypedArray()
            Log.e(TAG, "Arguments provided: ${argClasses.contentToString()}")
            logAvailableMethods(service, methodName)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error invoking method $methodName: ${e.message}", e)
        }
        return null
    }

    /**
     * Helper to log available methods of the service object for debugging NoSuchMethodException.
     */
    private fun logAvailableMethods(service: Any, methodNameFilter: String? = null) {
        Log.d(TAG, "Available methods in service ${service.javaClass.name}:")
        service.javaClass.methods.filter {
            methodNameFilter == null || it.name.contains(methodNameFilter, ignoreCase = true)
        }.sortedBy { it.name }.forEach { method ->
            val params = method.parameterTypes.joinToString(", ") { it.simpleName }
            Log.d(TAG, "  -> ${method.name}($params)")
        }
    }


    /**
     * Helper method to create a dynamic proxy for the ICallback interface.
     */
    private fun createCallback(methodName: String): Any? {
        return try {
            val callbackClass = Class.forName("woyou.aidlservice.jiuiv5.ICallback")

            // Using InvocationHandler for clarity
            Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { proxy, method, args ->
                Log.d(
                    TAG,
                    "ICallback: ${method.name} invoked for original call: $methodName (Args: ${args?.contentToString()})"
                )
                when (method.name) {
                    "onRunResult" -> {
                        val success = args?.getOrNull(0) as? Boolean
                        Log.d(TAG, "ICallback onRunResult for $methodName: Success - $success")
                        // TODO: Propagate this result if needed
                    }

                    "onReturnString" -> {
                        val resultStr = args?.getOrNull(0) as? String
                        Log.d(TAG, "ICallback onReturnString for $methodName: $resultStr")
                        // TODO: Propagate this result if needed
                    }

                    "onRaiseException" -> {
                        val code = args?.getOrNull(0) as? Int
                        val msg = args?.getOrNull(1) as? String
                        Log.e(
                            TAG,
                            "ICallback onRaiseException for $methodName: code=$code, msg=$msg"
                        )
                        // TODO: Propagate this error if needed
                    }

                    "onPrinterStatus" -> { // Assuming this callback method exists in ICallback.aidl
                        val status = args?.getOrNull(0) as? Int
                        Log.d(TAG, "ICallback onPrinterStatus for $methodName: $status")
                        // TODO: Propagate this status if needed
                    }
                    // Handle other ICallback methods as defined in your ICallback.aidl
                }
                // Most AIDL callback methods are 'oneway' and return 'void'.
                // If your callback methods return something, adjust this return.
                null
            }
        } catch (e: ClassNotFoundException) {
            Log.e(
                TAG,
                "❌ ICallback class (woyou.aidlservice.jiuiv5.ICallback) not found: ${e.message}",
                e
            )
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create ICallback proxy for $methodName: ${e.message}", e)
            null
        }
    }

    /**
     * Get connection info for debugging
     */
    @Suppress("UNUSED")
    fun getConnectionInfo(): String {
        return buildString {
            appendLine("=== PRINTER CONNECTION INFO ===")
            appendLine("Is Service Connected (flag): $isServiceConnected")
            appendLine("IWoyouService (typed AIDL obj): ${if (iWoyouService != null) "Available" else "Null"}")
            appendLine("WoyouService (reflection target obj): ${if (woyouService != null) "Available" else "Null"}")
            appendLine("Current Service Config: ${currentServiceConfig?.packageName ?: "N/A"}")
            appendLine("Connection Attempts: $connectionAttempts/$maxConnectionAttempts")
            appendLine("Android Version: ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("===============================")
        }
    }
}

