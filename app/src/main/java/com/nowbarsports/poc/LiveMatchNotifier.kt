package com.nowbarsports.poc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object LiveMatchNotifier {
    const val CHANNEL_ID = "live_matches"
    const val NOTIFICATION_ID = 1001
    const val ACTION_UPDATE = "com.nowbarsports.poc.UPDATE"
    const val ACTION_END = "com.nowbarsports.poc.END"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live matches",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ongoing sports and esports score updates"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun post(context: Context, snapshot: MatchSnapshot) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val updateIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, MatchActionReceiver::class.java).setAction(ACTION_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, MatchActionReceiver::class.java).setAction(ACTION_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_score)
            .setContentTitle(snapshot.title)
            .setContentText(snapshot.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snapshot.details))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(snapshot.chip.take(7))
            .addAction(R.drawable.ic_score, "模拟更新", updateIntent)
            .addAction(R.drawable.ic_score, "结束", endIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        MatchStore.save(context, snapshot)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        MatchStore.clear(context)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun canPostPromoted(context: Context): Boolean {
        return NotificationManagerCompat.from(context).canPostPromotedNotifications()
    }
}
