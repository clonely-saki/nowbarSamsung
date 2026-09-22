package com.nowbarsports.poc

/** Only the fields used by the first Football Real Data PoC are represented here. */
data class FootballFixtureDto(
    val fixture: FixtureInfoDto,
    val league: LeagueInfoDto,
    val teams: TeamsInfoDto,
    val goals: GoalsInfoDto
)

data class FixtureInfoDto(
    val id: Long,
    val date: String?,
    val timestamp: Long?,
    val status: FixtureStatusDto
)

data class FixtureStatusDto(
    val short: String,
    val long: String?,
    val elapsed: Int?
)

data class LeagueInfoDto(
    val name: String?,
    val round: String?,
    val logo: String?
)

data class TeamsInfoDto(
    val home: TeamInfoDto,
    val away: TeamInfoDto
)

data class TeamInfoDto(
    val id: Long?,
    val name: String?,
    val code: String?,
    val logo: String?
)

data class GoalsInfoDto(
    val home: Int?,
    val away: Int?
)
