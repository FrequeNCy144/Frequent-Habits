package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  updateThemeColors(darkTheme)
  val colorScheme = if (darkTheme) {
    darkColorScheme(
      primary = PrimaryViolet,
      onPrimary = TextPrimary,
      secondary = SecondaryViolet,
      onSecondary = TextPrimary,
      background = AppBg,
      onBackground = TextPrimary,
      surface = AppCard,
      onSurface = TextPrimary,
      surfaceVariant = AppCard,
      onSurfaceVariant = TextSecondary,
      outline = AppBorder,
      error = ErrorRed
    )
  } else {
    lightColorScheme(
      primary = PrimaryViolet,
      onPrimary = Color.White,
      secondary = SecondaryViolet,
      onSecondary = Color.White,
      background = Color(0xFFF9F9FB),
      onBackground = Color(0xFF111115),
      surface = Color.White,
      onSurface = Color(0xFF111115),
      surfaceVariant = Color(0xFFF1F1F5),
      onSurfaceVariant = Color(0xFF6B6B76),
      outline = Color(0xFFE5E5ED),
      error = ErrorRed
    )
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
