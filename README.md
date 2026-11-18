# ChessHub (`v2.0.0`)

**ChessHub** is an independent, offline-first Android chess ecosystem built with Kotlin, Jetpack Compose, Material 3, Room v2, DataStore, and Firebase Cloud Firestore.

> ChessHub is an independent chess platform and is not affiliated with or endorsed by Lichess or Chess.com.

---

## Highlights & Features

- **Offline-First & Low-RAM Ready (`minSdk 24`, Android 8.1+ / 1 GB RAM Optimized)**:
  - Automatic low-memory detection (`ActivityManager.isLowRamDevice` / `<= 1.4 GB` RAM) that scales engine hash tables, Multi-PV count, animations, and audio buffers cleanly.
- **Complete Chess Rules + 9 Variants**:
  - Standard FIDE Chess, Chess960 (Scharnagl `0..959`), Three-Check, King of the Hill, Antichess, Atomic, Horde, Racing Kings, and Crazyhouse (with drop pockets).
  - Legal moves, castling, en passant, 4-piece promotion/underpromotion, 3/5-fold repetition, 50/75-move rule, insufficient material, resignation, draw agreement, and premoves.
- **Play & 18 Distinct Bots (~250–3000 Approx. Strength) + Bot vs Bot**:
  - Pass & Play (Over the Board), Human vs Bot, Bot vs Bot spectator battles, Fischer/Bronstein/Simple Delay/Correspondence/Unlimited clocks (`1+0`, `2+1`, `3+0`, `3+2`, `5+0`, `5+3`, `10+0`, `10+5`, `15+10`, `30+0`, custom).
  - Personality-driven bot dialogue with optional Android `TextToSpeech` Bot & Trainer voices (independently toggleable).
- **Board Themes, Piece Styles & Annotations**:
  - 6 board themes (`Emerald Slate`, `Classic Walnut`, `Tournament Green`, `Midnight Blue`, `High Contrast`, `Colorblind Safe`) + 6 piece styles (`Classic Staunton`, `Modern`, `Minimal`, `Wooden`, `High Contrast`, `3D Bevel`).
  - Tap-to-move, drag-and-drop, move animations with speed slider, user arrows & square circles, engine threat/best-move arrows, and crazyhouse drop bar.
- **Analysis, Game Review, UCI Console & Explore**:
  - Pure-Kotlin Multi-PV Alpha-Beta/Quiescence engine (`ChessHub Kotlin UCI Engine 2.0`) with interactive UCI command console (`uci`, `isready`, `position`, `go`, `stop`, `setoption`, `quit`), evaluation bar, interactive evaluation graph, accuracy breakdown, missed wins/mates, and 10-point *"Why Was This a Mistake?"* diagnostics.
  - ECO opening recognition, 12 named gambits (Accepted/Declined/Offered), Endgame Classifier, interactive Board Editor (including *Save as Puzzle*), multi-game RAV PGN import/export, and Master Games Explore hub.
- **Room v2 Puzzle Collection, 20 Checkmate Patterns & Training Suite**:
  - `ChessPuzzleEntity` + `ChessPuzzleDao` + `MIGRATION_1_2` storing FEN strings, multi-move UCI solution sequences, difficulty ratings, themes, bookmarks, and solve/attempt stats.
  - 20 named Checkmate Patterns trainer, Daily Puzzle (Lichess API + offline fallback), Rated Glicko-2 Tactics, Streak, Storm, Racer, My Mistakes/Custom puzzles, 12 Opening Drills, Lessons, Endgames, and 30s Coordinate Trainer.
- **Focus Music Player & Chess News Reader**:
  - Built-in ambient focus chord loops (`AudioTrack` PCM synth) + zero-permission device audio loading via Android Storage Access Framework (`OpenMultipleDocuments`) with shuffle, repeat, seek, and Menu/Game/Training focus toggles.
  - 8 independent SFX category toggles (Move, Capture, Check, Checkmate, Promotion, Feedback, Button, Book Move) + haptics.
  - Live Lichess Official Blog Atom/RSS News Reader backed by Room offline cache (`cached_news`).
- **ChessHub Online (Google Sign-In + Cloud Firestore)**:
  - Credential Manager Google Sign-In gating online matchmaking (Bullet/Blitz/Rapid/Classical/Variants), Glicko-2 rated & casual matches, live move/clock sync, in-game & direct chat, global player search, friends, clubs, teams, Arena/Swiss/Simul tournaments, cloud studies, leaderboards, and abuse reporting under zero-trust `firestore.rules`.
  - Read-only official public API lookups for **Lichess** and **Chess.com** player profiles, public game PGNs, Opening Explorer, 7-piece Syzygy Tablebase, and TV/Broadcasts.
- **40+ Settings, Avatars, Badges & Localization**:
  - Customizable player avatars, badges/titles/achievements, ISO-3166 countries + FIDE federations, and full **English** & **বাংলা (Bengali)** (`values-bn`) localization.

---

## Build, Test & Universal APK Instructions

> Use `gradle` directly (the project uses Gradle `9.3.1` via environment tooling; do not invoke `./gradlew` or create `local.properties`).

```bash
# Copy Secrets placeholder file if missing
cp -n .env.example .env

# Run all Unit & Robolectric tests (rules, engine, UCI, Room v1->v2 migration, UI)
gradle :app:testDebugUnitTest

# Run Android Lint checks
gradle :app:lintDebug

# Build Universal Debug APK (single APK supporting all ABIs, no splits)
gradle :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build Signed Universal Release APK (requires upload keystore env vars)
KEYSTORE_PATH=/path/to/my-upload-key.jks \
STORE_PASSWORD=... \
KEY_PASSWORD=... \
gradle :app:assembleRelease

# Run Firestore Security Rules tests against local Firestore emulator
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

## CI/CD Universal APK Workflow
See `.github/workflows/chesshub-universal-apk.yml` for the automated GitHub Actions pipeline (JDK 21, Android SDK 36, Gradle 9.3.1, unit/Robolectric tests, Android Lint, Universal Debug APK + optional signed Universal Release APK, SHA-256 checksums, and build report artifacts).

## Documentation
- `ARCHITECTURE.md` — Layer & module architecture
- `DATABASE_SCHEMA.md` — Room v2 entities/DAOs/migrations, DataStore keys & Firestore collections
- `FEATURE_STATUS.md` — Complete feature & verification matrix
- `TESTING_REPORT.md` — Executed test, lint & Universal APK build results
- `THIRD_PARTY_NOTICES.md` — Open-source licenses & public API attributions
