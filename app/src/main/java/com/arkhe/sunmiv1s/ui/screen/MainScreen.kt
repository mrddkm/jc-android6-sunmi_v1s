package com.arkhe.sunmiv1s.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arkhe.sunmiv1s.ui.component.QRCodeDialog

@Composable
fun MainScreen(
    isServiceConnected: Boolean,
    connectionStatusMessage: String, // Added this parameter
    onPrintReceipt: () -> Unit,
    onScanQR: () -> Unit,
    onPrintQR: () -> Unit, // This is for showing the custom QR dialog
    onCheckStatus: () -> Unit,
    onCheckServices: () -> Unit,
    lastScanResult: String,
    showQRDialog: Boolean,
    onShowQRDialog: (Boolean) -> Unit,
    onPrintCustomQR: (String) -> Unit // This is for printing the custom QR data
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onScanQR()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to scan QR codes.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Section - Pass the new message
        HeaderSection(
            isServiceConnected = isServiceConnected,
            statusMessage = connectionStatusMessage // Pass the detailed status message
        )

        // Action Buttons Section
        ActionButtonsSection(
            isServiceConnected = isServiceConnected,
            hasCameraPermission = hasCameraPermission,
            onPrintReceipt = onPrintReceipt,
            onPrintQR = onPrintQR, // This should trigger the dialog: onShowQRDialog(true)
            onScanQR = onScanQR,
            onCheckStatus = onCheckStatus,
            onCheckServices = onCheckServices,
            onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
        )

        // Last Scan Result Section
        if (lastScanResult.isNotBlank()) {
            LastScanResultSection(scanResult = lastScanResult)
        }

        // Info Section
        InfoSection()
    }

    // QR Code Dialog for custom QR input
    if (showQRDialog) { // Ensure dialog is shown based on the state
        QRCodeDialog(
            showDialog = true, // Controlled by state
            onDismiss = { onShowQRDialog(false) },
            onPrintQR = { qrData ->
                onPrintCustomQR(qrData) // Call the specific lambda for printing custom QR
                onShowQRDialog(false) // Dismiss dialog after queuing print
            }
        )
    }
}

@Composable
private fun HeaderSection(
    isServiceConnected: Boolean,
    statusMessage: String // New parameter for detailed status
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Sunmi V1s Demo", // App Title
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Display the detailed connection status message
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = if (isServiceConnected && statusMessage.contains("✓")) {
                    MaterialTheme.colorScheme.primary // Greenish for success
                } else if (statusMessage.contains("✗") || statusMessage.contains("failed")) {
                    MaterialTheme.colorScheme.error // Reddish for error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant // Default for intermediate states
                }
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    isServiceConnected: Boolean,
    hasCameraPermission: Boolean,
    onPrintReceipt: () -> Unit,
    onPrintQR: () -> Unit,       // This lambda should trigger the dialog for custom QR
    onScanQR: () -> Unit,
    onCheckStatus: () -> Unit,
    onCheckServices: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    // Print Receipt Button
    Button(
        onClick = onPrintReceipt,
        enabled = isServiceConnected, // Enable only if service is truly connected
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), // Slightly adjusted height
        shape = RoundedCornerShape(8.dp) // Consistent corner rounding
    ) {
        Text(
            text = "🖨️ Print Sample Receipt",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Button to show the QR Code input dialog
    Button(
        onClick = onPrintQR, // This will call onShowQRDialog(true) in MainActivity
        enabled = isServiceConnected, // Enable only if service is connected
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = "📝 Print Custom QR Code",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // QR Scanner Button
    Button(
        onClick = {
            if (hasCameraPermission) {
                onScanQR()
            } else {
                onRequestCameraPermission()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Text(
            text = "📱 Scan QR Code",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Check Printer Status Button
    Button(
        onClick = onCheckStatus,
        enabled = isServiceConnected, // Enable only if service is connected
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface, // Different color for distinction
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = "🔍 Check Printer Status",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Check Sunmi Services Button
    Button(
        onClick = onCheckServices, // This button can always be enabled
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceDim, // Yet another color
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = "🛠️ Check Device Services",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LastScanResultSection(scanResult: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📱 Last Scanned QR Result:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = scanResult,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) // More subtle background
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp) // Adjusted spacing
        ) {
            Text(
                text = "Function Information:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Print Sample Receipt: Prints a pre-defined sales receipt.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Print Custom QR: Opens a dialog to enter text & print it as a QR code.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Scan QR Code: Uses the camera to scan QR codes.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Check Printer Status: Verifies the current state of the printer.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Check Device Services: Lists available Sunmi services on the device.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
