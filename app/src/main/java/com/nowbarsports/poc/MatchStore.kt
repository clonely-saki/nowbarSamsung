package com.nowbarsports.poc

import android.content.Context
import org.json.JSONObject

object MatchStore {
    private const val PREF = "match_state"
    private const val SOURCE_TYPE = "sourceType"
    private const val SOURCE_MOCK = "mock"
    private const val SOURCE_FOOTBALL_REAL = "football_real"
    private const val REAL_SNAPSHOT = "realSnapshot"
    private const val FIXTURE_ID = "fixtureId"

    fun save(context: Context, s: MatchSnapshot) {
        val editor = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("eventId", s.eventId)
            .putInt("step", s.step)
        if (FootballRealDataSource.isRealEvent(s.eventId)) {
            editor
                .putString(SOURCE_TYPE, SOURCE_FOOTBALL_REAL)
                .putString(FIXTURE_ID, FootballRealDataSource.fixtureIdFromEventId(s.eventId))
                .putString(REAL_SNAPSHOT, encodeSnapshot(s))
        } else {
            editor
                .putString(SOURCE_TYPE, SOURCE_MOCK)
                .remove(FIXTURE_ID)
                .remove(REAL_SNAPSHOT)
        }
        editor.apply()
    }

    fun load(context: Context, dataSource: MatchDataSource): MatchSnapshot? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (p.getString(SOURCE_TYPE, SOURCE_MOCK) == SOURCE_FOOTBALL_REAL) {
            return p.getString(REAL_SNAPSHOT, null)?.let(::decodeSnapshot)
        }
        val eventId = p.getString("eventId", null) ?: return null
        return dataSource.restore(eventId, p.getInt("step", 0))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun encodeSnapshot(snapshot: MatchSnapshot): String = JSONObject().apply {
        put("eventId", snapshot.eventId)
        put("kind", snapshot.kind.name)
        put("section", snapshot.section.name)
        put("title", snapshot.title)
        put("compactPrimary", snapshot.compactPrimary)
        putNullable("compactSecondary", snapshot.compactSecondary)
        putNullable("compactTertiary", snapshot.compactTertiary)
        put("expandedTitle", snapshot.expandedTitle)
        put("expandedSummary", snapshot.expandedSummary)
        put("expandedDetails", snapshot.expandedDetails)
        put("chip", snapshot.chip)
        put("step", snapshot.step)
        putNullable("lolPhase", snapshot.lolPhase?.name)
        put("branding", encodeBranding(snapshot.branding))
    }.toString()

    private fun decodeSnapshot(value: String): MatchSnapshot? = runCatching {
        val json = JSONObject(value)
        MatchSnapshot(
            eventId = json.getString("eventId"),
            kind = MatchKind.valueOf(json.getString("kind")),
            section = EventSection.valueOf(json.getString("section")),
            branding = decodeBranding(json.getJSONObject("branding")),
            title = json.getString("title"),
            compactPrimary = json.getString("compactPrimary"),
            compactSecondary = json.optNullableString("compactSecondary"),
            compactTertiary = json.optNullableString("compactTertiary"),
            expandedTitle = json.getString("expandedTitle"),
            expandedSummary = json.getString("expandedSummary"),
            expandedDetails = json.getString("expandedDetails"),
            chip = json.getString("chip"),
            step = json.getInt("step"),
            lolPhase = json.optNullableString("lolPhase")?.let(LolPhase::valueOf)
        )
    }.getOrNull()

    private fun encodeBranding(branding: EventBranding): JSONObject = JSONObject().apply {
        put("competition", encodeIdentity(branding.competition))
        put("primary", encodeIdentity(branding.primary))
        putNullable("secondary", branding.secondary?.let(::encodeIdentity))
    }

    private fun decodeBranding(json: JSONObject): EventBranding = EventBranding(
        competition = decodeIdentity(json.getJSONObject("competition")),
        primary = decodeIdentity(json.getJSONObject("primary")),
        secondary = json.optJSONObject("secondary")?.let(::decodeIdentity)
    )

    private fun encodeIdentity(identity: BrandIdentity): JSONObject = JSONObject().apply {
        put("name", identity.name)
        put("shortName", identity.shortName)
        put("initials", identity.initials)
        putNullable("logoResId", identity.logoResId)
    }

    private fun decodeIdentity(json: JSONObject): BrandIdentity = BrandIdentity(
        name = json.getString("name"),
        shortName = json.getString("shortName"),
        initials = json.getString("initials"),
        logoResId = json.optNullableInt("logoResId")
    )

    private fun JSONObject.putNullable(name: String, value: Any?) {
        put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name) || !has(name)) null else optInt(name)
}
