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

    fun load(context: Context, dataSource: MatchDataSource): MatchSnapshot? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val eventId = p.getString("eventId", null) ?: return null
        return dataSource.restore(eventId, p.getInt("step", 0))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
