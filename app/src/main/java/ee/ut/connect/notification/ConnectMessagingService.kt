package ee.ut.connect.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ee.ut.connect.MainActivity
import ee.ut.connect.R
import ee.ut.connect.data.repository.PushTokenRepository

class ConnectMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { PushTokenRepository.save(it, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "New Connect message"
        val body = message.notification?.body ?: message.data["body"] ?: "Open Connect to read it."
        createMessageChannel(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_connect)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ may have notifications disabled by the user.
        }
    }

    companion object {
        const val CHANNEL_ID = "connect_messages"
        fun createMessageChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "New direct and group messages" })
            }
        }
    }
}
