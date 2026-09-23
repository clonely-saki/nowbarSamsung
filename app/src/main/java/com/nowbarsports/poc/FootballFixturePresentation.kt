package com.nowbarsports.poc

import android.content.Context
import org.json.JSONObject

/** UI-only football fixture details; intentionally separate from MatchSnapshot's notification contract. */
data class FootballFixturePresentation(
    val homeTeamId: String?,
    val homeTeamName: String,
    val homeLogoUrl: String?,
    val awayTeamId: String?,
    val awayTeamName: String,
    val awayLogoUrl: String?,
    val leagueName: String,
    val leagueLogoUrl: String?,
    val round: String?,
    val statusShort: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val kickoffText: String?
)

object FootballFixturePresentationMapper {
    fun from(dto: FootballFixtureDto): FootballFixturePresentation = FootballFixturePresentation(
        homeTeamId = dto.teams.home.id?.toString(),
        homeTeamName = dto.teams.home.name?.takeIf { it.isNotBlank() } ?: dto.teams.home.code ?: "主队",
        homeLogoUrl = dto.teams.home.logo,
        awayTeamId = dto.teams.away.id?.toString(),
        awayTeamName = dto.teams.away.name?.takeIf { it.isNotBlank() } ?: dto.teams.away.code ?: "客队",
        awayLogoUrl = dto.teams.away.logo,
        leagueName = dto.league.name?.takeIf { it.isNotBlank() } ?: "足球赛事",
        leagueLogoUrl = dto.league.logo,
        round = dto.league.round?.takeIf { it.isNotBlank() },
        statusShort = dto.fixture.status.short.trim().uppercase(),
        homeScore = dto.goals.home,
        awayScore = dto.goals.away,
        kickoffText = FootballFixtureMapper.kickoffTextForUi(dto.fixture.timestamp, dto.fixture.date)
    )
}

/** Persists only display metadata so a restored real match still has its score, names and logo URLs. */
object FootballFixturePresentationStore {
    private const val PREFERENCES = "football_fixture_presentation"
    private const val KEY_PREFIX = "fixture:"

    fun save(context: Context, eventId: String, presentation: FootballFixturePresentation) {
        val json = JSONObject().apply {
            putNullable("homeTeamId", presentation.homeTeamId)
            put("homeTeamName", presentation.homeTeamName)
            putNullable("homeLogoUrl", presentation.homeLogoUrl)
            putNullable("awayTeamId", presentation.awayTeamId)
            put("awayTeamName", presentation.awayTeamName)
            putNullable("awayLogoUrl", presentation.awayLogoUrl)
            put("leagueName", presentation.leagueName)
            putNullable("leagueLogoUrl", presentation.leagueLogoUrl)
            putNullable("round", presentation.round)
            put("statusShort", presentation.statusShort)
            putNullable("homeScore", presentation.homeScore)
            putNullable("awayScore", presentation.awayScore)
            putNullable("kickoffText", presentation.kickoffText)
        }.toString()
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + eventId, json)
            .apply()
    }

    fun load(context: Context, eventId: String): FootballFixturePresentation? {
        val jsonText = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + eventId, null) ?: return null
        return runCatching {
            val json = JSONObject(jsonText)
            FootballFixturePresentation(
                homeTeamId = json.optNullableString("homeTeamId"),
                homeTeamName = json.getString("homeTeamName"),
                homeLogoUrl = json.optNullableString("homeLogoUrl"),
                awayTeamId = json.optNullableString("awayTeamId"),
                awayTeamName = json.getString("awayTeamName"),
                awayLogoUrl = json.optNullableString("awayLogoUrl"),
                leagueName = json.getString("leagueName"),
                leagueLogoUrl = json.optNullableString("leagueLogoUrl"),
                round = json.optNullableString("round"),
                statusShort = json.getString("statusShort"),
                homeScore = json.optNullableInt("homeScore"),
                awayScore = json.optNullableInt("awayScore"),
                kickoffText = json.optNullableString("kickoffText")
            )
        }.getOrNull()
    }

    private fun JSONObject.putNullable(name: String, value: Any?) {
        put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name) || !has(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name) || !has(name)) null else optInt(name)
}
