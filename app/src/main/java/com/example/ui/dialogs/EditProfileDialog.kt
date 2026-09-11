package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tr
import com.example.ui.components.AppTextField
import com.example.ui.theme.AppCard
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditProfileDialog(
    userName: String,
    profileImageUri: String,
    language: String,
    onSaveName: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempUserName by remember(userName) { mutableStateOf(userName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppCard,
        title = {
            Text(
                text = tr(language, "Profil bearbeiten", "პროფილის რედაქტირება", "编辑个人主页", "Edit Profile"),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = AppCard,
                        border = BorderStroke(2.dp, PrimaryViolet)
                    ) {
                        if (profileImageUri.isNotEmpty()) {
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
                                    contentDescription = "Avatar Placeholder",
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onChangePhoto,
                        border = BorderStroke(1.dp, PrimaryViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change picture",
                            tint = PrimaryViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(language, "Foto ändern", "ფოტოს შეცვლა", "更换照片", "Change photo"),
                            color = PrimaryViolet
                        )
                    }
                }

                AppTextField(
                    value = tempUserName,
                    onValueChange = { tempUserName = it },
                    labelText = tr(language, "Name", "სახელი", "名称", "Name"),
                    placeholderText = tr(language, "Dein Name...", "შენი სახელი...", "你的名字...", "Your name..."),
                    singleLine = true,
                    containerColor = AppCard,
                    testTag = "edit_profile_name_input"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveName(tempUserName)
                    onDismiss()
                }
            ) {
                Text(tr(language, "Speichern", "შენახვა", "保存", "Save"), color = PrimaryViolet, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(language, "Abbrechen", "გაუქმება", "取消", "Cancel"), color = TextSecondary)
            }
        }
    )
}
