package ee.ut.connect

import android.os.Bundle
import android.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ee.ut.connect.ui.ConnectApp
import ee.ut.connect.ui.theme.ConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.rgb(17, 17, 16)
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.rgb(17, 17, 16)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { ConnectTheme { ConnectApp() } }
    }
}
