package cz.hcasc.hotel.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.hcasc.hotel.MainActivity
import cz.hcasc.hotel.R

/**
 * Local notifications without FCM.
 *
 * Requirements mapping:
 * - C.8: No Google services; WorkManager polling triggers these notifications.
 * - Open app on the appropriate tab when tapped.
 */
object Notifier {

    private const val CHANNEL_ID = "hotel_polling"
    private const val CHANNEL_NAME = "Hotel upozornění"

    const val EXTRA_START_TAB = "start_tab"

    enum class StartTab(val value: String) {
        FRONTDESK("frontdesk"),
        MAINTENANCE("maintenance")
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Upozornění na nové nálezy a závady (polling)."
        }

        nm.createNotificationChannel(channel)
    }

    fun showNewFind(context: Context, reportId: String? = null) {
        show(
            context = context,
            title = "Nový nález",
            text = "Recepce: byl nahlášen nový nález.",
            tab = StartTab.FRONTDESK,
            reportId = reportId
        )
    }

    // Backwards-compatible names used by workers
    fun notifyNewFind(context: Context) = showNewFind(context, null)
    fun notifyNewIssue(context: Context) = showNewIssue(context, null)

    fun showNewIssue(context: Context, reportId: String? = null) {
        show(
            context = context,
            title = "Nová závada",
            text = "Údržba: byla nahlášena nová závada.",
            tab = StartTab.MAINTENANCE,
            reportId = reportId
        )
    }

    private fun show(
        context: Context,
        title: String,
        text: String,
        tab: StartTab,
        reportId: String?
    ) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_TAB, tab.value)
            if (reportId != null) {
                putExtra("report_id", reportId)
            }
        }

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            tab.value.hashCode(),
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Android 13+ runtime permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // Respect user choice; do not crash.
                return
            }
        }

        NotificationManagerCompat.from(context).notify(
            (System.currentTimeMillis() and 0xFFFFFFF).toInt(),
            notification
        )
    }
}
