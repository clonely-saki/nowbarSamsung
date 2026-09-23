package com.nowbarsports.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun FootballScoreCard(
    snapshot: MatchSnapshot,
    presentation: FootballFixturePresentation?,
    followedTeamIds: Set<String>,
    tracked: Boolean,
    onToggleTeamFollow: (String) -> Unit,
    onTrack: () -> Unit,
    onStopTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val homeName = presentation?.homeTeamName ?: snapshot.branding.primary.name
    val awayName = presentation?.awayTeamName ?: snapshot.branding.secondary?.name.orEmpty()
    val homeId = presentation?.homeTeamId ?: FollowStore.teamId(snapshot.branding.primary)
    val awayIdentity = snapshot.branding.secondary
    val awayId = presentation?.awayTeamId ?: awayIdentity?.let(FollowStore::teamId).orEmpty()
    val status = presentation?.statusShort.orEmpty()
    val upcoming = snapshot.section == EventSection.UPCOMING || status == "NS"
    val statusLabel = footballStatusLabel(status, snapshot)
    val homeScore = presentation?.homeScore
    val awayScore = presentation?.awayScore

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!presentation?.leagueLogoUrl.isNullOrBlank()) {
                    FixtureLogo(
                        url = presentation?.leagueLogoUrl,
                        initials = presentation?.leagueName?.take(2).orEmpty(),
                        size = 24.dp,
                        isLeague = true
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presentation?.leagueName ?: snapshot.branding.competition.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    presentation?.round?.let { round ->
                        Text(
                            text = round,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (status == "1H" || status == "2H" || status == "ET" || status == "P") {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = statusLabel.first,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (status == "1H" || status == "2H" || status == "ET" || status == "P") {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FootballTeamColumn(
                    name = homeName,
                    logoUrl = presentation?.homeLogoUrl,
                    fallback = snapshot.branding.primary.initials,
                    teamId = homeId,
                    followed = homeId in followedTeamIds,
                    onToggleFollow = onToggleTeamFollow,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier.weight(1.25f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (upcoming) {
                        Text(
                            text = "VS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = presentation?.kickoffText ?: snapshot.compactSecondaryLine.ifBlank { "待定" },
                            modifier = Modifier.padding(top = 7.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (homeScore != null && awayScore != null) "$homeScore  —  $awayScore" else "—  —  —",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = statusLabel.second,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (statusLabel.first == "LIVE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FootballTeamColumn(
                    name = awayName,
                    logoUrl = presentation?.awayLogoUrl,
                    fallback = awayIdentity?.initials ?: "客",
                    teamId = awayId,
                    followed = awayId in followedTeamIds,
                    onToggleFollow = onToggleTeamFollow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tracked) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "✓ 正在追踪",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = onStopTracking) { Text("停止追踪") }
                } else {
                    Button(onClick = onTrack, modifier = Modifier.fillMaxWidth()) {
                        Text("追踪本场")
                    }
                }
            }
        }
    }
}

@Composable
private fun FootballTeamColumn(
    name: String,
    logoUrl: String?,
    fallback: String,
    teamId: String,
    followed: Boolean,
    onToggleFollow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FixtureLogo(url = logoUrl, initials = fallback, size = 68.dp)
        Text(
            text = name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2
        )
        TextButton(
            onClick = { onToggleFollow(teamId) },
            modifier = Modifier.height(36.dp),
            enabled = teamId.isNotBlank()
        ) {
            Text(
                text = if (followed) "★ $name" else "☆ $name",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FixtureLogo(
    url: String?,
    initials: String,
    size: androidx.compose.ui.unit.Dp,
    isLeague: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(if (isLeague) RoundedCornerShape(6.dp) else CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifBlank { "⚽" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(if (isLeague) RoundedCornerShape(6.dp) else CircleShape),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun footballStatusLabel(status: String, snapshot: MatchSnapshot): Pair<String, String> = when (status) {
    "NS" -> "UPCOMING" to (snapshot.compactSecondary ?: "待开赛")
    "1H", "2H", "ET" -> "LIVE" to (snapshot.compactSecondary ?: "进行中")
    "HT" -> "HT" to "HALF TIME"
    "FT", "AET", "PEN" -> "FINAL" to "FULL TIME"
    "PST" -> "POSTPONED" to "比赛延期"
    "CANC" -> "CANCELLED" to "比赛取消"
    "SUSP" -> "SUSPENDED" to "比赛暂停"
    "INT" -> "INTERRUPTED" to "比赛中断"
    "BT" -> "BREAK" to "加时休息"
    "P" -> "LIVE" to "点球大战"
    else -> when (snapshot.section) {
        EventSection.UPCOMING -> "UPCOMING" to (snapshot.compactSecondary ?: "待开赛")
        EventSection.FINISHED -> "FINAL" to "FULL TIME"
        EventSection.LIVE -> "LIVE" to (snapshot.compactSecondary ?: "进行中")
    }
}
