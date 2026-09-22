package com.nowbarsports.poc

enum class MatchKind {
    FOOTBALL,
    BASKETBALL,
    LOL,
    FORMULA1
}

enum class EventSection {
    LIVE,
    UPCOMING,
    FINISHED
}

enum class LolPhase {
    IN_GAME,
    BETWEEN_GAMES,
    FINISHED
}

/** A future logo can be supplied through logoResId without changing the UI model. */
data class BrandIdentity(
    val name: String,
    val shortName: String,
    val initials: String,
    val logoResId: Int? = null
)

data class EventBranding(
    val competition: BrandIdentity,
    val primary: BrandIdentity,
    val secondary: BrandIdentity? = null
)

data class MockEvent(
    val id: String,
    val kind: MatchKind,
    val section: EventSection,
    val title: String,
    val branding: EventBranding
)

data class MatchSnapshot(
    val eventId: String,
    val kind: MatchKind,
    val section: EventSection,
    val branding: EventBranding,
    val title: String,
    // Compact Now Bar content is intentionally kept separate from expanded notification content.
    val compactPrimary: String,
    val compactSecondary: String? = null,
    val compactTertiary: String? = null,
    // Expanded notification content is the richer sport-specific state shown after expansion.
    val expandedTitle: String,
    val expandedSummary: String,
    val expandedDetails: String,
    val chip: String,
    val step: Int,
    val lolPhase: LolPhase? = null
) {
    val compactSecondaryLine: String
        get() = listOfNotNull(compactSecondary, compactTertiary).joinToString(" · ")
}

object MockEventCatalog {
    private val premierLeague = BrandIdentity("Premier League", "PL", "PL")
    private val nba = BrandIdentity("NBA", "NBA", "NBA")
    private val lpl = BrandIdentity("League of Legends · LPL", "LPL", "LPL")
    private val formulaOne = BrandIdentity("Formula 1", "F1", "F1")

    private val arsenal = BrandIdentity("Arsenal", "ARS", "ARS")
    private val chelsea = BrandIdentity("Chelsea", "CHE", "CHE")
    private val lakers = BrandIdentity("Los Angeles Lakers", "LAL", "LAL")
    private val celtics = BrandIdentity("Boston Celtics", "BOS", "BOS")
    private val blg = BrandIdentity("Bilibili Gaming", "BLG", "BLG")
    private val tes = BrandIdentity("Top Esports", "TES", "TES")
    private val verstappen = BrandIdentity("Max Verstappen", "VER", "VER")
    private val redBull = BrandIdentity("Red Bull Racing", "RBR", "RB")

    val events: List<MockEvent> = listOf(
        MockEvent("football-live", MatchKind.FOOTBALL, EventSection.LIVE, "Arsenal vs Chelsea", EventBranding(premierLeague, arsenal, chelsea)),
        MockEvent("basketball-live", MatchKind.BASKETBALL, EventSection.LIVE, "Lakers vs Celtics", EventBranding(nba, lakers, celtics)),
        MockEvent("lol-live", MatchKind.LOL, EventSection.LIVE, "BLG vs TES", EventBranding(lpl, blg, tes)),
        MockEvent("formula1-live", MatchKind.FORMULA1, EventSection.LIVE, "Max Verstappen · Japanese GP", EventBranding(formulaOne, verstappen, redBull)),
        MockEvent("football-upcoming", MatchKind.FOOTBALL, EventSection.UPCOMING, "Arsenal vs Chelsea", EventBranding(premierLeague, arsenal, chelsea)),
        MockEvent("lol-upcoming", MatchKind.LOL, EventSection.UPCOMING, "BLG vs TES", EventBranding(lpl, blg, tes)),
        MockEvent("basketball-finished", MatchKind.BASKETBALL, EventSection.FINISHED, "Lakers vs Celtics", EventBranding(nba, lakers, celtics)),
        MockEvent("formula1-finished", MatchKind.FORMULA1, EventSection.FINISHED, "Max Verstappen · Japanese GP", EventBranding(formulaOne, verstappen, redBull))
    )

    fun find(eventId: String): MockEvent? = events.firstOrNull { it.id == eventId }
}

object MatchSimulator {
    fun initial(eventId: String): MatchSnapshot = snapshot(eventId, 0)

    fun next(current: MatchSnapshot): MatchSnapshot {
        if (current.section == EventSection.UPCOMING || current.lolPhase == LolPhase.FINISHED) return current
        return snapshot(current.eventId, current.step + 1)
    }

    fun snapshot(eventId: String, step: Int): MatchSnapshot {
        val event = MockEventCatalog.find(eventId) ?: error("Unknown mock event: $eventId")
        return when (event.kind) {
            MatchKind.FOOTBALL -> football(event, step)
            MatchKind.BASKETBALL -> basketball(event, step)
            MatchKind.LOL -> lol(event, step)
            MatchKind.FORMULA1 -> formula1(event, step)
        }
    }

    private fun football(event: MockEvent, step: Int): MatchSnapshot {
        if (event.section == EventSection.UPCOMING) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "ARS vs CHE", compactSecondary = "18:30",
                expandedTitle = "ARS vs CHE", expandedSummary = "Premier League · Kick-off 18:30",
                expandedDetails = "Arsenal vs Chelsea\nKick-off 18:30", chip = "ARS", step = step
            )
        }
        if (event.section == EventSection.FINISHED) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "ARS 3 : 0 CHE", compactSecondary = "FINAL",
                expandedTitle = "ARS vs CHE", expandedSummary = "Premier League · FINAL",
                expandedDetails = "Score 3 : 0", chip = "3-0", step = step
            )
        }

        val minute = 73 + step.coerceAtMost(8)
        val homeScore = if (step >= 4) 3 else 2
        val awayScore = if (step >= 1) 2 else 1
        val section = if (step >= 6) EventSection.FINISHED else EventSection.LIVE
        val secondary = if (section == EventSection.FINISHED) "FINAL" else "${minute}'"
        return MatchSnapshot(
            eventId = event.id, kind = event.kind, section = section, branding = event.branding, title = event.title,
            compactPrimary = "ARS $homeScore : $awayScore CHE", compactSecondary = secondary,
            expandedTitle = "ARS vs CHE", expandedSummary = "Premier League · ${minute}'",
            expandedDetails = "Score $homeScore : $awayScore\nShots ${12 + step} : ${8 + step / 2}\nShots on target ${5 + step / 2} : ${3 + step / 3}",
            chip = "$homeScore-$awayScore", step = step
        )
    }

    private fun basketball(event: MockEvent, step: Int): MatchSnapshot {
        if (event.section == EventSection.UPCOMING) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "LAL vs BOS", compactSecondary = "20:00",
                expandedTitle = "LAL vs BOS", expandedSummary = "NBA · Tip-off 20:00",
                expandedDetails = "Lakers vs Celtics", chip = "LAL", step = step
            )
        }
        if (event.section == EventSection.FINISHED) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "LAL 110 : 104 BOS", compactSecondary = "FINAL",
                expandedTitle = "LAL vs BOS", expandedSummary = "NBA · FINAL",
                expandedDetails = "Score 110 : 104", chip = "110-104", step = step
            )
        }

        val lakersScore = 102 + step * 2
        val celticsScore = 98 + step
        val remainingSeconds = (201 - step * 30).coerceAtLeast(0)
        val clock = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
        val section = if (step >= 7) EventSection.FINISHED else EventSection.LIVE
        val secondary = if (section == EventSection.FINISHED) "FINAL" else "Q4 $clock"
        return MatchSnapshot(
            eventId = event.id, kind = event.kind, section = section, branding = event.branding, title = event.title,
            compactPrimary = "LAL $lakersScore : $celticsScore BOS", compactSecondary = secondary,
            expandedTitle = "LAL vs BOS", expandedSummary = "NBA · Q4 · $clock",
            expandedDetails = "Score $lakersScore : $celticsScore\nFG% 48 : ${45 + step / 2}\nRebounds ${41 + step} : ${39 + step / 2}",
            chip = "$lakersScore-$celticsScore".take(7), step = step
        )
    }

    private fun lol(event: MockEvent, step: Int): MatchSnapshot {
        if (event.section == EventSection.UPCOMING) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "BLG vs TES", compactSecondary = "19:00",
                expandedTitle = "BLG vs TES", expandedSummary = "LPL · Series starts 19:00",
                expandedDetails = "BLG vs TES", chip = "BLG", step = step
            )
        }
        if (event.section == EventSection.FINISHED) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "BLG 3 : 1 TES", compactSecondary = "FINAL",
                expandedTitle = "BLG vs TES", expandedSummary = "LPL · FINAL",
                expandedDetails = "Series 3-1", chip = "3-1", step = step, lolPhase = LolPhase.FINISHED
            )
        }

        return when {
            step <= 1 -> {
                val gameTimeSeconds = 24 * 60 + 36 + step * 42
                val gameTime = "%d:%02d".format(gameTimeSeconds / 60, gameTimeSeconds % 60)
                val blgKills = 12 + step * 3
                val tesKills = 8 + step * 2
                MatchSnapshot(
                    eventId = event.id, kind = event.kind, section = EventSection.LIVE, branding = event.branding, title = event.title,
                    compactPrimary = "BLG $blgKills : $tesKills TES", compactSecondary = "G3 · $gameTime", compactTertiary = "Series 1-1",
                    expandedTitle = "BLG vs TES", expandedSummary = "LPL · Game 3 · $gameTime",
                    expandedDetails = "Series 1-1\nKills $blgKills : $tesKills\nGold %.1fK : %.1fK\nTowers 6 : 3 · Dragons 2 : 1".format(45.2 + step * 0.7, 42.7 + step * 0.6),
                    chip = "$blgKills-$tesKills", step = step, lolPhase = LolPhase.IN_GAME
                )
            }
            step == 2 -> MatchSnapshot(
                eventId = event.id, kind = event.kind, section = EventSection.LIVE, branding = event.branding, title = event.title,
                compactPrimary = "BLG 2 : 1 TES", compactSecondary = "GAME 4 NEXT",
                expandedTitle = "BLG vs TES", expandedSummary = "LPL · GAME 4 NEXT",
                expandedDetails = "Series 2-1\nBetween games", chip = "2-1", step = step, lolPhase = LolPhase.BETWEEN_GAMES
            )
            step == 3 -> MatchSnapshot(
                eventId = event.id, kind = event.kind, section = EventSection.LIVE, branding = event.branding, title = event.title,
                compactPrimary = "BLG 3 : 2 TES", compactSecondary = "G4 · 00:48", compactTertiary = "Series 2-1",
                expandedTitle = "BLG vs TES", expandedSummary = "LPL · Game 4 · 00:48",
                expandedDetails = "Series 2-1\nKills 3 : 2\nGold 7.8K : 6.9K\nTowers 1 : 0 · Dragons 0 : 0", chip = "3-2", step = step, lolPhase = LolPhase.IN_GAME
            )
            else -> MatchSnapshot(
                eventId = event.id, kind = event.kind, section = EventSection.FINISHED, branding = event.branding, title = event.title,
                compactPrimary = "BLG 3 : 1 TES", compactSecondary = "FINAL",
                expandedTitle = "BLG vs TES", expandedSummary = "LPL · FINAL",
                expandedDetails = "Series 3-1", chip = "3-1", step = step, lolPhase = LolPhase.FINISHED
            )
        }
    }

    private fun formula1(event: MockEvent, step: Int): MatchSnapshot {
        if (event.section == EventSection.UPCOMING) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "VER · RACE", compactSecondary = "SUN 15:00",
                expandedTitle = "Japanese GP", expandedSummary = "VER · RACE",
                expandedDetails = "Max Verstappen\nRed Bull Racing\nRace starts Sunday 15:00", chip = "VER", step = step
            )
        }
        if (event.section == EventSection.FINISHED) {
            return MatchSnapshot(
                eventId = event.id, kind = event.kind, section = event.section, branding = event.branding, title = event.title,
                compactPrimary = "VER P2 · L57/57", compactSecondary = "FINAL",
                expandedTitle = "Japanese GP", expandedSummary = "VER · P2",
                expandedDetails = "Lap 57 / 57\nGap +4.12\nStatus FINISHED", chip = "P2", step = step
            )
        }

        val lap = 37 + step.coerceAtMost(20)
        val position = if (step >= 4) 2 else 1
        val gap = if (step >= 4) "+4.12" else "+%.2f".format(1.28 - step * 0.09)
        val status = when {
            step >= 5 -> "RED FLAG"
            step == 3 -> "VSC"
            step == 2 -> "SC"
            else -> "GREEN"
        }
        val section = if (step >= 8) EventSection.FINISHED else EventSection.LIVE
        return MatchSnapshot(
            eventId = event.id, kind = event.kind, section = section, branding = event.branding, title = event.title,
            compactPrimary = "VER P$position · L$lap/57", compactSecondary = if (section == EventSection.FINISHED) "FINAL" else gap,
            expandedTitle = "Japanese GP", expandedSummary = "VER · P$position",
            expandedDetails = "Driver Max Verstappen\nTeam Red Bull Racing\nLap $lap / 57\nGap $gap\nStatus $status",
            chip = "P$position", step = step
        )
    }
}
