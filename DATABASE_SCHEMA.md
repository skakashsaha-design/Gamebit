# ChessHub Database Schema & Persistence Architecture

ChessHub separates local device persistence (**Room v2 + Jetpack DataStore**) from authenticated online server persistence (**Modular REST/HTTPS + WebSocket Backend**). Offline Mode never opens backend WebSockets or runs cloud listeners.

---

## 1. Local SQLite Database (`ChessHubDatabase`, Room `version = 2`)
Database file: `chesshub_local.db` (`com.example.data.local.ChessHubDatabase`).

### 1.1 Entities & Tables (9 Tables)

| Table Name | Entity Class | Primary Key | Indices | Purpose |
|---|---|---|---|---|
| `chess_puzzles` | `ChessPuzzleEntity` | `id: String` | `difficultyRating`, `source`, `isCheckmatePattern`, `bookmarked` | Full offline & custom puzzle collection storing FEN strings, space-separated UCI solution sequences (`solutionMoves`), Glicko-style `difficultyRating`, comma-separated `themes`, hints, explanations, `isCheckmatePattern`, `patternName`, `source` (`builtin`, `custom`, `mistake`, `lichess_daily`), `solvedCount`, `attemptCount`, `bookmarked`, `lastAttemptedAt`, `createdAt`. |
| `local_games` | `LocalGameEntity` | `id: String` | `createdAt`, `variant`, `openingEco`, `favorite` | Completed OTB, Bot, Online, and PGN-imported games with player names/ratings, result, `variant`, `timeControl`, `mode`, `openingEco`, `openingName`, `initialFen`, `finalFen`, `pgn`, `favorite`, `tags`, `notes`, `collectionId`, `whiteAccuracy`, `blackAccuracy`. |
| `local_studies` | `LocalStudyEntity` | `id: String` | `updatedAt` | Multi-chapter studies (`chaptersJson`), tags, visibility, optional `syncedCloudId`. |
| `game_collections` | `CollectionEntity` | `id: String` | `createdAt` | User-created game collections/folders. |
| `puzzle_attempts` | `PuzzleAttemptEntity` | `id: String` | `attemptedAt`, `theme` | Historical puzzle solve/fail attempts (`rated`, `daily`, `streak`, `storm`, `racer`, `mistake_drill`), rating, and elapsed time. |
| `custom_puzzles` | `CustomPuzzleEntity` | `id: String` | `createdAt` | Legacy v1 & quick-saved user/mistake puzzles (`fen`, `solutionUci`, `theme`, `rating`, `sourceGameId`). |
| `lesson_progress` | `LessonProgressEntity` | `lessonId: String` | — | Per-lesson step completion (`completedSteps`, `totalSteps`, `completed`, `downloadedOffline`). |
| `music_tracks` | `MusicTrackEntity` | `id: String` | `addedAt` | SAF device audio tracks (`content://` URIs via `OpenMultipleDocuments`) and built-in Focus Synth tracks (`isBuiltIn`), `playlistName`, `durationMs`. |
| `cached_news` | `CachedNewsEntity` | `id: String` | `fetchedAt` | Offline-cached official Lichess Atom/Blog news items with `title`, `summary`, `url`, `sourceName`, `publishedAt`, `category`, `fetchedAt`. |

### 1.2 DAO Interfaces

#### `ChessPuzzleDao` (`com.example.data.local.ChessPuzzleDao`)
- Reactive queries (`Flow`): `observeAllPuzzles()`, `observeByDifficultyRange(minRating, maxRating)`, `observeByTheme(theme)`, `observeCheckmatePatterns()`, `observeBookmarkedPuzzles()`, `observeBySource(source)`
- One-shot reads: `getAllPuzzles()`, `getPuzzleById(id)`, `countPuzzles()`
- Writes & updates: `upsertPuzzle(puzzle)`, `insertAllIgnoreConflict(puzzles)`, `updatePuzzle(puzzle)`, `setBookmarked(id, bookmarked)`, `recordPuzzleAttemptStats(id, solved, timestamp)`
- Cleanup: `deletePuzzleById(id)`, `clearCustomPuzzles()`

#### `ChessHubDao` (`com.example.data.local.ChessHubDao`)
- Manages `local_games`, `local_studies`, `game_collections`, `puzzle_attempts`, `custom_puzzles`, and `lesson_progress`.

#### `MediaAndNewsDao` (`com.example.data.local.MediaAndNewsDao`)
- Manages `music_tracks` (`observeMusicTracks()`, `upsertMusicTrack()`, `deleteMusicTrack()`) and `cached_news` (`observeCachedNews()`, `insertAllNews()`, `clearCachedNews()`).

### 1.3 Safe Database Migration (`MIGRATION_1_2`)
`ChessHubDatabase.MIGRATION_1_2` upgrades existing v1 installations non-destructively:
1. Creates `chess_puzzles` and its 4 indices (`difficultyRating`, `source`, `isCheckmatePattern`, `bookmarked`).
2. Creates `music_tracks` (`isBuiltIn` default `0`) and `index_music_tracks_addedAt`.
3. Creates `cached_news` and `index_cached_news_fetchedAt`.
4. Backfills all existing rows from v1 `custom_puzzles` into `chess_puzzles` (`INSERT OR IGNORE ... SELECT ... FROM custom_puzzles` with `source = 'custom'`).

---

## 2. Preferences DataStore (`chesshub_prefs` via `SettingsRepository`)
Persists 40+ typed preference keys (`AppSettings`) without SQLite overhead:
- **Appearance & Board**: `dark_theme`, `high_contrast`, `dynamic_color`, `board_theme` (6 themes), `piece_style` (`classic`, `modern`, `minimal`, `wooden`, `high_contrast`, `3d`), `show_coords`, `show_legal`, `show_last_move`, `move_animation`, `reduced_animations`, `animation_speed`, `font_scale`, `board_scale`, `language`.
- **Gameplay & Controls**: `auto_queen`, `confirm_moves`, `premove`, `streak_penalty`, `coord_notation`.
- **Audio, Music & Voice**: `sound`, `sound_vol`, 8 independent SFX category flags (`sfx_move`, `sfx_capture`, `sfx_check`, `sfx_mate`, `sfx_promo`, `sfx_feedback`, `sfx_button`, `sfx_book`), `haptics`, `bot_voice`, `trainer_voice`, `voice_vol`, `music_enabled`, `music_vol`, `music_menu`, `music_game`, `music_train`, `low_mem_audio`.
- **Engine & Recognition**: `eval_bar`, `threat_arrow`, `best_move_arrow`, `board_arrows`, `engine_depth` (1–5), `engine_multipv` (1–3), `engine_threads`, `engine_hash_mb`, `opening_book`, `opening_recog`, `gambit_recog`, `endgame_recog`.
- **Performance, Profile & Home Layout**: `low_memory` (auto-detected via `ActivityManager.isLowRamDevice` or `<= 1.4 GB` total RAM), `social_feed`, `allow_chat`, `notifications_enabled`, `country_iso`, `fide_fed`, `local_avatar`, `local_name`, `local_bio`, `hidden_cards`, `card_order`, `puzzle_rating`, `puzzle_rd`, `best_streak`, `best_storm`.

---

## 3. Auth Session DataStore & Online Backend Contract (Authenticated Online Mode)
### 3.1 Auth Session Store (`chesshub_auth_session` via `HttpAuthRepository`)
Stores only the authenticated bearer token, configurable server base URL, `userId`, `username`, and `email` (never stores raw passwords).

### 3.2 REST/HTTPS + WebSocket Server-Authoritative Models (`ChessHubBackendModels.kt`)
All online entities are transported via Retrofit REST (`/v1/...`) and real-time OkHttp WebSockets (`wss://.../v1/ws`):
- **Accounts & Profiles**: Private user account metadata + searchable `PublicProfileDto` (`bulletRating`, `blitzRating`, `rapidRating`, `classicalRating`, `puzzleRating`, W/D/L counters, `countryIso`, `fideFederation`, `onlineStatus`, `lastSeenAtMs`) and `RatingHistoryPointDto` snapshots.
- **Matchmaking (`MatchTicketDto`)**: Variant, time control, and rated queue tickets with expanding rating bands and WebSocket match notifications.
- **Server-Authoritative Games (`OnlineGameDto`, `GameChatMessageDto`)**: Server-validated move transitions (`fen`, `pgn`, `movesUci`), authoritative white/black clocks (`whiteRemainingMs`, `blackRemainingMs`, `serverTimestampMs`), connection/presence indicators (`whiteConnected`, `blackConnected`), draw/takeback offers, server-calculated rating deltas, reconnection support, and participant chat.
- **Social & Organizations**: `FriendshipDto`, `DirectMessageDto`, `CloudStudyDto`, `ClubDto`, `TeamDto`, `TournamentDto`, and `AbuseReportRequest`.
