package dev.o3000y.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF4F46E5)
val IndigoLight = Color(0xFFE8EDFF)
val IndigoDark = Color(0xFF4338CA)

val ChartColors = listOf(
    Color(0xFF4F46E5), Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF6366F1),
)

val ErrorRed = Color(0xFFDC2626)
val ErrorBg = Color(0xFFFEF2F2)
val SuccessGreen = Color(0xFF16A34A)
val SuccessBg = Color(0xFFDCFCE7)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDark,
    surface = Color.White,
    onSurface = Color(0xFF212529),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF6C757D),
    outline = Color(0xFFDEE2E6),
    outlineVariant = Color(0xFFE9ECEF),
    error = ErrorRed,
    errorContainer = ErrorBg,
)

@Composable
fun O3000yTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
