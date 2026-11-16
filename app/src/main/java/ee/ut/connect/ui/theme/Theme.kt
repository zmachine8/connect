package ee.ut.connect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import ee.ut.connect.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

enum class ConnectThemeStyle(val label: String) {
    DARK_FANTASY("Dark fantasy"), LIGHT("Light"), MIDNIGHT("Midnight"),
}

val Context.connectPreferences by preferencesDataStore(name = "connect-settings")
val ConnectThemePreferenceKey = stringPreferencesKey("theme_style")

val LocalConnectThemeStyle = staticCompositionLocalOf { ConnectThemeStyle.DARK_FANTASY }

private val DarkFantasyColors = darkColorScheme(
    primary = Color(0xFFE9AF68), onPrimary = Color(0xFF251509),
    primaryContainer = Color(0xFF62341F), onPrimaryContainer = Color(0xFFFFEAD3),
    secondary = Color(0xFFC18B57), onSecondary = Color(0xFF251509),
    background = Color(0xFF111110), onBackground = Color(0xFFF2E8DA),
    surface = Color(0xFF1D1C1A), onSurface = Color(0xFFF2E8DA),
    surfaceVariant = Color(0xFF2C2B28), onSurfaceVariant = Color(0xFFCEC0AD),
    outline = Color(0xFF97724C), error = Color(0xFFFFB4AB),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF704214), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB6), onPrimaryContainer = Color(0xFF271800),
    secondary = Color(0xFF715B42), onSecondary = Color.White,
    background = Color(0xFFFFF9F2), onBackground = Color(0xFF211A13),
    surface = Color(0xFFFFF9F2), onSurface = Color(0xFF211A13),
    surfaceVariant = Color(0xFFF0E3D3), onSurfaceVariant = Color(0xFF514638),
    outline = Color(0xFF89765F), error = Color(0xFFBA1A1A),
)
private val MidnightColors = darkColorScheme(
    primary = Color(0xFFB8C7FF), onPrimary = Color(0xFF17264D),
    primaryContainer = Color(0xFF304272), onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFBDC6E0), onSecondary = Color(0xFF273047),
    background = Color(0xFF0E1420), onBackground = Color(0xFFE1E6F3),
    surface = Color(0xFF171F2D), onSurface = Color(0xFFE1E6F3),
    surfaceVariant = Color(0xFF283244), onSurfaceVariant = Color(0xFFC2CADB),
    outline = Color(0xFF8290AA), error = Color(0xFFFFB4AB),
)
val ConnectDisplayFont = FontFamily(Font(R.font.cinzel_semibold, FontWeight.SemiBold))
private val ConnectBodyFont = FontFamily(Font(R.font.lato_regular))
private val baseTypography = Typography()
private val ConnectTypography = baseTypography.copy(
    displaySmall = baseTypography.displaySmall.copy(fontFamily = ConnectDisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, letterSpacing = 2.sp),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = ConnectDisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 30.sp),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = ConnectDisplayFont, fontWeight = FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = ConnectBodyFont, fontSize = 18.sp),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = ConnectBodyFont),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = ConnectBodyFont),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = ConnectBodyFont),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = ConnectBodyFont),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = ConnectBodyFont),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = ConnectBodyFont),
)
@Composable
fun ConnectTheme(style: ConnectThemeStyle = ConnectThemeStyle.DARK_FANTASY, content: @Composable () -> Unit) {
    val colors = when (style) {
        ConnectThemeStyle.DARK_FANTASY -> DarkFantasyColors
        ConnectThemeStyle.LIGHT -> LightColors
        ConnectThemeStyle.MIDNIGHT -> MidnightColors
    }
    CompositionLocalProvider(LocalConnectThemeStyle provides style) {
    MaterialTheme(colorScheme = colors, typography = ConnectTypography,
        shapes = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(12.dp)), content = {
            Surface(color = colors.background, content = content)
        })
    }
}
