package com.example.ui.screens.onboarding

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LocalDarkMode
import com.example.LocalHapticsEnabled
import com.frequent.habits.R
import com.example.tr
import com.example.ui.HabitIconMapping
import com.example.ui.HabitsViewModel
import com.example.ui.LanguageDropdown
import com.example.ui.components.AppSegmentedButtonWithIcons
import com.example.ui.components.AppTextField
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.theme.*

@Composable
fun OnboardingStepWelcome(language: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Frequent Habits Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = tr(language, "Willkommen", "მოგესალმებით", "欢迎", "Welcome"),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(language, "bei Frequent Habits", "Frequent Habits-ში", "来到 Frequent Habits", "to Frequent Habits"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Frequent Habits, Permanent Results",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// 2. PERSONALISIERUNG (Language, Theme, Notifications & Audio/Auto Backup)
@Composable
fun OnboardingStepPersonalize(language: String, viewModel: HabitsViewModel) {
    val accentColorName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val accentColor = remember(accentColorName) { HabitIconMapping.getColor(accentColorName) }
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val backupFolderUri by viewModel.backupFolderUri.collectAsStateWithLifecycle()
    val isBackupConfigured = !backupFolderUri.isNullOrEmpty()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.setReviewNotificationsEnabled(true)
            viewModel.setInsightNotificationsEnabled(true)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.saveBackupFolderUri(uri.toString())
        }
    }

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = tr(language, "App personalisieren", "პერსონალიზაცია", "个性化应用", "Personalize App", "Personnaliser l'application"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, lineHeight = 34.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(
                language,
                "Passe Design, Sprache und Backups an.",
                "მოარგეთ დიზაინი, ენა და ასლები თქვენს გემოვნებას.",
                "自定义设计、语言与备份。",
                "Customize look, language, and backups.",
                "Personnalisez l'apparence, la langue et les sauvegardes."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Row 1: Language & Theme side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1.1f)) {
                Text(
                    text = tr(language, "SPRACHE", "ენა", "语言", "LANGUAGE", "LANGUE"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LanguageDropdown(
                    currentLanguage = language,
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(0.9f)) {
                Text(
                    text = tr(language, "THEME", "თემა", "主题", "THEME", "THÈME"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                AppSegmentedButtonWithIcons(
                    options = listOf(
                        (tr(language, "Dunkel", "Dark")) to Icons.Default.DarkMode,
                        (tr(language, "Hell", "Light")) to Icons.Default.LightMode
                    ),
                    selectedIndex = if (darkModeEnabled) 0 else 1,
                    onOptionSelected = { index -> viewModel.setDarkModeEnabled(index == 0) },
                    accentColor = accentColor,
                    testTagPrefix = "customizer_theme_toggle"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Row 2: Accent Color
        Text(
            text = tr(language, "AKZENTFARBE", "აქცენტი ფერი", "强调配色", "ACCENT COLOR", "COULEUR D'ACCENT"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ColorPaletteSelector(
            selectedColorKey = accentColorName,
            onColorSelected = { viewModel.setAccentColorName(it) },
            language = language
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Row 3: Notifications Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accentColor.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(language, "Tägliche Erinnerungen", "შეხსენებები", "每日提醒", "Daily Reminders", "Rappels quotidiens"),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasNotificationPermission) {
                            tr(language, "Aktiviert ✓", "ჩართულია ✓", "已开启 ✓", "Enabled ✓", "Activé ✓")
                        } else {
                            tr(language, "Push-Nachrichten & Insights", "შეტყობინებები", "推送提醒与洞察", "Push alerts & insights", "Alertes & aperçus")
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (hasNotificationPermission) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!hasNotificationPermission) {
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setReviewNotificationsEnabled(true)
                                viewModel.setInsightNotificationsEnabled(true)
                                hasNotificationPermission = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = tr(language, "Aktivieren", "ჩართვა", "开启", "Enable", "Activer"),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr(language, "Aktiv", "აქტიური", "已开启", "Active", "Actif"),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 4: Audio- & Auto-Backup Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accentColor.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(language, "Audio- & Daten-Backup", "აუდიო და სარეზერვო ასლი", "音频与数据备份", "Audio & Data Backup", "Sauvegarde audio & données"),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBackupConfigured) {
                            tr(language, "Tägliches Backup aktiv ✓", "ყოველდღიური ასლი აქტიურია ✓", "每日自动备份已启用 ✓", "Daily backup active ✓", "Sauvegarde quotidienne active ✓")
                        } else {
                            tr(language, "Gewohnheiten & Sounds sichern", "ჩვევების და ხმების დაცვა", "自动保存习惯与音频", "Auto-save habits & sounds", "Sauvegarde des habitudes & sons")
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isBackupConfigured) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!isBackupConfigured) {
                    Button(
                        onClick = { folderLauncher.launch(null) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Ordner wählen", "არჩევა", "选择文件夹", "Select", "Choisir"),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { folderLauncher.launch(null) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr(language, "Aktiv", "აქტიური", "已开启", "Active", "Actif"),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}



// 3. TRANSITION PAGE (Typografisches Statement)
