package com.nowbarsports.poc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MatchActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            LiveMatchNotifier.ACTION_UPDATE -> {
                val pendingResult = goAsync()
                val dataSource = MatchDataSources.current
                val current = runCatching { MatchStore.load(context, dataSource) }.getOrNull()
                if (current == null) {
                    pendingResult.finish()
                    return
                }
                try {
                    dataSource.refresh(context, current) { result ->
                        try {
                            result.onSuccess { snapshot ->
                                if (FollowStore.load(context).isFollowed(snapshot)) {
                                    LiveMatchNotifier.post(context, snapshot)
                                } else {
                                    LiveMatchNotifier.cancel(context)
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } catch (_: RuntimeException) {
                    pendingResult.finish()
                }
            }
            LiveMatchNotifier.ACTION_END -> LiveMatchNotifier.cancel(context)
        }
    }
}
