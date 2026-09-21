package com.nowbarsports.poc

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var currentText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LiveMatchNotifier.ensureChannel(this)
        setContentView(buildUi())
        requestNotificationsIfNeeded()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi(): ScrollView {
        val pad = dp(20)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Sports Now Bar Lab"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "V0.1 · 只验证 Samsung Now Bar / Live Updates"
            textSize = 15f
            setPadding(0, dp(4), 0, dp(18))
        })

        statusText = TextView(this).apply {
            textSize = 15f
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        root.addView(statusText, fullWidth())

        root.addView(sectionTitle("开始一场模拟赛事"))
        root.addView(button("⚽ 足球 · Arsenal vs Chelsea") { start(MatchKind.FOOTBALL) })
        root.addView(button("🏀 篮球 · Lakers vs Celtics") { start(MatchKind.BASKETBALL) })
        root.addView(button("🎮 LOL · BLG vs TES") { start(MatchKind.LOL) })

        root.addView(sectionTitle("控制"))
        root.addView(button("更新一次比分") { updateOnce() })
        root.addView(button("结束 Live Update") {
            LiveMatchNotifier.cancel(this)
            refreshStatus()
        })
        root.addView(button("打开 Live Updates / 提升通知设置") { openPromotionSettings() })

        root.addView(sectionTitle("当前模拟数据"))
        currentText = TextView(this).apply {
            textSize = 15f
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        root.addView(currentText, fullWidth())

        root.addView(TextView(this).apply {
            text = "测试方法：开始赛事 → 锁屏观察 Now Bar → 解锁观察状态栏 chip → 从通知/Now Bar 尝试展开 → 点“模拟更新”或回 App 更新 → 最后结束。"
            textSize = 14f
            setPadding(0, dp(18), 0, dp(40))
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun start(kind: MatchKind) {
        if (!LiveMatchNotifier.hasNotificationPermission(this)) {
            requestNotificationsIfNeeded()
            Toast.makeText(this, "先允许通知权限", Toast.LENGTH_SHORT).show()
            return
        }
        val snapshot = MatchSimulator.initial(kind)
        LiveMatchNotifier.post(this, snapshot)
        refreshStatus()
    }

    private fun updateOnce() {
        val current = MatchStore.load(this)
        if (current == null) {
            Toast.makeText(this, "请先开始一场赛事", Toast.LENGTH_SHORT).show()
            return
        }
        LiveMatchNotifier.post(this, MatchSimulator.next(current))
        refreshStatus()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    private fun openPromotionSettings() {
        if (Build.VERSION.SDK_INT < 36) {
            Toast.makeText(this, "系统低于 Android 16，不支持 promoted notifications", Toast.LENGTH_LONG).show()
            return
        }
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

    private fun refreshStatus() {
        val notif = LiveMatchNotifier.hasNotificationPermission(this)
        val promoted = if (Build.VERSION.SDK_INT >= 36) LiveMatchNotifier.canPostPromoted(this) else false
        val fullSdk = if (Build.VERSION.SDK_INT >= 36) Build.VERSION.SDK_INT_FULL.toString() else "n/a"
        statusText.text = buildString {
            append("Android API: ").append(Build.VERSION.SDK_INT).append("  · full: ").append(fullSdk).append('\n')
            append("普通通知权限: ").append(if (notif) "✅" else "❌").append('\n')
            append("可发布 Promoted/Live Update: ").append(if (promoted) "✅" else "❌ / 未开启")
        }

        val current = MatchStore.load(this)
        currentText.text = current?.let { "${it.title}\n${it.summary}\n\n${it.details}\n\nchip = ${it.chip}" }
            ?: "暂无正在进行的模拟赛事"
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(24), 0, dp(8))
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        gravity = Gravity.CENTER_VERTICAL
        setOnClickListener { onClick() }
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
