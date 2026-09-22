package com.nowbarsports.poc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MatchActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dataSource = MatchDataSources.current
        when (intent.action) {
            LiveMatchNotifier.ACTION_UPDATE -> {
                val current = MatchStore.load(context, dataSource) ?: return
                LiveMatchNotifier.post(context, dataSource.next(current))
            }
            LiveMatchNotifier.ACTION_END -> LiveMatchNotifier.cancel(context)
        }
    }
}
