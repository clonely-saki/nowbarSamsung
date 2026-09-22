package com.nowbarsports.poc

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val matchDataSource: MatchDataSource = MatchDataSources.current
    private var refreshScreen: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LiveMatchNotifier.ensureChannel(this)
        setContent {
            var refreshKey by remember { mutableIntStateOf(0) }
            val current = remember(refreshKey) {
                MatchStore.load(this@MainActivity, matchDataSource)
            }

            DisposableEffect(Unit) {
                refreshScreen = { refreshKey++ }
                onDispose { refreshScreen = null }
            }

            SportsNowBarApp(
                activity = this@MainActivity,
                dataSource = matchDataSource,
                current = current,
                onShowInNowBar = { event ->
                    if (!LiveMatchNotifier.hasNotificationPermission(this@MainActivity)) {
                        requestNotificationsIfNeeded()
                    } else {
                        val snapshot = if (current?.eventId == event.id) {
                            current
                        } else {
                            matchDataSource.initial(event.id)
                        }
                        LiveMatchNotifier.post(this@MainActivity, snapshot)
                        refreshKey++
                    }
                },
                onStop = {
                    LiveMatchNotifier.cancel(this@MainActivity)
                    refreshKey++
                },
                onUpdate = {
                    current?.let {
                        LiveMatchNotifier.post(this@MainActivity, matchDataSource.next(it))
                    }
                    refreshKey++
                },
                onOpenPromotionSettings = ::openPromotionSettings
            )
        }
        requestNotificationsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshScreen?.invoke()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    private fun openPromotionSettings() {
        if (Build.VERSION.SDK_INT < 36) return
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }
}

@Composable
private fun SportsNowBarApp(
    activity: Activity,
    dataSource: MatchDataSource,
    current: MatchSnapshot?,
    onShowInNowBar: (MockEvent) -> Unit,
    onStop: () -> Unit,
    onUpdate: () -> Unit,
    onOpenPromotionSettings: () -> Unit
) {
    val displayEvents = remember(current?.eventId, current?.step, dataSource) {
        dataSource.events().map { event ->
            if (event.id == current?.eventId) current else dataSource.initial(event.id)
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = if (BuildConfig.SAMSUNG_DIAGNOSTIC) {
                                "Samsung Now Bar Diagnostic — NOT FOR DISTRIBUTION"
                            } else {
                                "体育 Now Bar"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "足球 · 篮球 · 英雄联盟 · 一级方程式",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                item { DiagnosticCard(activity, onOpenPromotionSettings) }
                eventSectionCard("进行中", displayEvents.filter { it.section == EventSection.LIVE }, dataSource, current, onShowInNowBar, onStop, onUpdate)
                eventSectionCard("即将开始", displayEvents.filter { it.section == EventSection.UPCOMING }, dataSource, current, onShowInNowBar, onStop, onUpdate)
                eventSectionCard("已结束", displayEvents.filter { it.section == EventSection.FINISHED }, dataSource, current, onShowInNowBar, onStop, onUpdate)

                item {
                    Text(
                        text = "仅使用模拟数据。开始赛事，检查 Now Bar 紧凑状态，更新后再停止。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(activity: Activity, onOpenPromotionSettings: () -> Unit) {
    val notificationAllowed = LiveMatchNotifier.hasNotificationPermission(activity)
    val promotedAllowed = Build.VERSION.SDK_INT >= 36 && LiveMatchNotifier.canPostPromoted(activity)
    val metadataPresent = activity.packageManager.getApplicationInfo(
        activity.packageName,
        PackageManager.GET_META_DATA
    ).metaData?.getBoolean(LiveMatchNotifier.SAMSUNG_METADATA_KEY, false) == true

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("平台状态", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("包名：${activity.packageName}", style = MaterialTheme.typography.bodySmall)
            Text("Android API：${Build.VERSION.SDK_INT}", style = MaterialTheme.typography.bodySmall)
            Text("通知权限：${if (notificationAllowed) "已允许" else "未允许"}", style = MaterialTheme.typography.bodySmall)
            Text("Promoted 通知：${if (promotedAllowed) "已允许" else "未允许/不可用"}", style = MaterialTheme.typography.bodySmall)
            Text("Samsung 元数据：${if (metadataPresent) "已存在" else "缺失"}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenPromotionSettings) { Text("打开 Live Updates 设置") }
        }
    }
}

private fun LazyListScope.eventSectionCard(
    title: String,
    snapshots: List<MatchSnapshot>,
    dataSource: MatchDataSource,
    current: MatchSnapshot?,
    onShowInNowBar: (MockEvent) -> Unit,
    onStop: () -> Unit,
    onUpdate: () -> Unit
) {
    item {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    items(snapshots, key = { it.eventId }) { snapshot ->
        EventCard(
            snapshot = snapshot,
            isShownInNowBar = snapshot.eventId == current?.eventId,
            onShowInNowBar = { dataSource.findEvent(snapshot.eventId)?.let(onShowInNowBar) },
            onStop = onStop,
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun EventCard(
    snapshot: MatchSnapshot,
    isShownInNowBar: Boolean,
    onShowInNowBar: () -> Unit,
    onStop: () -> Unit,
    onUpdate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = snapshot.section == EventSection.LIVE,
                    onClick = {},
                    label = { Text(snapshot.kind.displayName()) },
                    enabled = false
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = snapshot.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            BrandingRow(snapshot.branding)
            Spacer(Modifier.height(12.dp))
            Text("Now Bar 紧凑预览", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(snapshot.compactPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!snapshot.compactSecondary.isNullOrBlank()) {
                Text(snapshot.compactSecondaryLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (snapshot.kind == MatchKind.LOL && snapshot.lolPhase == LolPhase.BETWEEN_GAMES) {
                Text("两局之间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(10.dp))
            Text(snapshot.expandedDetails, style = MaterialTheme.typography.bodyMedium)

            if (snapshot.section == EventSection.LIVE || isShownInNowBar) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isShownInNowBar) {
                        OutlinedButton(onClick = onStop) { Text("停止") }
                        if (snapshot.section == EventSection.LIVE) {
                            Button(onClick = onUpdate) { Text("更新") }
                        }
                    } else {
                        Button(onClick = onShowInNowBar) { Text("显示在 Now Bar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandingRow(branding: EventBranding) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrandBadge(branding.competition, compact = true)
        Spacer(Modifier.weight(1f))
        BrandBadge(branding.primary)
        if (branding.secondary != null) {
            Text("对阵", color = MaterialTheme.colorScheme.onSurfaceVariant)
            BrandBadge(branding.secondary)
        }
    }
}

@Composable
private fun BrandBadge(identity: BrandIdentity, compact: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (identity.logoResId != null) {
                Icon(
                    painter = painterResource(identity.logoResId),
                    contentDescription = identity.name,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = identity.initials,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(identity.shortName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private fun MatchKind.displayName(): String = when (this) {
    MatchKind.FOOTBALL -> "足球"
    MatchKind.BASKETBALL -> "篮球"
    MatchKind.LOL -> "英雄联盟"
    MatchKind.FORMULA1 -> "一级方程式"
}
