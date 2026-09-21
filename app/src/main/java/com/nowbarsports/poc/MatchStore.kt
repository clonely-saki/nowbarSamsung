package com.nowbarsports.poc

import android.content.Context

object MatchStore {
    private const val PREF = "match_state"

    fun save(context: Context, s: MatchSnapshot) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("kind", s.kind.name)
            .putString("title", s.title)
            .putString("summary", s.summary)
            .putString("details", s.details)
            .putString("chip", s.chip)
            .putInt("step", s.step)
            .apply()
    }

    fun load(context: Context): MatchSnapshot? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val kind = p.getString("kind", null) ?: return null
        return MatchSnapshot(
            kind = MatchKind.valueOf(kind),
            title = p.getString("title", "") ?: "",
            summary = p.getString("summary", "") ?: "",
            details = p.getString("details", "") ?: "",
            chip = p.getString("chip", "") ?: "",
            step = p.getInt("step", 0)
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
