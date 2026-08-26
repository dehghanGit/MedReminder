package me.sandbad.medireminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Palette ─────────────────────────────────────────────────────────────────
// Calm clinical teal — reads as "health" without the alarm-red of a hospital UI.
val BrandPrimary      = Color(0xFF0D9488)
val BrandPrimaryLight = Color(0xFFCCFBF1)
val BrandSecondary    = Color(0xFF115E59)

val TakenGreen        = Color(0xFF22C55E)
val TakenGreenLight   = Color(0xFFDCFCE7)
val SkippedAmber      = Color(0xFFF59E0B)
val SkippedAmberLight = Color(0xFFFEF3C7)
val MissedRose        = Color(0xFFF43F5E)
val MissedRoseLight   = Color(0xFFFFE4E6)
val PendingSlate      = Color(0xFF64748B)
val PendingSlateLight = Color(0xFFF1F5F9)

val RefillOrange      = Color(0xFFF97316)
val RefillOrangeLight = Color(0xFFFFEDD5)

val SurfaceWhite      = Color(0xFFFFFFFF)
val Background        = Color(0xFFF6F8FA)
val CardSurface       = Color(0xFFFFFFFF)
val DividerColor      = Color(0xFFE5E7EB)

val TextPrimary       = Color(0xFF0F172A)
val TextSecondary     = Color(0xFF64748B)
val TextHint          = Color(0xFF94A3B8)

// Dark counterparts
private val DarkBackground   = Color(0xFF0B1220)
private val DarkSurface      = Color(0xFF152034)
private val DarkTextPrimary  = Color(0xFFE2E8F0)
private val DarkTextSecond   = Color(0xFF94A3B8)

// ─── Color schemes ───────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary            = BrandPrimary,
    onPrimary          = Color.White,
    primaryContainer   = BrandPrimaryLight,
    onPrimaryContainer = BrandSecondary,
    secondary          = PendingSlate,
    onSecondary        = Color.White,
    background         = Background,
    onBackground       = TextPrimary,
    surface            = SurfaceWhite,
    onSurface          = TextPrimary,
    surfaceVariant     = Color(0xFFF1F5F9),
    onSurfaceVariant   = TextSecondary,
    outline            = DividerColor,
    error              = MissedRose,
    onError            = Color.White
)

private val DarkColors = darkColorScheme(
    primary            = Color(0xFF2DD4BF),
    onPrimary          = Color(0xFF042F2E),
    primaryContainer   = Color(0xFF115E59),
    onPrimaryContainer = BrandPrimaryLight,
    secondary          = Color(0xFF94A3B8),
    onSecondary        = Color(0xFF0B1220),
    background         = DarkBackground,
    onBackground       = DarkTextPrimary,
    surface            = DarkSurface,
    onSurface          = DarkTextPrimary,
    surfaceVariant     = Color(0xFF1E293B),
    onSurfaceVariant   = DarkTextSecond,
    outline            = Color(0xFF334155),
    error              = MissedRose,
    onError            = Color.White
)

// ─── Typography ──────────────────────────────────────────────────────────────
private val AppTypography = Typography(
    displaySmall   = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

private val AppShapes = Shapes(
    small  = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large  = RoundedCornerShape(24.dp)
)

@Composable
fun MediReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
