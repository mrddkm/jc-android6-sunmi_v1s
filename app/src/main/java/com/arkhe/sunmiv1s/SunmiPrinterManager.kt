package com.arkhe.sunmiv1s

// import java.lang.reflect.InvocationHandler // No longer needed directly for lambda
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import woyou.aidlservice.jiuiv5.ICallback
import woyou.aidlservice.jiuiv5.IWoyouService
import java.util.LinkedList
import java.util.Queue

/**
 * Enhanced Sunmi V1s Printer Manager with Multiple Connection Strategies
 * Handles Android 11+ package visibility and multiple fallback approaches.
 * Uses direct AIDL interface calls instead of reflection.
 */
class SunmiPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "SunmiV1sPrinterManager"
        private const val PRIMARY_SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
        private const val PRIMARY_SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
        private val ALTERNATIVE_SERVICES = listOf(
            ServiceConfig("com.sunmi.printerservice", "com.sunmi.printerservice.IWoyouService"),
            ServiceConfig(
                "woyou.aidlservice.jiuiv5.main",
                "woyou.aidlservice.jiuiv5.IWoyouService"
            ),
            ServiceConfig("com.sunmi.sunmiservice", "woyou.aidlservice.jiuiv5.IWoyouService")
        )
    }

    data class ServiceConfig(val packageName: String, val action: String)

    private var iWoyouService: IWoyouService? = null
    private var isServiceBound = false
    private var connectionListener: ((Boolean) -> Unit)? = null
    private var currentServiceConfig: ServiceConfig? = null
    private var connectionAttempts = 0
    private val maxConnectionAttempts = 1 + ALTERNATIVE_SERVICES.size

    private val pendingCommands: Queue<Pair<String, () -> Unit>> = LinkedList()
    private var isConnecting = false

    // ServiceConnection remains largely the same as it implements multiple methods.
    // Kotlin doesn't offer a direct lambda conversion for multi-method interfaces
    // in the same way it does for SAM interfaces.
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "🔌 Service connected: ${name?.packageName}/${name?.className}")
            isConnecting = false
            try {
                iWoyouService = IWoyouService.Stub.asInterface(service)
                if (iWoyouService != null) {
                    isServiceBound = true
                    Log.i(
                        TAG,
                        "✅ IWoyouService interface obtained successfully for ${name?.packageName}."
                    )
                    processPendingCommands()
                    connectionListener?.invoke(true)
                } else {
                    Log.e(
                        TAG,
                        "❌ IWoyouService is null after asInterface for ${name?.packageName}. This might indicate an AIDL stub issue or service problem."
                    )
                    handleFailedConnectionAttempt()
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "❌ Exception during onServiceConnected for ${name?.packageName}: ${e.message}",
                    e
                )
                handleFailedConnectionAttempt()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "🔌 Service disconnected: ${name?.packageName}/${name?.className}")
            isConnecting = false
            isServiceBound = false
            iWoyouService = null
            connectionListener?.invoke(false)
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.e(
                TAG,
                "🆘 Binding DIED for service: ${name?.packageName}/${name?.className}. Attempting to reconnect."
            )
            isConnecting = false
            isServiceBound = false
            iWoyouService = null
            connectionListener?.invoke(false)
            connectService()
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.e(
                TAG,
                "❌ Null binding received for service: ${name?.packageName}/${name?.className}. Service might not be available or configured correctly."
            )
            isConnecting = false
            handleFailedConnectionAttempt()
        }
    }

    private fun handleFailedConnectionAttempt() {
        isServiceBound = false
        iWoyouService = null
        if (connectionAttempts < maxConnectionAttempts) {
            Log.i(
                TAG,
                "Retrying connection, attempt #${connectionAttempts + 1} of $maxConnectionAttempts"
            )
            connectServiceWithFallback()
        } else {
            Log.e(
                TAG,
                "❌ Max connection attempts ($maxConnectionAttempts) reached. Failed to connect to any Sunmi service."
            )
            isConnecting = false
            connectionListener?.invoke(false)
            clearPendingCommandsWithError("Max connection attempts reached.")
        }
    }

    private fun processPendingCommands() {
        synchronized(pendingCommands) {
            if (pendingCommands.isNotEmpty()) {
                Log.d(TAG, "Processing ${pendingCommands.size} pending command(s).")
                while (pendingCommands.isNotEmpty()) {
                    val commandPair = pendingCommands.poll()
                    commandPair?.let {
                        Log.d(TAG, "Executing queued command: ${it.first}")
                        try {
                            it.second.invoke()
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error executing queued command '${it.first}': ${e.message}",
                                e
                            )
                        }
                    }
                }
            } else {
                Log.d(TAG, "No pending commands to process.")
            }
        }
    }

    private fun addCommandToQueue(commandDescription: String, command: () -> Unit) {
        synchronized(pendingCommands) {
            Log.d(TAG, "Queuing command: $commandDescription")
            pendingCommands.add(Pair(commandDescription, command))
        }
    }

    private fun clearPendingCommandsWithError(reason: String) {
        synchronized(pendingCommands) {
            if (pendingCommands.isNotEmpty()) {
                Log.w(TAG, "Clearing ${pendingCommands.size} pending command(s) due to: $reason")
                pendingCommands.clear()
            }
        }
    }

    fun setConnectionListener(listener: (Boolean) -> Unit) {
        this.connectionListener = listener
    }

    fun connectService() {
        if (isConnected()) {
            Log.d(TAG, "Service already connected and IWoyouService is available.")
            connectionListener?.invoke(true)
            processPendingCommands()
            return
        }
        if (isConnecting) {
            Log.d(TAG, "Connection attempt already in progress.")
            return
        }
        isConnecting = true
        isServiceBound = false
        iWoyouService = null
        connectionAttempts = 0
        Log.i(TAG, "Attempting to connect to Sunmi printer service...")
        connectServiceWithFallback()
    }

    private fun connectServiceWithFallback() {
        if (connectionAttempts >= maxConnectionAttempts) {
            Log.e(
                TAG,
                "❌ Max connection attempts reached. Failed to connect after $maxConnectionAttempts attempts."
            )
            isConnecting = false
            connectionListener?.invoke(false)
            clearPendingCommandsWithError("Max connection attempts reached during fallback.")
            return
        }

        val servicesToTry = listOf(
            ServiceConfig(
                PRIMARY_SERVICE_PACKAGE,
                PRIMARY_SERVICE_ACTION
            )
        ) + ALTERNATIVE_SERVICES
        currentServiceConfig = servicesToTry.getOrNull(connectionAttempts)
        connectionAttempts++

        if (currentServiceConfig == null) {
            Log.e(
                TAG,
                "❌ No more service configurations to try. (Attempt $connectionAttempts/$maxConnectionAttempts)"
            )
            isConnecting = false
            connectionListener?.invoke(false)
            clearPendingCommandsWithError("No more service configurations.")
            return
        }

        Log.i(
            TAG,
            "Attempt #${connectionAttempts}/${maxConnectionAttempts}: Trying to bind to service ${currentServiceConfig!!.packageName} (Action: ${currentServiceConfig!!.action})"
        )
        val intent = Intent().apply {
            setPackage(currentServiceConfig!!.packageName)
            action = currentServiceConfig!!.action
        }

        try {
            val isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (isBound) {
                Log.d(TAG, "Binding initiated for ${currentServiceConfig!!.packageName}...")
            } else {
                Log.w(
                    TAG,
                    "bindService returned false immediately for ${currentServiceConfig!!.packageName}. Trying next..."
                )
                handleFailedConnectionAttempt()
            }
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "SecurityException for ${currentServiceConfig!!.packageName}: ${e.message}. Check <queries> tag.",
                e
            )
            handleFailedConnectionAttempt()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Exception during bindService for ${currentServiceConfig!!.packageName}: ${e.message}",
                e
            )
            handleFailedConnectionAttempt()
        }
    }

    fun disconnectService() {
        if (isServiceBound) {
            try {
                context.unbindService(serviceConnection)
                Log.d(TAG, "Printer service unbound by manager.")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Service was not registered or already unbound: ${e.message}")
            }
        }
        isServiceBound = false
        iWoyouService = null
        isConnecting = false
    }

    fun isConnected(): Boolean {
        return isServiceBound && iWoyouService != null
    }

    private fun executePrintCommand(
        commandDescription: String,
        // Optional: Add a parameter here for a Kotlin lambda to be called by ICallback.Stub
        // e.g., onResult: (Boolean, String?) -> Unit,
        commandAction: (service: IWoyouService, callback: ICallback) -> Unit
    ) {
        if (!isConnected()) {
            Log.w(TAG, "$commandDescription: Service not connected. Queuing command.")
            addCommandToQueue(commandDescription) {
                executePrintCommand(
                    commandDescription,
                    commandAction
                )
            }
            if (!isConnecting && !isServiceBound) connectService()
            return
        }

        val service = iWoyouService
        if (service == null) {
            Log.e(TAG, "$commandDescription: IWoyouService is null. Queuing.")
            addCommandToQueue(commandDescription) {
                executePrintCommand(
                    commandDescription,
                    commandAction
                )
            }
            if (!isConnecting) connectService()
            return
        }

        // Set the description for the current command on the shared callback instance
        // This is a simple way; more robust would be per-command callbacks or request IDs.
        printerCommandCallback.setCommandDescription(commandDescription) // Hacky way for dynamic dispatch if needed, or better, make printerCommandCallback a class
        // Better: make printerCommandCallback an interface and implement it
        // Or, directly pass commandDescription to the ICallback.Stub if it's designed to accept it.
        // For now, if printerCommandCallback is the object as above, this cast won't work directly.
        // Let's modify printerCommandCallback for this.

        // Simpler approach for now:
        // If your ICallback.Stub is simple and just logs, you might not even need to set a description per call,
        // as the log in executePrintCommand already states what's being executed.
        // The callback logs would then be generic.

        // If you need specific actions per callback, you'd pass lambdas to executePrintCommand
        // and your ICallback.Stub would invoke those.

        try {
            Log.d(TAG, "Executing: $commandDescription")
            commandAction(service, printerCommandCallback) // Pass the concrete Stub instance
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException during $commandDescription: ${e.message}", e)
            // Consider the state of the service. A RemoteException (like DeadObjectException)
            // often means the service connection is lost.
            disconnectService() // Good practice
            addCommandToQueue(commandDescription) {
                executePrintCommand(
                    commandDescription,
                    commandAction
                )
            }
            connectService() // Attempt to reconnect
        } catch (e: Exception) {
            Log.e(TAG, "General Exception during $commandDescription: ${e.message}", e)
            // Depending on the exception, you might not need to disconnect/reconnect here.
        }
    }

    fun printText(text: String, align: Int) {
        val commandDesc = "printText (align: $align, text: '$text')"
        executePrintCommand(commandDesc) { service, mainCallback ->
            // Pass the same mainCallback to all service methods requiring it
            service.setAlignment(align, mainCallback) // Use the mainCallback
            Log.d(TAG, "Alignment command sent for $commandDesc")
            service.printText(text, mainCallback)     // Use the mainCallback
            Log.d(TAG, "printText command sent for $commandDesc")
        }
    }

    fun printTextWithFont(text: String, typeface: String, fontSize: Float, align: Int) {
        val commandDesc =
            "printTextWithFont (align: $align, font: $typeface, size: $fontSize, text: '$text')"
        executePrintCommand(commandDesc) { service, mainCallback ->
            service.setAlignment(align, mainCallback)
            Log.d(TAG, "Alignment command sent for $commandDesc")
            service.printTextWithFont(text, typeface, fontSize, mainCallback)
            Log.d(TAG, "printTextWithFont command sent for $commandDesc")
        }
    }

    fun setAlignment(align: Int) {
        val commandDesc = "setAlignment (align: $align)"
        executePrintCommand(commandDesc) { service, mainCallback ->
            service.setAlignment(align, mainCallback)
            Log.d(TAG, "setAlignment command sent for $commandDesc")
        }
    }

    fun setFontSize(fontSize: Float) {
        val commandDesc = "setFontSize (size: $fontSize)"
        executePrintCommand(commandDesc) { service, mainCallback ->
            service.setFontSize(fontSize, mainCallback)
            Log.d(TAG, "setFontSize command sent for $commandDesc")
        }
    }

    fun lineWrap(count: Int) {
        val commandDesc = "lineWrap (count: $count)"
        executePrintCommand(commandDesc) { service, mainCallback ->
            service.lineWrap(count, mainCallback)
            Log.d(TAG, "lineWrap command sent for $commandDesc")
        }
    }

    fun printQRCode(text: String, size: Int, errorLevel: Int) {
        val commandDesc = "printQRCode (size: $size, error: $errorLevel, text: '$text')"
        executePrintCommand(commandDesc) { service, mainCallback ->
            service.printQRCode(text, size, errorLevel, mainCallback)
            Log.d(TAG, "printQRCode command sent for $commandDesc")
        }
    }


    fun getPrinterStatus(statusResultCallback: (status: Int) -> Unit) {
        val commandDescription = "getPrinterStatus"
        if (!isConnected()) {
            Log.w(TAG, "$commandDescription: Service not connected. Queuing.")
            addCommandToQueue(commandDescription) { getPrinterStatus(statusResultCallback) }
            if (!isConnecting && !isServiceBound) connectService()
            return
        }
        val service = iWoyouService
        if (service == null) {
            Log.e(TAG, "$commandDescription: IWoyouService is null. Queuing.")
            addCommandToQueue(commandDescription) { getPrinterStatus(statusResultCallback) }
            if (!isConnecting) connectService()
            return
        }

        try {
            Log.d(TAG, "Executing: $commandDescription")
            val status = service.printerStatus
            Log.d(TAG, "Printer status reported: $status")
            statusResultCallback(status)
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException during $commandDescription: ${e.message}", e)
            statusResultCallback(-1001)
            disconnectService()
            addCommandToQueue(commandDescription) { getPrinterStatus(statusResultCallback) }
            connectService()
        } catch (e: Exception) {
            Log.e(TAG, "General Exception during $commandDescription: ${e.message}", e)
            statusResultCallback(-1002)
        }
    }

    private val printerCommandCallback = object : ICallback.Stub() {
        // We'll need a way to correlate responses to commands if using a single callback instance
        // For simplicity now, just logging. You might need a more sophisticated system
        // to route callbacks to the original caller (e.g., using a map of request IDs).
        // Or, for each command, pass a specific Kotlin lambda that this stub calls.

        private var currentCommandDescription: String = "N/A"

        fun setCommandDescription(description: String) {
            this.currentCommandDescription = description
        }

        override fun onRunResult(isSuccess: Boolean) {
            Log.i(
                TAG,
                "ICallback.onRunResult for [$currentCommandDescription]: Success - $isSuccess"
            )
            // Here you would typically invoke a Kotlin lambda passed with the command
        }

        override fun onReturnString(result: String?) {
            Log.i(
                TAG,
                "ICallback.onReturnString for [$currentCommandDescription]: Result - '${result ?: ""}'"
            )
        }

        override fun onRaiseException(code: Int, msg: String?) {
            Log.e(
                TAG,
                "ICallback.onRaiseException for [$currentCommandDescription]: Code - $code, Message - '${msg ?: "Unknown error"}'"
            )
        }

        override fun onProgressUpdate(progress: Int) {
            Log.d(
                TAG,
                "ICallback.onProgressUpdate for [$currentCommandDescription]: Progress - $progress"
            )
        }

        override fun isCallbackReady(): Boolean {
            val isReady = iWoyouService != null && isServiceBound
            Log.d(
                TAG,
                "ICallback.isCallbackReady for [$currentCommandDescription], returning: $isReady"
            )
            // Generally, if the service is bound and the interface is not null, it's ready.
            return isReady
        }
    }

    fun getConnectionInfo(): String {
        return buildString {
            appendLine("=== PRINTER CONNECTION INFO ===")
            appendLine("Is Service Bound (isServiceBound flag): $isServiceBound")
            appendLine("IWoyouService (typed AIDL object): ${if (iWoyouService != null) "Available" else "Null"}")
            appendLine("Is Connecting (isConnecting flag): $isConnecting")
            appendLine("Current Service Config Tried: ${currentServiceConfig?.packageName ?: "N/A"}")
            appendLine("Connection Attempts Made: $connectionAttempts / $maxConnectionAttempts")
            appendLine("Pending Commands in Queue: ${pendingCommands.size}")
            appendLine("Android OS Version: API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Product: ${Build.PRODUCT})")
            appendLine("===============================")
        }
    }
}
