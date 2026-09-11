package com.example.ui.screens.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tr
import com.example.ui.theme.*

@Composable
fun OnboardingStepAndMuchMore(language: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Text(
            text = tr(language, "Und vieles mehr", "და კიდევ ბევრი რამ", "以及更多丰富功能", "And much more"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tr(language, "Entdecke viele weitere durchdachte Funktionen:", "აღმოაჩინეთ კიდევ ბევრი გააზრებულად შემუშავებული ფუნქცია:", "探索更多精心雕琢的实用功能：", "Discover many more thoughtfully crafted features:"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Asymmetric Feature Mosaic Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1 (Asymmetric widths: 1.3f vs 1.0f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.MusicNote,
                    title = tr(language, "Audio Timer", "აუდიო ტაიმერი", "白噪音计时器", "Audio Timer"),
                    subtitle = tr(language, "Fokus & Soundscapes", "ფოკუსი და ხმოვანი პეიზაჟები", "专注与背景白噪音", "Focus & Soundscapes"),
                    color = PrimaryViolet,
                    modifier = Modifier.weight(1.3f)
                )
                FeatureTile(
                    icon = Icons.Default.BarChart,
                    title = tr(language, "Unrivaled Analytics", "შეუდარებელი ანალიტიკა", "深度数据分析", "Unrivaled Analytics"),
                    subtitle = tr(language, "Heatmaps & Trends", "სითბოს რუქები და ტენდენციები", "打卡热力图与趋势", "Heatmaps & Trends"),
                    color = HabitBlue,
                    modifier = Modifier.weight(1.0f)
                )
            }

            // Row 2 (Asymmetric widths: 1.0f vs 1.4f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.EditNote,
                    title = tr(language, "Mini Notebook", "მინი ნოუთბუქი", "迷你随手记", "Mini Notebook"),
                    subtitle = tr(language, "Notizen zu Habits", "შენიშვნები და ანარეკლები", "习惯感悟与笔记", "Notes & Reflections"),
                    color = HabitOrange,
                    modifier = Modifier.weight(1.0f)
                )
                FeatureTile(
                    icon = Icons.Default.Share,
                    title = tr(language, "Sharing Options", "გაზიარების პარამეტრები", "丰富分享功能", "Sharing Options"),
                    subtitle = tr(language, "Karten & Export", "ბარათები და ექსპორტი", "卡片生成与导出", "Cards & Export"),
                    color = HabitGreen,
                    modifier = Modifier.weight(1.4f)
                )
            }

            // Row 3 (Asymmetric widths: 1.2f vs 1.0f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.Widgets,
                    title = tr(language, "Homescreen Widget", "საწყისი ეკრანის ვიჯეტი", "桌面小组件", "Homescreen Widget"),
                    subtitle = tr(language, "Schnelles Abhaken", "სწრაფი შემოწმება", "桌面快捷打卡", "Quick Checking"),
                    color = SecondaryViolet,
                    modifier = Modifier.weight(1.2f)
                )
                FeatureTile(
                    icon = Icons.Default.EmojiEvents,
                    title = tr(language, "Gamification", "გემიფიკაცია", "趣味成长体系", "Gamification"),
                    subtitle = tr(language, "Trophäen & Level", "ტროფები და სამკერდე ნიშნები", "奖杯与成就勋章", "Trophies & Badges"),
                    color = HabitYellow,
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }
}

@Composable
private fun FeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 8. FINAL LAUNCH PAGE (Manifest & Versprechen)
@Composable
fun OnboardingStepFinalLaunch(
    language: String,
    onComplete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = tr(language, "Deine Gewohnheiten.\nDeine Daten.\nDeine Zukunft.", "შენი ჩვევები.\nშენი მონაცემები.\nშენი მომავალი.", "你的习惯。\n你的数据。\n你的未来。", "Your habits.\nYour data.\nYour future."),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, lineHeight = 46.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ManifestoBulletPoint(
                    icon = Icons.Default.CheckCircle,
                    color = SuccessGreen,
                    title = tr(language, "Kein Tracking, keine Werbung", "არანაირი თვალთვალი, არანაირი რეკლამა", "无追踪，零广告", "No tracking, no advertisements"),
                    description = tr(language, "Absolut werbefrei und ohne versteckte Analysedienste.", "სრულიად ურეკლამო, ფარული თვალთვალის სერვისების გარეშე.", "完全无广告，且无任何隐藏追踪服务。", "Completely ad-free without hidden tracking services.")
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ManifestoBulletPoint(
                    icon = Icons.Default.Security,
                    color = PrimaryViolet,
                    title = tr(language, "Lokaler Speicherort", "მკაცრად ადგილობრივი საცავი", "严格本地离线存储", "Strictly local storage"),
                    description = tr(language, "Alle Notizen und Daten verbleiben sicher auf deinem Gerät.", "ყველა ჩანაწერი და მეტრიკა მკაცრად რჩება თქვენს ტელეფონში.", "所有打卡数据与指标均严格保存在你的手机本地。", "All entries and metrics remain strictly on your phone.")
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ManifestoBulletPoint(
                    icon = Icons.Default.VerifiedUser,
                    color = HabitBlue,
                    title = tr(language, "Keine Registrierung erforderlich", "რეგისტრაცია არ არის საჭირო", "无需注册任何账号", "No registration needed"),
                    description = tr(language, "Sofort loslegen – ganz ohne Konto oder Passwort.", "დაიწყეთ დაუყოვნებლივ - არ არის საჭირო ანგარიში ან შესვლა.", "即开即用——无需账户或繁琐密码。", "Start immediately — no account or login required.")
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "created with ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "❤️",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = " by ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "FrequNCy",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ManifestoBulletPoint(
    icon: ImageVector,
    color: Color,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}


