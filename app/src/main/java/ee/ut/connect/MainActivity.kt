package ee.ut.connect

import android.os.Bundle
import android.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import ee.ut.connect.ui.theme.ConnectThemePreferenceKey
import ee.ut.connect.ui.theme.ConnectThemeStyle
import ee.ut.connect.ui.theme.connectPreferences
import ee.ut.connect.ui.ConnectApp
import ee.ut.connect.ui.theme.ConnectTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember

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
        setContent {
            val appContext = LocalContext.current.applicationContext
            val preferencesFlow = remember(appContext) {
                appContext.connectPreferences.data
                    .catch { emit(emptyPreferences()) }
            }
            val preferences by preferencesFlow.collectAsState(initial = emptyPreferences())
            val selectedTheme = ConnectThemeStyle.entries.firstOrNull {
                it.name == preferences[ConnectThemePreferenceKey]
            } ?: ConnectThemeStyle.DARK_FANTASY
            SideEffect {
                val lightBars = selectedTheme == ConnectThemeStyle.LIGHT
                @Suppress("DEPRECATION")
                window.statusBarColor = if (lightBars) android.graphics.Color.rgb(255, 249, 242) else Color.rgb(17, 17, 16)
                @Suppress("DEPRECATION")
                window.navigationBarColor = if (lightBars) android.graphics.Color.rgb(255, 249, 242) else Color.rgb(17, 17, 16)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            val scope = rememberCoroutineScope()
            ConnectTheme(selectedTheme) {
                ConnectApp(onThemeChange = { style ->
                    scope.launch {
                        appContext.connectPreferences.edit { it[ConnectThemePreferenceKey] = style.name }
                    }
                })
            }
        }
    }
}
