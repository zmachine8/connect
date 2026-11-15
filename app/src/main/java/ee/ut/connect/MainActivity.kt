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
import ee.ut.connect.data.repository.PresenceManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import ee.ut.connect.data.repository.PushTokenRepository
import ee.ut.connect.notification.ConnectMessagingService

class MainActivity : ComponentActivity() {
    private lateinit var presenceManager: PresenceManager
    private var registeredUid: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        val previous = registeredUid
        if (previous != null && previous != uid) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { PushTokenRepository.remove(previous, it) }
        }
        registeredUid = uid
        if (uid != null) FirebaseMessaging.getInstance().token.addOnSuccessListener { PushTokenRepository.save(uid, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConnectMessagingService.createMessageChannel(this)
        presenceManager = PresenceManager()
        lifecycle.addObserver(presenceManager)
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

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        super.onStop()
    }

    override fun onDestroy() {
        presenceManager.close()
        super.onDestroy()
    }
}
