package com.arkhe.sunmiv1s.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun QRCodeDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPrintQR: (String) -> Unit
) {
    if (showDialog) {
        var qrText by remember { mutableStateOf("") }
        var selectedTemplate by remember { mutableIntStateOf(0) }

        val templates = listOf(
            "Custom Text",
            "Website URL",
            "WiFi Info",
            "Contact Info",
            "Product Info"
        )

        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Print QR Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Template Selector
                    Text(
                        text = "Select Template:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Radio buttons for templates
                    templates.forEachIndexed { index, template ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTemplate == index,
                                onClick = {
                                    selectedTemplate = index
                                    qrText = when (index) {
                                        0 -> ""
                                        1 -> "https://www.example.com"
                                        2 -> "WIFI:T:WPA;S:MyNetwork;P:password123;;"
                                        3 -> "CARD:N:John Doe;TEL:+628123456789;EMAIL:john@example.com;;"
                                        4 -> "Product: Sunmi V1s\nPrice: Rp 5,000,000\nSKU: SNM-V1S-001"
                                        else -> ""
                                    }
                                }
                            )
                            Text(
                                text = template,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Input
                    OutlinedTextField(
                        value = qrText,
                        onValueChange = { qrText = it },
                        label = { Text("QR Code Text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (qrText.isNotBlank()) {
                                    onPrintQR(qrText)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = qrText.isNotBlank()
                        ) {
                            Text("Print")
                        }
                    }
                }
            }
        }
    }
}