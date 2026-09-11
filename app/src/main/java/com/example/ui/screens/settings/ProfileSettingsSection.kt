package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tr
import com.example.ui.HabitsViewModel
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

fun LazyListScope.profileSettingsSection(
    language: String,
    userName: String,
    profileImageUri: String?,
    viewModel: HabitsViewModel,
    photoPickerLauncher: ActivityResultLauncher<String>,
    onNameChange: (String) -> Unit
) {
                    // SUBPAGE 1: PROFIL & KONTO
                    // Card 1: Profilbild
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCard),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Profilbild", "Profile Picture"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = CircleShape,
                                        color = AppCard,
                                        border = BorderStroke(2.dp, PrimaryViolet)
                                    ) {
                                        if (!profileImageUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = profileImageUri,
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(PrimaryViolet.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = PrimaryViolet,
                                                    modifier = Modifier.size(50.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { photoPickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr(language, "Foto ändern", "Change Photo"))
                                    }

                                    if (!profileImageUri.isNullOrEmpty()) {
                                        OutlinedButton(
                                            onClick = { viewModel.updateProfileImageUri("") },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, ErrorRed)
                                        ) {
                                            Text(tr(language, "Entfernen", "Remove"), color = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: Nutzername & Konto
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCard),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = tr(language, "Nutzername & Konto", "Username & Account"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                AppTextField(
                                    value = userName,
                                    onValueChange = onNameChange,
                                    placeholderText = tr(language, "Gib deinen Namen ein...", "Enter your name..."),
                                    containerColor = ProgressTrack,
                                    singleLine = true,
                                    testTag = "settings_username_input"
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = tr(
                                        language,
                                        "Dein Name und Profilbild werden nur lokal auf deinem Gerät gespeichert.",
                                        "Your name and profile picture are stored strictly locally on your device."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
}