package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tr
import com.example.ui.theme.*

@Composable
fun ProfileTopBar(
    language: String,
    accentColor: Color,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp)
    ) {
        // Top Left: Share Profile Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { onShareClick() }
                .align(Alignment.CenterStart)
                .testTag("profile_share_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Profile",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center Title
        Text(
            text = tr(language, "Profil", "პროფილი", "我的", "Profile"),
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )

        // Top Right: Settings Gear Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { onSettingsClick() }
                .align(Alignment.CenterEnd)
                .testTag("profile_settings_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileAvatarAndProgressCard(
    userName: String,
    profileImageUri: String?,
    accentColor: Color,
    totalUnlockedCount: Int,
    totalPossibleCount: Int,
    language: String,
    onEditNameClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar & Name
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .border(2.dp, accentColor, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(AppCard)
                    .clickable { onEditNameClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!profileImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture Placeholder",
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onEditNameClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (userName.isBlank()) {
                    Text(
                        text = tr(language, "Name festlegen", "სახელის მითითება", "设置昵称", "Set Name"),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Unlocked Count Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AppCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(language, "Erfolge freigeschaltet", "მიღწევები განბლოკილია", "已解锁成就", "Achievements Unlocked"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$totalUnlockedCount / $totalPossibleCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = HabitYellow
                    )
                }
                val progressFraction = if (totalPossibleCount > 0) totalUnlockedCount.toFloat() / totalPossibleCount.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = HabitYellow,
                    trackColor = ProgressTrack
                )
            }
        }
    }
}
