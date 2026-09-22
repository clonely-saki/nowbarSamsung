package com.nowbarsports.poc

import android.content.Context

object MatchStore {
    private const val PREF = "match_state"

    fun save(context: Context, s: MatchSnapshot) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("eventId", s.eventId)
            .putInt("step", s.step)
            .apply()
    }

    fun load(context: Context): MatchSnapshot? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val eventId = p.getString("eventId", null) ?: return null
        return runCatching {
            MatchSimulator.snapshot(eventId, p.getInt("step", 0))
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
