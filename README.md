# ChessHub

**ChessHub** is an independent, offline-first Android chess application built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, and Firebase Cloud Firestore.

> ChessHub is an independent chess platform and is not affiliated with or endorsed by Lichess or Chess.com.

## Highlights
- **Offline-First Core**: Play Pass & Play (Over the Board) or against 7 local bots (~250–2400 bot strength labels) with zero internet dependency.
- **9 Chess Variants**: Standard, Chess960 (Scharnagl 0–959), Three-Check, King of the Hill, Antichess, Atomic, Horde, Racing Kings, and Crazyhouse (with drop pockets).
- **Analysis Board & Game Review**: Multi-PV local alpha-beta/quiescence engine, evaluation bar, interactive evaluation graph, opening recognition (ECO), deterministic Chess Coach explanations, full variation trees with comments & NAG annotations, and PGN/FEN import/export/share.
- **Training Suite**: Puzzles (Daily via official Lichess public API with offline fallback, Rated Glicko-2, Streak, Storm, Racer, From My Mistakes, 16 themes, and stats dashboard), Beginner/Intermediate/Advanced interactive lessons, Endgame Trainer, and 30s Coordinate Trainer.
- **ChessHub Online (Google Sign-In + Cloud Firestore)**: Real-time matchmaking pool, live games with game chat, Glicko-2 rating updates, friends & direct messaging, clubs, teams, Arena/Swiss/Simul tournaments, global/country leaderboards, cloud studies, and moderation reporting.
- **External Read-Only Integrations**: Official public Lichess & Chess.com endpoints for Daily Puzzle, Opening Explorer (Masters & Lichess), 7-piece Syzygy Tablebase, Broadcasts/TV, and player rating/PGN import.
- **Localization & Low-Memory Support**: Full English and বাংলা (Bengali) localization, Country + separate FIDE Federation selector with flags, high-contrast accessibility, and Low-Memory mode tailored for Android 8.1+ / low-RAM devices.

## Build & Test Commands
```bash
# Run local JVM unit tests & Robolectric UI tests
gradle :app:testDebugUnitTest

# Build Universal Debug APK
gradle :app:assembleDebug

# Run Firestore Security Rules tests against local emulator
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

## CI/CD Universal APK
See `.github/workflows/build-universal-apk.yml` for automated test, lint, and universal APK builds (`ChessHub-universal-debug.apk` and optional signed `ChessHub-universal-release.apk`).
