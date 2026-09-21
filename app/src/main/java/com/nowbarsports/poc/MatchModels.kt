package com.nowbarsports.poc

enum class MatchKind { FOOTBALL, BASKETBALL, LOL }

data class MatchSnapshot(
    val kind: MatchKind,
    val title: String,
    val summary: String,
    val details: String,
    val chip: String,
    val step: Int
)

object MatchSimulator {
    fun initial(kind: MatchKind): MatchSnapshot = when (kind) {
        MatchKind.FOOTBALL -> football(0)
        MatchKind.BASKETBALL -> basketball(0)
        MatchKind.LOL -> lol(0)
    }

    fun next(current: MatchSnapshot): MatchSnapshot = when (current.kind) {
        MatchKind.FOOTBALL -> football(current.step + 1)
        MatchKind.BASKETBALL -> basketball(current.step + 1)
        MatchKind.LOL -> lol(current.step + 1)
    }

    private fun football(step: Int): MatchSnapshot {
        val minute = 62 + step.coerceAtMost(25)
        val home = when {
            step >= 7 -> 2
            else -> 1
        }
        val away = if (step >= 3) 1 else 0
        val details = buildString {
            append("Premier League · ").append(minute).append("′\n")
            append("Arsenal ").append(home).append(" — ").append(away).append(" Chelsea\n")
            append("射门 12 : 8   ·   射正 5 : 3\n")
            append("控球 54% : 46%")
            if (step >= 7) append("\n⚽ Arsenal 69′")
            else if (step >= 3) append("\n⚽ Chelsea 65′")
        }
        return MatchSnapshot(
            MatchKind.FOOTBALL,
            "Arsenal vs Chelsea",
            "$home - $away · ${minute}′",
            details,
            "$home-$away",
            step
        )
    }

    private fun basketball(step: Int): MatchSnapshot {
        val lakers = 88 + step * 2
        val celtics = 87 + (step / 2) * 2
        val remainingSec = (402 - step * 18).coerceAtLeast(0)
        val min = remainingSec / 60
        val sec = remainingSec % 60
        val clock = "%d:%02d".format(min, sec)
        return MatchSnapshot(
            MatchKind.BASKETBALL,
            "Lakers vs Celtics",
            "$lakers - $celtics · Q4 $clock",
            "NBA · Q4 $clock\nLakers $lakers — $celtics Celtics\nFG 48% : 46%\nRebounds 39 : 36",
            "$lakers-$celtics".take(7),
            step
        )
    }

    private fun lol(step: Int): MatchSnapshot {
        val elapsed = 24 * 60 + 36 + step * 35
        val min = elapsed / 60
        val sec = elapsed % 60
        val blgKills = 12 + step / 2
        val tesKills = 8 + step / 3
        val blgGold = 45.2 + step * 0.7
        val tesGold = 42.7 + step * 0.6
        return MatchSnapshot(
            MatchKind.LOL,
            "BLG vs TES",
            "Series 1-1 · Game 3 · %d:%02d".format(min, sec),
            "LPL · Game 3 · %d:%02d\nKills $blgKills : $tesKills\nGold %.1fK : %.1fK\nTowers 6 : 3 · Dragons 2 : 1".format(min, sec, blgGold, tesGold),
            "1-1 G3",
            step
        )
    }
}
