package ee.ut.connect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ConnectColors = lightColorScheme(
    primary = Color(0xFF315DA8),
    secondary = Color(0xFF536D93),
    tertiary = Color(0xFF75546F),
)

@Composable
fun ConnectTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ConnectColors, content = content)
}
