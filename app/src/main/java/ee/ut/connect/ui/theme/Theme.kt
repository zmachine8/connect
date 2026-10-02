package ee.ut.connect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import ee.ut.connect.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ConnectColors = darkColorScheme(
    primary = Color(0xFFE9AF68), onPrimary = Color(0xFF251509),
    primaryContainer = Color(0xFF62341F), onPrimaryContainer = Color(0xFFFFEAD3),
    secondary = Color(0xFFC18B57), onSecondary = Color(0xFF251509),
    background = Color(0xFF111110), onBackground = Color(0xFFF2E8DA),
    surface = Color(0xFF1D1C1A), onSurface = Color(0xFFF2E8DA),
    surfaceVariant = Color(0xFF2C2B28), onSurfaceVariant = Color(0xFFCEC0AD),
    outline = Color(0xFF97724C), error = Color(0xFFFFB4AB),
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
fun ConnectTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ConnectColors, typography = ConnectTypography,
        shapes = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(12.dp)), content = {
            Surface(color = ConnectColors.background, content = content)
        })
}
