package com.arkhe.sunmiv1s.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
    onPrintReceipt: () -> Unit,
    onScanQR: () -> Unit,
    onPrintQR: () -> Unit,
    onCheckStatus: () -> Unit,
    onCheckServices: () -> Unit,
    lastScanResult: String,
    showQRDialog: Boolean,
    onShowQRDialog: (Boolean) -> Unit,
    onPrintCustomQR: (String) -> Unit
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
                "Camera permission required for QR scanning",
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
        // Header Section
        HeaderSection(isServiceConnected = isServiceConnected)

        // Action Buttons Section
        ActionButtonsSection(
            isServiceConnected = isServiceConnected,
            hasCameraPermission = hasCameraPermission,
            onPrintReceipt = onPrintReceipt,
            onPrintQR = onPrintQR,
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

    // QR Code Dialog
    QRCodeDialog(
        showDialog = showQRDialog,
        onDismiss = { onShowQRDialog(false) },
        onPrintQR = { qrData ->
            onPrintCustomQR(qrData)
            onShowQRDialog(false)
        }
    )
}

@Composable
private fun HeaderSection(isServiceConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sunmi V1s App",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isServiceConnected) "Printer: Connected ✓" else "Printer: Disconnected ✗",
                fontSize = 14.sp,
                color = if (isServiceConnected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    isServiceConnected: Boolean,
    hasCameraPermission: Boolean,
    onPrintReceipt: () -> Unit,
    onPrintQR: () -> Unit,
    onScanQR: () -> Unit,
    onCheckStatus: () -> Unit,
    onCheckServices: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    // Print Receipt Button
    Button(
        onClick = onPrintReceipt,
        enabled = isServiceConnected,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "🖨️ Print Receipt",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Print QR Code Button
    Button(
        onClick = onPrintQR,
        enabled = isServiceConnected,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Text(
            text = "📋 Print QR Code",
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
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
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
        enabled = isServiceConnected,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.outline
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
        onClick = onCheckServices,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = "🛠️ Check Services",
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📱 Last Scan Result:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scanResult,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Information:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Print Receipt - Print sample sales receipt",
                fontSize = 12.sp
            )
            Text(
                text = "• Print QR Code - Print QR code with templates",
                fontSize = 12.sp
            )
            Text(
                text = "• Scan QR Code - Scan QR code with camera",
                fontSize = 12.sp
            )
            Text(
                text = "• Check Status - Check printer status real-time",
                fontSize = 12.sp
            )
            Text(
                text = "• Check Services - Check Sunmi service availability",
                fontSize = 12.sp
            )
        }
    }
}