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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4285F4),       // Google Blue
    secondary = Color(0xFF9B72CB),     // Indigo/Lavender Gradient Accent
    tertiary = Color(0xFFA8C7FA),      // Light Blue highlight
    background = Color(0xFF111318),    // Deep dark slate/black
    surface = Color(0xFF1E1F20),       // Material card surface
    surfaceVariant = Color(0xFF282A2F),// Input box grey background
    onPrimary = Color(0xFF001D35),     // Dark text for light blue bubble
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E2E6),  // Main text crisp white
    onSurface = Color(0xFFE2E2E6),
    outline = Color(0xFF444746),       // Border grey
    outlineVariant = Color(0xFF303134)
)

private val LightColorScheme = DarkColorScheme // Standardize on Elegant Dark globally as requested

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false, // Force Elegant Dark styling
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
