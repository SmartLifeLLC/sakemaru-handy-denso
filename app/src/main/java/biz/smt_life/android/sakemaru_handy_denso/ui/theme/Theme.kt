package biz.smt_life.android.sakemaru_handy_denso.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = LightBlue80,
    background = Color(0xFF0D1B2A),
    surface = Color(0xFF1B263B),
    onPrimary = Color(0xFF0D1B2A),
    onSecondary = Color(0xFF0D1B2A),
    onTertiary = Color(0xFF0D1B2A),
    onBackground = Color(0xFFE0E1DD),
    onSurface = Color(0xFFE0E1DD),
    surfaceVariant = Color(0xFF1B3A5F),
    onSurfaceVariant = Color(0xFFB8C9DC),
    primaryContainer = Color(0xFF1B3A5F),
    onPrimaryContainer = Color(0xFFB3D4FF),
    tertiaryContainer = Color(0xFF0D47A1),
    onTertiaryContainer = Color(0xFFE3F2FD)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueGrey40,
    tertiary = LightBlue40,
    background = Color(0xFFF5F9FF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A237E),
    onSurface = Color(0xFF1A237E),
    surfaceVariant = BlueSurface,
    onSurfaceVariant = Color(0xFF37474F),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    tertiaryContainer = Color(0xFFE1F5FE),
    onTertiaryContainer = Color(0xFF01579B),
    error = Color(0xFFB00020),
    onError = Color.White
)

// Square shapes - no rounded corners
private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun SakemaruhandydensoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled dynamic color to use blue theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
