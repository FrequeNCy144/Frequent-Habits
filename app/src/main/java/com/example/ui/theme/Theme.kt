package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val HabitsColorScheme = darkColorScheme(
  primary = PrimaryViolet,
  onPrimary = TextPrimary,
  secondary = SecondaryViolet,
  onSecondary = TextPrimary,
  background = DarkBg,
  onBackground = TextPrimary,
  surface = DarkCard,
  onSurface = TextPrimary,
  surfaceVariant = DarkCard,
  onSurfaceVariant = TextSecondary,
  outline = DarkBorder,
  error = ErrorRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme to match the beautiful video UI
  dynamicColor: Boolean = false, // Keep our custom gorgeous brand colors instead of wallpaper colors
  content: @Composable () -> Unit,
) {
  val colorScheme = HabitsColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
