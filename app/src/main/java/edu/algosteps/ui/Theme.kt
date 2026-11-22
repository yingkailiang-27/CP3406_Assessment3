package edu.algosteps.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1D5E51), onPrimary = Color.White,
    primaryContainer = Color(0xFFC9EDDA), onPrimaryContainer = Color(0xFF0B382D),
    secondary = Color(0xFF556427), secondaryContainer = Color(0xFFE3EDB7), onSecondaryContainer = Color(0xFF28350F),
    tertiary = Color(0xFF884527), tertiaryContainer = Color(0xFFFFDAC5), onTertiaryContainer = Color(0xFF502211),
    surface = Color(0xFFF9F9F3), background = Color(0xFFF9F9F3), surfaceVariant = Color(0xFFE4E9DE),
    onSurface = Color(0xFF19251F), onBackground = Color(0xFF19251F), onSurfaceVariant = Color(0xFF425047)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA5DBC3), onPrimary = Color(0xFF08372C),
    primaryContainer = Color(0xFF21513E), onPrimaryContainer = Color(0xFFC7EFD9),
    secondary = Color(0xFFC7D99B), secondaryContainer = Color(0xFF394A22), onSecondaryContainer = Color(0xFFE1ECB8),
    tertiary = Color(0xFFFFB892), tertiaryContainer = Color(0xFF693820), onTertiaryContainer = Color(0xFFFFD9C4),
    surface = Color(0xFF131C18), background = Color(0xFF131C18), onSurface = Color(0xFFE2E9DE),
    onBackground = Color(0xFFE2E9DE)
)

@Composable
fun AlgoStepsTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
