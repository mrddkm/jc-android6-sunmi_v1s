package com.arkhe.sunmiv1s.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log

/**
 * Enhanced Service Checker for Sunmi V1s with better Android 11+ support
 */
object ServiceChecker {
    private const val TAG = "ServiceChecker"

    data class ServiceCheckResult(
        val hasPrimaryService: Boolean,
        val hasAlternativeService: Boolean,
        val availableServices: List<String>,
        val deviceInfo: DeviceInfo,
        val errors: List<String>
    ) {
        val hasAnyService: Boolean get() = hasPrimaryService || hasAlternativeService || availableServices.isNotEmpty()

        fun getReport(): String {
            val sb = StringBuilder()
            sb.appendLine("=== ENHANCED SUNMI SERVICE CHECK REPORT ===")
            sb.appendLine()
            sb.appendLine("Primary Service Available: $hasPrimaryService")
            sb.appendLine("Alternative Service Available: $hasAlternativeService")
            sb.appendLine("Any Service Available: $hasAnyService")
            sb.appendLine("Available Services Count: ${availableServices.size}")
            sb.appendLine()

            if (availableServices.isNotEmpty()) {
                sb.appendLine("✅ Found Services:")
                availableServices.forEach { service ->
                    sb.appendLine("  - $service")
                }
                sb.appendLine()
            }

            sb.appendLine("Details:")
            if (hasPrimaryService) {
                sb.appendLine("✅ Primary Sunmi V1s service found: woyou.aidlservice.jiuiv5")
            } else {
                sb.appendLine("❌ Primary Sunmi V1s service NOT found: woyou.aidlservice.jiuiv5")
            }

            if (hasAlternativeService) {
                sb.appendLine("✅ Alternative Sunmi service found")
            } else {
                sb.appendLine("❌ Alternative Sunmi service NOT found")
            }

            if (errors.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("Errors encountered:")
                errors.forEach { error ->
                    sb.appendLine("❌ $error")
                }
            }

            sb.appendLine()
            sb.appendLine("Device Info:")
            sb.appendLine("- Manufacturer: ${deviceInfo.manufacturer}")
            sb.appendLine("- Brand: ${deviceInfo.brand}")
            sb.appendLine("- Model: ${deviceInfo.model}")
            sb.appendLine("- SDK: ${deviceInfo.sdkVersion}")
            sb.appendLine("- Android Version: ${deviceInfo.androidVersion}")
            sb.appendLine("- Is Sunmi Device: ${deviceInfo.isSunmiDevice}")
            sb.appendLine("- Supports Package Queries: ${deviceInfo.supportsPackageQueries}")
            sb.appendLine("=====================================")

            return sb.toString()
        }
    }

    data class DeviceInfo(
        val manufacturer: String,
        val brand: String,
        val model: String,
        val sdkVersion: Int,
        val androidVersion: String,
        val isSunmiDevice: Boolean,
        val supportsPackageQueries: Boolean
    )

    fun checkSunmiServices(context: Context): ServiceCheckResult {
        Log.d(TAG, "Starting enhanced Sunmi service check...")

        val errors = mutableListOf<String>()
        val availableServices = mutableListOf<String>()
        var hasPrimaryService = false
        var hasAlternativeService = false

        val deviceInfo = DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            sdkVersion = Build.VERSION.SDK_INT,
            androidVersion = Build.VERSION.RELEASE,
            isSunmiDevice = Build.MANUFACTURER.uppercase().contains("SUNMI"),
            supportsPackageQueries = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        )

        Log.d(TAG, "Device Info: $deviceInfo")

        // Method 1: Check by package name (for Android 11+, requires <queries> in manifest)
        try {
            val primaryPackage = "woyou.aidlservice.jiuiv5"
            hasPrimaryService = isPackageInstalled(context, primaryPackage)
            if (hasPrimaryService) {
                availableServices.add("Package: $primaryPackage")
                Log.d(TAG, "✅ Primary package found: $primaryPackage")
            } else {
                Log.d(TAG, "❌ Primary package not found: $primaryPackage")
            }
        } catch (e: Exception) {
            val error = "Package check failed: ${e.message}"
            errors.add(error)
            Log.e(TAG, error, e)
        }

        // Method 2: Query Intent Services with multiple approaches
        val serviceIntents = listOf(
            "woyou.aidlservice.jiuiv5.IWoyouService",
            "woyou.aidlservice.jiuiv5",
            "com.sunmi.printerservice.IWoyouService",
            "com.sunmi.printerservice"
        )

        serviceIntents.forEach { action ->
            try {
                val services = queryServicesByAction(context, action)
                if (services.isNotEmpty()) {
                    availableServices.addAll(services.map { "Action($action): ${it.serviceInfo.packageName}" })
                    if (action.contains("woyou.aidlservice.jiuiv5")) {
                        hasPrimaryService = true
                    } else {
                        hasAlternativeService = true
                    }
                    Log.d(TAG, "✅ Found ${services.size} services for action: $action")
                } else {
                    Log.d(TAG, "❌ No services found for action: $action")
                }
            } catch (e: Exception) {
                val error = "Service query failed for action '$action': ${e.message}"
                errors.add(error)
                Log.e(TAG, error, e)
            }
        }

        // Method 3: Direct package and service combination check
        try {
            val intent = Intent().apply {
                setPackage("woyou.aidlservice.jiuiv5")
                action = "woyou.aidlservice.jiuiv5.IWoyouService"
            }

            val services = queryServices(context, intent)
            if (services.isNotEmpty()) {
                hasPrimaryService = true
                availableServices.add("Direct: woyou.aidlservice.jiuiv5/IWoyouService")
                Log.d(TAG, "✅ Direct service check successful")
            } else {
                Log.d(TAG, "❌ Direct service check failed")
            }
        } catch (e: Exception) {
            val error = "Direct service check failed: ${e.message}"
            errors.add(error)
            Log.e(TAG, error, e)
        }

        // Method 4: Alternative service packages
        val alternativePackages = listOf(
            "com.sunmi.printerservice",
            "woyou.aidlservice.jiuiv5.main",
            "com.sunmi.sunmiservice"
        )

        alternativePackages.forEach { packageName ->
            try {
                if (isPackageInstalled(context, packageName)) {
                    hasAlternativeService = true
                    availableServices.add("Alt Package: $packageName")
                    Log.d(TAG, "✅ Alternative package found: $packageName")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check alternative package $packageName: ${e.message}")
            }
        }

        val result = ServiceCheckResult(
            hasPrimaryService = hasPrimaryService,
            hasAlternativeService = hasAlternativeService,
            availableServices = availableServices.distinct(),
            deviceInfo = deviceInfo,
            errors = errors
        )

        Log.d(TAG, "Service check completed. Found ${availableServices.size} services total")
        return result
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.w(TAG, "Exception checking package $packageName: ${e.message}")
            false
        }
    }

    private fun queryServicesByAction(context: Context, action: String): List<ResolveInfo> {
        return try {
            val intent = Intent(action)
            queryServices(context, intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query services by action $action: ${e.message}")
            emptyList()
        }
    }

    private fun queryServices(context: Context, intent: Intent): List<ResolveInfo> {
        return try {
            val packageManager = context.packageManager
            val flags =
                PackageManager.MATCH_ALL

            val services = packageManager.queryIntentServices(intent, flags)
            services
        } catch (e: Exception) {
            Log.w(TAG, "queryIntentServices failed: ${e.message}")
            emptyList()
        }
    }
}