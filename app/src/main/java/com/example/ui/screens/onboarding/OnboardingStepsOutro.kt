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
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = tr(language, "Features im Überblick", "ფუნქციების მიმოხილვა", "功能特性一览", "Feature Overview", "Aperçu des fonctionnalités"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, lineHeight = 34.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tr(language, "Einige der beliebtesten Werkzeuge für deinen täglichen Erfolg:", "ყველაზე პოპულარული ინსტრუმენტები შენი წარმატებისთვის:", "助你达成日常目标的实用工具：", "A selection of popular tools for your daily success:", "Une sélection d'outils populaires pour votre réussite au quotidien :"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Asymmetric Feature Mosaic Grid covering all features
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Smart Insights & Reviews/Time Capsule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.Lightbulb,
                    title = tr(language, "Smart Insights", "სმარტ ინსაიტები", "智能洞察分析", "Smart Insights"),
                    subtitle = tr(language, "Trends & Verhaltensmuster", "ტენდენციები და ქცევა", "打卡趋势与行为规律", "Trends & behavior patterns"),
                    color = HabitCyan,
                    modifier = Modifier.weight(1.2f)
                )
                FeatureTile(
                    icon = Icons.Default.AutoStories,
                    title = tr(language, "Monats-Rückblick", "თვიური მიმოხილვა", "月度与年度回顾", "Monthly Review"),
                    subtitle = tr(language, "Stories & Zeitkapsel", "ისტორიები და კაფსულა", "故事卡片与时光胶囊", "Stories & Time Capsule"),
                    color = PrimaryViolet,
                    modifier = Modifier.weight(1.0f)
                )
            }

            // Row 2: Milestones/Rewards & Audio Focus Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.EmojiEvents,
                    title = tr(language, "Meilensteine", "მიღწევები და ჯილდოები", "里程碑与自定义奖励", "Milestone Rewards"),
                    subtitle = tr(language, "Belohnungen bei Streaks", "ჯილდოები სერიებისთვის", "连续打卡解锁自我奖励", "Rewards at streak goals"),
                    color = HabitYellow,
                    modifier = Modifier.weight(1.0f)
                )
                FeatureTile(
                    icon = Icons.Default.MusicNote,
                    title = tr(language, "Fokus & Audio", "ფოკუსი და აუდიო", "专注计时与音效", "Focus & Soundscapes"),
                    subtitle = tr(language, "Pomodoro & 12 Naturklänge", "პომოდორო და 12 ხმა", "番茄钟与12种自然白噪音", "Pomodoro & 12 ambiances"),
                    color = SecondaryViolet,
                    modifier = Modifier.weight(1.3f)
                )
            }

            // Row 3: Heatmaps/Analytics & Homescreen Widgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.BarChart,
                    title = tr(language, "Tiefe Analysen", "დეტალური ანალიტიკა", "深度数据热力图", "Deep Analytics"),
                    subtitle = tr(language, "GitHub Heatmap & Trends", "სითბური რუკა და გრაფიკები", "热力矩阵与完成率曲线", "Activity Heatmaps & Trends"),
                    color = HabitBlue,
                    modifier = Modifier.weight(1.2f)
                )
                FeatureTile(
                    icon = Icons.Default.Widgets,
                    title = tr(language, "Widget Support", "ვიჯეტის მხარდაჭერა", "桌面快捷组件", "Homescreen Widget"),
                    subtitle = tr(language, "1-Tap direkt abhaken", "1 შეხებით მონიშვნა", "桌面一键打卡", "1-Tap Quick checking"),
                    color = HabitOrange,
                    modifier = Modifier.weight(1.0f)
                )
            }

            // Row 4: Mini Notebook & 100% Offline/Auto-Backup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    icon = Icons.Default.EditNote,
                    title = tr(language, "Mini-Notizbuch", "მინი ნოუთბუქი", "随手记事本", "Mini Notebook"),
                    subtitle = tr(language, "Reflexion zu jedem Habit", "შენიშვნა ყველა ჩვევაზე", "每日打卡随笔感悟", "Daily reflections & notes"),
                    color = HabitPink,
                    modifier = Modifier.weight(1.0f)
                )
                FeatureTile(
                    icon = Icons.Default.Security,
                    title = tr(language, "100% Offline", "100% ოფლაინ", "100% 本地离线", "100% Offline"),
                    subtitle = tr(language, "Volle Privatsphäre & Backup", "სრული კონფიდენციალურობა", "无追踪、自动本地备份", "Private & Daily Backups"),
                    color = HabitGreen,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = tr(
                language,
                "...und vieles mehr!",
                "...და კიდევ ბევრი სხვა!",
                "……以及更多精彩功能！",
                "...and much more!",
                "...et bien plus encore !"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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


