package com.nowbarsports.poc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MatchActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            LiveMatchNotifier.ACTION_UPDATE -> {
                val current = MatchStore.load(context) ?: return
                LiveMatchNotifier.post(context, MatchSimulator.next(current))
            }
            LiveMatchNotifier.ACTION_END -> LiveMatchNotifier.cancel(context)
        }
    }
}
