package com.nowbarsports.poc

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object FootballFixtureMapper {
    private val kickoffFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault())

    fun toSnapshot(dto: FootballFixtureDto, revision: Int = 0): MatchSnapshot {
        val status = dto.fixture.status.short.trim().uppercase(Locale.US)
        val home = dto.teams.home.toBrandIdentity()
        val away = dto.teams.away.toBrandIdentity()
        val competition = BrandIdentity(
            name = dto.league.name ?: "Football",
            shortName = dto.league.name?.take(8) ?: "足球",
            initials = dto.league.name?.initials() ?: "足球"
        )
        val eventId = FootballRealDataSource.eventIdFor(dto.fixture.id.toString())
        val section = sectionFor(status)
        val score = scoreText(dto.goals.home, dto.goals.away)
        val minute = dto.fixture.status.elapsed?.let { "$it'" }
        val kickoff = kickoffText(dto.fixture.timestamp, dto.fixture.date)
        val stateText = stateText(status, minute)
        val shortHome = home.shortName
        val shortAway = away.shortName
        val compactPrimary = when (section) {
            EventSection.UPCOMING -> "$shortHome vs $shortAway"
            else -> "$shortHome $score $shortAway"
        }
        val compactSecondary = when (status) {
            "NS" -> kickoff ?: "未开始"
            "1H", "2H", "ET" -> minute ?: stateText
            "HT", "BT", "P" -> stateText
            "FT", "AET", "PEN" -> "FINAL"
            else -> stateText
        }
        val leagueText = dto.league.name ?: "Football"
        val summary = when (status) {
            "NS" -> "$leagueText · Kick-off ${kickoff ?: "待定"}"
            "FT", "AET", "PEN" -> "$leagueText · FINAL"
            else -> "$leagueText · $stateText"
        }
        val details = buildString {
            append("Score ").append(score)
            if (!kickoff.isNullOrBlank()) append("\nKick-off ").append(kickoff)
            append("\nStatus ").append(dto.fixture.status.long ?: status)
        }
        val chip = when {
            dto.goals.home != null && dto.goals.away != null -> "${dto.goals.home}-${dto.goals.away}"
            else -> status.take(7)
        }

        return MatchSnapshot(
            eventId = eventId,
            kind = MatchKind.FOOTBALL,
            section = section,
            branding = EventBranding(
                competition = competition,
                primary = home,
                secondary = away
            ),
            title = "${home.name} vs ${away.name}",
            compactPrimary = compactPrimary,
            compactSecondary = compactSecondary,
            expandedTitle = "$shortHome vs $shortAway",
            expandedSummary = summary,
            expandedDetails = details,
            chip = chip,
            step = revision
        )
    }

    private fun sectionFor(status: String): EventSection = when (status) {
        "NS" -> EventSection.UPCOMING
        "1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT" -> EventSection.LIVE
        "FT", "AET", "PEN", "PST", "CANC" -> EventSection.FINISHED
        else -> EventSection.LIVE
    }

    private fun stateText(status: String, minute: String?): String = when (status) {
        "NS" -> "未开始"
        "1H" -> minute ?: "上半场"
        "HT" -> "半场"
        "2H" -> minute ?: "下半场"
        "ET" -> minute ?: "加时"
        "BT" -> "加时休息"
        "P" -> "点球"
        "FT", "AET", "PEN" -> "FINAL"
        "PST" -> "延期"
        "CANC" -> "取消"
        "SUSP" -> "暂停"
        "INT" -> "中断"
        else -> status
    }

    private fun scoreText(home: Int?, away: Int?): String =
        "${home?.toString() ?: "-"} : ${away?.toString() ?: "-"}"

    private fun kickoffText(timestamp: Long?, date: String?): String? = try {
        when {
            timestamp != null && timestamp > 0 -> Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(kickoffFormatter)
            !date.isNullOrBlank() -> date.replace('T', ' ').take(16)
            else -> null
        }
    } catch (_: RuntimeException) {
        date?.replace('T', ' ')?.take(16)
    }

    internal fun kickoffTextForUi(timestamp: Long?, date: String?): String? =
        kickoffText(timestamp, date)

    private fun TeamInfoDto.toBrandIdentity(): BrandIdentity {
        val fullName = name?.takeIf { it.isNotBlank() } ?: code ?: "Unknown"
        val short = code?.takeIf { it.isNotBlank() }
            ?: fullName.split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .joinToString("") { it.first().uppercaseChar().toString() }
                .take(4)
                .ifBlank { "TEAM" }
        return BrandIdentity(
            name = fullName,
            shortName = short,
            initials = short.take(3).uppercase(Locale.US)
        )
    }

    private fun String.initials(): String = split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .take(3)
}
