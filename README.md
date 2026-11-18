<div align="center">

# ♟️ ChessHub (`v2.0.0`)

**Offline-first Android chess platform built with Kotlin, Jetpack Compose, Material 3, Room v2, and Firebase.**

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk_24_%7C_targetSdk_36-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Room DB](https://img.shields.io/badge/Room-v2_Migration-0F9D58?style=for-the-badge)
![CI](https://img.shields.io/badge/Universal_APK-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

> *ChessHub is an independent chess ecosystem and is not affiliated with or endorsed by Lichess or Chess.com.*

</div>

---

## ✨ What's Inside

<table>
<tr>
<td width="50%" valign="top">

### 🎮 Play & Local Chess Engine
- **`LocalChessEngine` Interface & Difficulty Presets** — Unified move validation (`validateMove` / `validateUciMove`) and 6 AI difficulty tiers (**Beginner ~450** to **Master ~2650**) via `KotlinChessEngineAdapter`.
- **18 Bot Personalities + Bot vs Bot** — Play against distinct styles (~250–3000 approx. Elo) or spectate live Bot vs Bot battles with pause & step controls.
- **Scrollable Move History & PGN Support** — Non-destructive back/forth move navigation (`|<`, `<`, `>`, `>|`, tap any ply), live-review indicator, and built-in PGN Export/Copy/Share & Import dialog.
- **Tournament Clock UI** — Fischer, Bronstein, Simple Delay, Correspondence, and Unlimited clocks with remaining-time progress bars, amber (<30s) & red tenths-of-a-second (<20s) urgency mode.
- **9 Complete Variants** — Standard FIDE, **Chess960** (`0..959`), Three-Check, King of the Hill, Antichess, Atomic, Horde, Racing Kings, and Crazyhouse (with drop pocket).

</td>
<td width="50%" valign="top">

### 🧠 Analysis, Training & Media
- **Pure-Kotlin UCI Console & Multi-PV** — Interactive `ChessHub Kotlin UCI Engine 2.0` (`uci`, `isready`, `position`, `go`, `stop`, `setoption`, `quit`), evaluation bar, graph, and 10-point *"Why Was This a Mistake?"* diagnostics.
- **Room v2 Puzzle Database** — `ChessPuzzleEntity` + `ChessPuzzleDao` + verified `MIGRATION_1_2` storing FEN strings, UCI solutions, difficulty ratings, bookmarks, and attempt stats.
- **20 Checkmate Patterns & Trainers** — Rated Glicko-2 tactics, Daily Puzzle, Streak, Storm, Racer, 12 Named Gambits, 7 Opening Drills, Lessons, Endgame Trainer, and 30s Coordinate Trainer.
- **Focus Music Player & Chess News** — Built-in PCM ambient chord synth (`AudioTrack`) + Storage Access Framework local tracks, 8 independent SFX toggles, TTS coaches, and Room-cached Lichess RSS news.
- **Player Profile & Online Sync** — Hero Profile card, offline Local Game History, piece avatars, badges, 40+ settings, **EN / বাংলা (Bengali)** localization, Credential Manager Google Sign-In, Cloud Firestore matchmaking, and read-only Lichess / Chess.com lookups.

</td>
</tr>
</table>

---

## 🛠️ Tech Stack & Architecture

| Layer | Technologies |
|---|---|
| **UI & Design** | Jetpack Compose, Material 3 (Dynamic Color, Adaptive Window Size Classes, Edge-to-Edge), Custom Canvas Chessboard (`6` board themes, `6` piece styles) |
| **State & Concurrency** | MVVM (`ViewModel`, `StateFlow`, `collectAsStateWithLifecycle`), Kotlin Coroutines & Flows |
| **Chess Core** | Pure-Kotlin `MoveGenerator`, `LocalChessEngine`, `ChessEngine` (Alpha-Beta + Quiescence + TT), `UciChessEngine`, `PgnParser` (RAV), `Glicko2RatingSystem`, `OpeningBook` |
| **Local Persistence** | Room Database v2 (`9` entities, `3` DAOs, `MIGRATION_1_2`), Jetpack Preferences DataStore (`40+` settings) |
| **Cloud & APIs** | Firebase Auth (Credential Manager Google Sign-In), Cloud Firestore (zero-trust `firestore.rules`), Retrofit + OkHttp + Moshi (Lichess & Chess.com public APIs) |
| **Low-RAM Support** | Automatic `ActivityManager.isLowRamDevice` / `<= 1.4 GB` scaling for hash tables, Multi-PV, animations, and audio buffers (`minSdk 24`) |

---

## 📦 Build, Test & Universal APK

> **Environment Note:** Build directly with `gradle` (`9.3.1`). Do not invoke `./gradlew` or add `local.properties`.

```bash
# 1. Initialize Secrets placeholder config if not present
cp -n .env.example .env

# 2. Run Unit & Robolectric Tests (Rules, Local Engine, History/PGN, Room Migration, UI)
gradle :app:testDebugUnitTest --no-daemon

# 3. Run Android Lint
gradle :app:lintDebug --no-daemon

# 4. Build Universal Debug APK (single APK supporting all ABIs, no splits)
gradle :app:assembleDebug --no-daemon
# Output -> app/build/outputs/apk/debug/app-debug.apk
```

### Signed Release APK (Optional)
```bash
KEYSTORE_PATH=/path/to/my-upload-key.jks \
STORE_PASSWORD=your_store_password \
KEY_PASSWORD=your_key_password \
gradle :app:assembleRelease --no-daemon
```

---

## 🤖 GitHub Actions CI/CD

The workflow in [`.github/workflows/chesshub-universal-apk.yml`](.github/workflows/chesshub-universal-apk.yml) runs automatically on every push, pull request, or manual `workflow_dispatch` with two parallel jobs:

| Job | What It Does | Artifact Uploaded |
|---|---|---|
| **`Build Universal APK`** | Sets up JDK 21, Android SDK (`platforms;android-36.1`, `platforms;android-36`, `build-tools;36.0.0`), Gradle `9.3.1`, `.env`, and `debug.keystore`, then builds `ChessHub-universal-debug.apk` (+ signed release APK if secrets are set) with SHA-256 summary | `ChessHub-Universal-APK` |
| **`Unit Tests & Android Lint`** | Executes `:app:testDebugUnitTest` and `:app:lintDebug` with CI-tuned JVM heap limits | `ChessHub-Reports` |

To enable signed Release APKs in GitHub Actions, configure repository secrets: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` (and optionally `GOOGLE_SERVICES_JSON`).

---

## 📌 Honest Implementation Notes

- **Pure-Kotlin Engine (No C++ Stockfish Binary):** Local play, difficulty tiers, game review, and the UCI console run on the built-in pure-Kotlin engine (`ChessHub Kotlin Chess Engine 2.0`), without bundling a native Stockfish C++ executable.
- **Online & External Features:** Real-time matchmaking, chat, tournaments, and cloud studies require Google Sign-In + Firebase Firestore; external player lookups, opening explorer, tablebase, and live chess news require internet access.

---

## 📚 Documentation

| Document | Description |
|---|---|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Layer, package & module architecture |
| [`DATABASE_SCHEMA.md`](DATABASE_SCHEMA.md) | Room v2 entities, DAOs, `MIGRATION_1_2`, DataStore & Firestore schemas |
| [`FEATURE_STATUS.md`](FEATURE_STATUS.md) | Complete feature & verification matrix |
| [`TESTING_REPORT.md`](TESTING_REPORT.md) | Test suites, Android Lint & Universal APK build verification |
| [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) | Open-source licenses & public API attributions |
