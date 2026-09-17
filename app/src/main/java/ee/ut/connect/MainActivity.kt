package ee.ut.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ee.ut.connect.ui.ConnectApp
import ee.ut.connect.ui.theme.ConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ConnectTheme { ConnectApp() } }
    }
}
