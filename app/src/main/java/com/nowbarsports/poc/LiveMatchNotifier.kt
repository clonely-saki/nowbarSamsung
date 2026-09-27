package com.nowbarsports.poc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object LiveMatchNotifier {
    const val CHANNEL_ID = "live_matches"
    const val NOTIFICATION_ID = 1001
    const val ACTION_UPDATE = "com.nowbarsports.poc.UPDATE"
    const val ACTION_END = "com.nowbarsports.poc.END"
    const val SAMSUNG_METADATA_KEY = "com.samsung.android.support.ongoing_activity"

    private const val SAMSUNG_STYLE_KEY = "android.ongoingActivityNoti.style"
    private const val SAMSUNG_PRIMARY_INFO_KEY = "android.ongoingActivityNoti.primaryInfo"
    private const val SAMSUNG_SECONDARY_INFO_KEY = "android.ongoingActivityNoti.secondaryInfo"
    private const val SAMSUNG_NOWBAR_PRIMARY_INFO_KEY = "android.ongoingActivityNoti.nowbarPrimaryInfo"
    private const val SAMSUNG_NOWBAR_SECONDARY_INFO_KEY = "android.ongoingActivityNoti.nowbarSecondaryInfo"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "实时赛事",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "体育与电竞实时比分更新"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun post(context: Context, snapshot: MatchSnapshot) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val compactSecondary = snapshot.compactSecondaryLine

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

        val samsungExpandedSecondary = buildString {
            append(snapshot.expandedSummary)
            if (snapshot.expandedDetails.isNotBlank()) {
                append('\n')
                append(snapshot.expandedDetails)
            }
        }

        val samsungExtras = Bundle().apply {
            // Samsung's reverse-engineered standard ongoing-activity style marker.
            putInt(SAMSUNG_STYLE_KEY, 1)
            // Samsung uses these fields for the expanded ongoing-activity content.
            putCharSequence(SAMSUNG_PRIMARY_INFO_KEY, snapshot.expandedTitle)
            putCharSequence(SAMSUNG_SECONDARY_INFO_KEY, samsungExpandedSecondary)
            // The Now Bar compact surface has its own fields and must stay concise.
            putCharSequence(SAMSUNG_NOWBAR_PRIMARY_INFO_KEY, snapshot.compactPrimary)
            putCharSequence(SAMSUNG_NOWBAR_SECONDARY_INFO_KEY, compactSecondary)
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_score)
            // The standard Android Live Update path always carries the rich expanded content.
            .setContentTitle(snapshot.expandedTitle)
            .setContentText(snapshot.expandedSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snapshot.expandedDetails))
            .setContentIntent(openApp)
            .setSubText(snapshot.title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(snapshot.chip.take(7))

        // Controlled A/B mode: keep the Samsung package identity and manifest metadata,
        // but let the official Android Live Update renderer receive no Samsung-private extras.
        if (!BuildConfig.SAMSUNG_STANDARD_ONLY) {
            notificationBuilder.setExtras(samsungExtras)
        }

        val notification = notificationBuilder
            .addAction(R.drawable.ic_score, "刷新比分", updateIntent)
            .addAction(R.drawable.ic_score, "停止推送", endIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The user can revoke notification permission between the explicit check and notify().
            return
        }
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
