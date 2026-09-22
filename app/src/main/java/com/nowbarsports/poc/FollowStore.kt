package com.nowbarsports.poc

import android.content.Context

data class FollowState(
    val followedTeamIds: Set<String>,
    val trackedMatchId: String?
) {
    fun isTeamFollowed(teamId: String): Boolean =
        FollowStore.normalizeTeamId(teamId) in followedTeamIds

    fun isTracked(matchId: String): Boolean = trackedMatchId == matchId
}

object FollowStore {
    private const val PREF = "follow_state"
    private const val FOLLOWED_TEAM_IDS = "followedTeamIds"
    private const val TRACKED_MATCH_ID = "trackedMatchId"

    // Kept only to migrate the previous prototype's participant follow values.
    private const val LEGACY_PARTICIPANT_KEYS = "participantKeys"

    fun load(context: Context): FollowState {
        val preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val currentTeamIds = preferences
            .getStringSet(FOLLOWED_TEAM_IDS, null)
            ?.map(::normalizeTeamId)
            ?.toSet()
        val migratedTeamIds = preferences
            .getStringSet(LEGACY_PARTICIPANT_KEYS, emptySet())
            .orEmpty()
            .map { it.removePrefix("participant:") }
            .map(::normalizeTeamId)
            .toSet()

        return FollowState(
            followedTeamIds = currentTeamIds ?: migratedTeamIds,
            trackedMatchId = preferences.getString(TRACKED_MATCH_ID, null)
        )
    }

    fun toggleTeam(context: Context, teamId: String): FollowState {
        val current = load(context)
        val normalizedTeamId = normalizeTeamId(teamId)
        val next = current.followedTeamIds.toMutableSet()
        if (!next.add(normalizedTeamId)) next.remove(normalizedTeamId)
        return save(context, current.copy(followedTeamIds = next))
    }

    fun trackMatch(context: Context, matchId: String): FollowState =
        save(context, load(context).copy(trackedMatchId = matchId))

    fun clearTrackedMatch(context: Context): FollowState =
        save(context, load(context).copy(trackedMatchId = null))

    fun teamId(identity: BrandIdentity): String = normalizeTeamId(identity.shortName)

    internal fun normalizeTeamId(teamId: String): String =
        teamId.trim().uppercase()

    private fun save(context: Context, state: FollowState): FollowState {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putStringSet(FOLLOWED_TEAM_IDS, state.followedTeamIds)
            .apply {
                if (state.trackedMatchId == null) {
                    remove(TRACKED_MATCH_ID)
                } else {
                    putString(TRACKED_MATCH_ID, state.trackedMatchId)
                }
            }
            .apply()
        return state
    }
}
