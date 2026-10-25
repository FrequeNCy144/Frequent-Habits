package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun EmergencyRepairScreen(
    errorMessage: String,
    stackTrace: String,
    onRepairAndRestart: () -> Unit,
    onResetPrefs: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    var isRepairedMessage by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFF0F0E17)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1F1D2E),
                modifier = Modifier.size(80.dp),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRepairedMessage) Icons.Default.CheckCircle else Icons.Default.Build,
                        contentDescription = null,
                        tint = if (isRepairedMessage) SuccessGreen else PrimaryViolet,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (isRepairedMessage) "App erfolgreich repariert!" else "App-Selbstreparatur aktiv",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Frequent Habits hat ein Startproblem sicher abgefangen. Deine Gewohnheiten und Verlaufsdaten sind geschützt.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA5A3BC),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // Primary action: Repair & Restart
            Button(
                onClick = {
                    isRepairedMessage = true
                    onRepairAndRestart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "App jetzt reparieren & starten",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            // Secondary action: Reset Corrupted Prefs & Clean
            OutlinedButton(
                onClick = onResetPrefs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDDD6FE))
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFA78BFA))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Einstellungen reparieren (Daten behalten)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            // Diagnostic details toggle
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(
                    text = if (showDetails) "Diagnosedetails ausblenden ▲" else "Diagnosedetails anzeigen ▼",
                    color = Color(0xFF8C8A9E),
                    fontSize = 13.sp
                )
            }

            AnimatedVisibility(visible = showDetails) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171622)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Fehlermeldung:",
                            color = Color(0xFFFFAA00),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = errorMessage.ifBlank { "Keine Fehlermeldung" },
                            color = Color(0xFFE2E0F0),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (stackTrace.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stackTrace.take(800),
                                color = Color(0xFF88869E),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
