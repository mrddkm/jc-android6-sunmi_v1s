package com.arkhe.sunmiv1s.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QRCodeDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPrintQR: (String) -> Unit
) {
    if (showDialog) {
        var qrText by remember { mutableStateOf("") }
        var selectedTemplate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Print QR Code",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose a template or enter custom text:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Template buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedTemplate = "WiFi Template"
                                    qrText = "WIFI:T:WPA;S:MyWiFi;P:password123;;"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("WiFi", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    selectedTemplate = "URL Template"
                                    qrText = "https://www.google.com"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("URL", fontSize = 12.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedTemplate = "Contact Template"
                                    qrText = "BEGIN:VCARD\nVERSION:3.0\nFN:John Doe\nTEL:+628123456789\nEMAIL:john@example.com\nEND:VCARD"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Contact", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    selectedTemplate = "SMS Template"
                                    qrText = "SMSTO:+628123456789:Hello from QR Code!"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("SMS", fontSize = 12.sp)
                            }
                        }
                    }

                    if (selectedTemplate.isNotEmpty()) {
                        Text(
                            text = "Selected: $selectedTemplate",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom text input
                    OutlinedTextField(
                        value = qrText,
                        onValueChange = {
                            qrText = it
                            selectedTemplate = if (it.isEmpty()) "" else "Custom"
                        },
                        label = { Text("QR Code Text") },
                        placeholder = { Text("Enter text for QR code...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (qrText.isNotBlank()) {
                            onPrintQR(qrText)
                        }
                    },
                    enabled = qrText.isNotBlank()
                ) {
                    Text("Print QR")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}