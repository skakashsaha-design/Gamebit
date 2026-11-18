package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.ChessVariant
import com.example.chess.CountryCatalog
import com.example.chess.TimeControl
import com.example.data.local.AppSettings
import com.example.data.remote.AbuseReportDoc
import com.example.data.remote.CloudStudyDoc
import com.example.data.remote.ClubDoc
import com.example.data.remote.DirectMessageDoc
import com.example.data.remote.ExternalBroadcastItem
import com.example.data.remote.ExternalPlayerSummary
import com.example.data.remote.ExternalTvChannel
import com.example.data.remote.FriendshipDoc
import com.example.data.remote.MessageThreadDoc
import com.example.data.remote.OnlineGameDoc
import com.example.data.remote.PublicProfileDoc
import com.example.data.remote.TeamDoc
import com.example.data.remote.TournamentDoc
import com.example.data.remote.TournamentEntryDoc
import com.example.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityHubScreen(
    isOnline: Boolean,
    currentUserId: String?,
    isAdmin: Boolean,
    settings: AppSettings,
    myProfile: PublicProfileDoc?,
    leaderboardState: UiState<List<PublicProfileDoc>>,
    playerSearchResults: List<PublicProfileDoc>,
    tournamentsState: UiState<List<TournamentDoc>>,
    selectedTournamentEntries: List<TournamentEntryDoc>,
    clubsState: UiState<List<ClubDoc>>,
    teamsState: UiState<List<TeamDoc>>,
    liveWatchState: UiState<List<OnlineGameDoc>>,
    friendshipsState: UiState<List<FriendshipDoc>>,
    threadsState: UiState<List<MessageThreadDoc>>,
    directMessages: List<DirectMessageDoc>,
    publicStudiesState: UiState<List<CloudStudyDoc>>,
    broadcasts: List<ExternalBroadcastItem>,
    tvChannels: List<ExternalTvChannel>,
    externalPlayerSummary: ExternalPlayerSummary?,
    adminReports: List<AbuseReportDoc>,
    onRequestSignIn: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewsReader: () -> Unit,
    onSearchPlayers: (String) -> Unit,
    onCreateTournament: (String, String, ChessVariant, TimeControl) -> Unit,
    onJoinTournament: (TournamentDoc) -> Unit,
    onSelectTournamentForStandings: (String) -> Unit,
    onCreateClub: (String, String, String) -> Unit,
    onJoinClub: (ClubDoc) -> Unit,
    onCreateTeam: (String, String) -> Unit,
    onSpectateGame: (String) -> Unit,
    onLoadBroadcastsAndTv: () -> Unit,
    onChallengeFriend: (String, String, TimeControl, ChessVariant) -> Unit,
    onAddFriend: (String, String) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onBlockFriend: (String) -> Unit,
    onOpenMessageThread: (String) -> Unit,
    onSendDirectMessage: (String, String) -> Unit,
    onSubmitReport: (String, String, String) -> Unit,
    onLoadAdminReports: () -> Unit,
    onResolveReport: (String, String) -> Unit,
    onUpdateProfile: (String, String, String, String, Boolean) -> Unit,
    onUpdateLocalIdentity: (String, String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onLookupExternalPlayer: (String, String) -> Unit,
    onImportExternalPgn: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(selectedTab, isOnline) {
        if (selectedTab == 2) onLoadBroadcastsAndTv()
        if (selectedTab == 5 && isAdmin && isOnline) onLoadAdminReports()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ChessHub Community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenNewsReader, modifier = Modifier.testTag("hub_news_btn")) {
                    Icon(Icons.Default.Newspaper, contentDescription = "Chess News")
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("hub_settings_btn")) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tournaments") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Clubs & Teams") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Watch & TV") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Ranks & Social") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Profile & Lookup") })
            if (isAdmin) {
                Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Admin") })
            }
        }

        when (selectedTab) {
            0 -> TournamentsTab(
                isOnline = isOnline,
                currentUserId = currentUserId,
                tournamentsState = tournamentsState,
                entries = selectedTournamentEntries,
                onRequestSignIn = onRequestSignIn,
                onCreateTournament = onCreateTournament,
                onJoinTournament = onJoinTournament,
                onSelectTournament = onSelectTournamentForStandings,
            )
            1 -> ClubsTeamsTab(
                isOnline = isOnline,
                currentUserId = currentUserId,
                countryIso = settings.countryIso,
                clubsState = clubsState,
                teamsState = teamsState,
                publicStudiesState = publicStudiesState,
                onRequestSignIn = onRequestSignIn,
                onCreateClub = onCreateClub,
                onJoinClub = onJoinClub,
                onCreateTeam = onCreateTeam,
                onImportStudyPgn = onImportExternalPgn,
            )
            2 -> WatchAndBroadcastsTab(
                isOnline = isOnline,
                liveWatchState = liveWatchState,
                broadcasts = broadcasts,
                tvChannels = tvChannels,
                onSpectateGame = onSpectateGame,
                onRefresh = onLoadBroadcastsAndTv,
                onOpenNewsReader = onOpenNewsReader,
            )
            3 -> SocialAndLeaderboardTab(
                isOnline = isOnline,
                currentUserId = currentUserId,
                leaderboardState = leaderboardState,
                playerSearchResults = playerSearchResults,
                friendshipsState = friendshipsState,
                threadsState = threadsState,
                directMessages = directMessages,
                onRequestSignIn = onRequestSignIn,
                onSearchPlayers = onSearchPlayers,
                onAddFriend = onAddFriend,
                onAcceptFriend = onAcceptFriend,
                onBlockFriend = onBlockFriend,
                onChallengeFriend = onChallengeFriend,
                onOpenMessageThread = onOpenMessageThread,
                onSendDirectMessage = onSendDirectMessage,
                onOpenReport = { uid, name -> reportTarget = uid to name },
            )
            4 -> ProfileAndLookupTab(
                isOnline = isOnline,
                settings = settings,
                myProfile = myProfile,
                externalSummary = externalPlayerSummary,
                onRequestSignIn = onRequestSignIn,
                onUpdateProfile = onUpdateProfile,
                onUpdateLocalIdentity = onUpdateLocalIdentity,
                onAvatarSelected = onAvatarSelected,
                onLookupExternalPlayer = onLookupExternalPlayer,
                onImportExternalPgn = onImportExternalPgn,
                onSignOut = onSignOut,
            )
            else -> AdminModerationTab(
                reports = adminReports,
                onResolve = onResolveReport,
            )
        }
    }

    reportTarget?.let { (targetUid, targetName) ->
        var category by remember { mutableStateOf("Unsportsmanlike") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report $targetName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Unsportsmanlike", "Fair Play", "Harassment", "Spam").forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubmitReport(targetUid, category, details.ifBlank { category })
                    reportTarget = null
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SignInRequiredNotice(title: String, onRequestSignIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Sign in with Google to use ChessHub Online community features.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onRequestSignIn) { Text("Sign in with Google") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentsTab(
    isOnline: Boolean,
    currentUserId: String?,
    tournamentsState: UiState<List<TournamentDoc>>,
    entries: List<TournamentEntryDoc>,
    onRequestSignIn: () -> Unit,
    onCreateTournament: (String, String, ChessVariant, TimeControl) -> Unit,
    onJoinTournament: (TournamentDoc) -> Unit,
    onSelectTournament: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("arena") }
    var selectedTourId by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isOnline || currentUserId == null) {
            item { SignInRequiredNotice("Arena, Swiss & Simul Tournaments", onRequestSignIn) }
            return@LazyColumn
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create Tournament", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tournament Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("arena", "swiss", "simul").forEach { f ->
                            FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.uppercase()) })
                        }
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreateTournament(name.trim(), format, ChessVariant.STANDARD, TimeControl.PRESETS[3])
                                name = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Launch Event") }
                }
            }
        }
        when (tournamentsState) {
            UiState.Loading -> item { Text("Loading tournaments...") }
            is UiState.Error -> item { Text(tournamentsState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                items(tournamentsState.data, key = { it.id }) { t ->
                    val joined = currentUserId in t.participantUids
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedTourId = t.id
                            onSelectTournament(t.id)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${t.format.uppercase()} • ${t.timeControl}", style = MaterialTheme.typography.labelSmall)
                            }
                            Text("Players: ${t.participantUids.size} • Status: ${t.status}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!joined) {
                                    FilledTonalButton(onClick = { onJoinTournament(t) }) { Text("Join") }
                                } else {
                                    Text("Joined", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(onClick = {
                                    selectedTourId = t.id
                                    onSelectTournament(t.id)
                                }) { Text("Standings") }
                            }
                            if (selectedTourId == t.id && entries.isNotEmpty()) {
                                entries.forEachIndexed { idx, e ->
                                    Text("${idx + 1}. ${e.username} (${e.rating}) — ${e.points} pts", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubsTeamsTab(
    isOnline: Boolean,
    currentUserId: String?,
    countryIso: String,
    clubsState: UiState<List<ClubDoc>>,
    teamsState: UiState<List<TeamDoc>>,
    publicStudiesState: UiState<List<CloudStudyDoc>>,
    onRequestSignIn: () -> Unit,
    onCreateClub: (String, String, String) -> Unit,
    onJoinClub: (ClubDoc) -> Unit,
    onCreateTeam: (String, String) -> Unit,
    onImportStudyPgn: (String) -> Unit,
) {
    var clubName by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isOnline || currentUserId == null) {
            item { SignInRequiredNotice("Clubs, Teams & Cloud Studies", onRequestSignIn) }
            return@LazyColumn
        }
        item {
            Text("Clubs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = clubName, onValueChange = { clubName = it }, label = { Text("New Club Name") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = {
                    if (clubName.isNotBlank()) {
                        onCreateClub(clubName.trim(), "ChessHub Club", countryIso)
                        clubName = ""
                    }
                }) { Text("Create") }
            }
        }
        if (clubsState is UiState.Success) {
            items(clubsState.data, key = { it.id }) { club ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${CountryCatalog.flagForIso(club.countryIso)} ${club.name}", fontWeight = FontWeight.SemiBold)
                            Text("${club.memberCount} members • ${club.description}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (currentUserId !in club.memberUids) {
                            FilledTonalButton(onClick = { onJoinClub(club) }) { Text("Join") }
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Joined", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text("Teams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = teamName, onValueChange = { teamName = it }, label = { Text("New Team Name") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = {
                    if (teamName.isNotBlank()) {
                        onCreateTeam(teamName.trim(), "Team Match Squad")
                        teamName = ""
                    }
                }) { Text("Create") }
            }
        }
        if (teamsState is UiState.Success) {
            items(teamsState.data, key = { it.id }) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(team.name, fontWeight = FontWeight.SemiBold)
                        Text("${team.memberUids.size} players • Avg ${team.ratingAvg}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (publicStudiesState is UiState.Success && publicStudiesState.data.isNotEmpty()) {
            item { Text("Community Cloud Studies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(publicStudiesState.data, key = { it.id }) { st ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onImportStudyPgn(st.pgn) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(st.title, fontWeight = FontWeight.SemiBold)
                        Text("by ${st.ownerName} • ${st.description}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchAndBroadcastsTab(
    isOnline: Boolean,
    liveWatchState: UiState<List<OnlineGameDoc>>,
    broadcasts: List<ExternalBroadcastItem>,
    tvChannels: List<ExternalTvChannel>,
    onSpectateGame: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenNewsReader: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Live Watch & Broadcasts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onOpenNewsReader) {
                        Icon(Icons.Default.Newspaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("News")
                    }
                    OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                }
            }
        }
        if (isOnline && liveWatchState is UiState.Success && liveWatchState.data.isNotEmpty()) {
            item { Text("Live ChessHub Games", style = MaterialTheme.typography.labelLarge) }
            items(liveWatchState.data, key = { it.id }) { g ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSpectateGame(g.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${g.whiteName} vs ${g.blackName}", fontWeight = FontWeight.SemiBold)
                        Text("${g.timeControl} • Watch", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (tvChannels.isNotEmpty()) {
            item { Text("External Lichess TV Channels (Read-Only)", style = MaterialTheme.typography.labelLarge) }
            items(tvChannels.take(6)) { ch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ch.channelName, fontWeight = FontWeight.SemiBold)
                        Text(ch.playerWhite, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (broadcasts.isNotEmpty()) {
            item { Text("Official Tournament Broadcasts (Lichess)", style = MaterialTheme.typography.labelLarge) }
            items(broadcasts.take(8)) { b ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(b.name, fontWeight = FontWeight.SemiBold)
                        if (b.description.isNotBlank()) {
                            Text(b.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialAndLeaderboardTab(
    isOnline: Boolean,
    currentUserId: String?,
    leaderboardState: UiState<List<PublicProfileDoc>>,
    playerSearchResults: List<PublicProfileDoc>,
    friendshipsState: UiState<List<FriendshipDoc>>,
    threadsState: UiState<List<MessageThreadDoc>>,
    directMessages: List<DirectMessageDoc>,
    onRequestSignIn: () -> Unit,
    onSearchPlayers: (String) -> Unit,
    onAddFriend: (String, String) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onBlockFriend: (String) -> Unit,
    onChallengeFriend: (String, String, TimeControl, ChessVariant) -> Unit,
    onOpenMessageThread: (String) -> Unit,
    onSendDirectMessage: (String, String) -> Unit,
    onOpenReport: (String, String) -> Unit,
) {
    var playerQuery by remember { mutableStateOf("") }
    var countryFilter by remember { mutableStateOf("") }
    var activeChatUid by remember { mutableStateOf<String?>(null) }
    var activeChatName by remember { mutableStateOf("") }
    var dmDraft by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isOnline || currentUserId == null) {
            item { SignInRequiredNotice("Global Leaderboards, Player Search & Messages", onRequestSignIn) }
            return@LazyColumn
        }

        // Global Player Search
        item {
            Text("Global Player Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = playerQuery,
                    onValueChange = { playerQuery = it },
                    label = { Text("Search ChessHub username") },
                    modifier = Modifier.weight(1f).testTag("player_search_input"),
                    singleLine = true,
                )
                Button(onClick = { onSearchPlayers(playerQuery) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        }
        if (playerSearchResults.isNotEmpty()) {
            items(playerSearchResults, key = { "search_${it.userId}" }) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${CountryCatalog.flagForIso(p.countryIso)} ${p.username} (${p.fideFederation})", fontWeight = FontWeight.SemiBold)
                            Text("Blitz ${p.blitzRating} • Rapid ${p.rapidRating} • Bullet ${p.bulletRating}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (p.userId != currentUserId) {
                            OutlinedButton(onClick = { onAddFriend(p.userId, p.username) }) { Text("Add") }
                            Spacer(Modifier.width(4.dp))
                            FilledTonalButton(onClick = { onChallengeFriend(p.userId, p.username, TimeControl.PRESETS[3], ChessVariant.STANDARD) }) { Text("Play") }
                        }
                    }
                }
            }
        }

        item {
            Text("Friends & Challenges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (friendshipsState is UiState.Success) {
            if (friendshipsState.data.isEmpty()) {
                item { Text("Add friends from Search or the Leaderboard below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(friendshipsState.data, key = { it.id }) { fr ->
                val isRequester = fr.requesterUid == currentUserId
                val peerUid = if (isRequester) fr.recipientUid else fr.requesterUid
                val peerName = if (isRequester) fr.recipientName else fr.requesterName
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(peerName, fontWeight = FontWeight.SemiBold)
                            Text("Status: ${fr.status}", style = MaterialTheme.typography.labelSmall)
                        }
                        if (fr.status == "pending" && !isRequester) {
                            FilledTonalButton(onClick = { onAcceptFriend(fr.id) }) { Text("Accept") }
                        } else if (fr.status == "accepted") {
                            FilledTonalButton(onClick = {
                                onChallengeFriend(peerUid, peerName, TimeControl.PRESETS[3], ChessVariant.STANDARD)
                            }) { Text("Play") }
                            Spacer(Modifier.width(4.dp))
                            OutlinedButton(onClick = {
                                activeChatUid = peerUid
                                activeChatName = peerName
                                val tid = if (currentUserId < peerUid) "${currentUserId}_$peerUid" else "${peerUid}_$currentUserId"
                                onOpenMessageThread(tid)
                            }) { Text("DM") }
                        }
                        IconButton(onClick = { onOpenReport(peerUid, peerName) }) {
                            Icon(Icons.Default.Report, contentDescription = "Report")
                        }
                    }
                }
            }
        }

        if (activeChatUid != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Chat with $activeChatName", fontWeight = FontWeight.Bold)
                        directMessages.takeLast(8).forEach { m ->
                            Text("${m.senderName}: ${m.text}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dmDraft,
                                onValueChange = { dmDraft = it },
                                label = { Text("Direct message") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            IconButton(onClick = {
                                val target = activeChatUid
                                if (target != null && dmDraft.isNotBlank()) {
                                    onSendDirectMessage(target, dmDraft)
                                    dmDraft = ""
                                }
                            }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text("Global & Country Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = countryFilter,
                onValueChange = { countryFilter = it.uppercase().take(3) },
                label = { Text("Filter Country ISO or FIDE (e.g., BD, US, BAN)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (leaderboardState is UiState.Success) {
            val filtered = leaderboardState.data.filter { p ->
                countryFilter.isBlank() || p.countryIso.equals(countryFilter, true) || p.fideFederation.equals(countryFilter, true)
            }
            items(filtered.withIndex().toList(), key = { it.value.userId }) { (idx, player) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${idx + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${CountryCatalog.flagForIso(player.countryIso)} ${player.username} (${player.fideFederation})", fontWeight = FontWeight.SemiBold)
                            Text("Blitz ${player.blitzRating} • Rapid ${player.rapidRating} • Bullet ${player.bulletRating}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (player.userId != currentUserId) {
                            OutlinedButton(onClick = { onAddFriend(player.userId, player.username) }) { Text("Add") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminModerationTab(
    reports: List<AbuseReportDoc>,
    onResolve: (String, String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Moderation Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        if (reports.isEmpty()) {
            item { Text("No moderation reports currently in queue.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(reports, key = { it.id }) { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${rep.category} (${rep.status})", fontWeight = FontWeight.Bold)
                        Text("Target UID: ${rep.targetUid}", style = MaterialTheme.typography.labelSmall)
                        Text(rep.details, style = MaterialTheme.typography.bodySmall)
                        if (rep.status == "open") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onResolve(rep.id, "reviewed") }) { Text("Mark Reviewed") }
                                OutlinedButton(onClick = { onResolve(rep.id, "dismissed") }) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
        }
    }
}
