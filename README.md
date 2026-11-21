<div align="center">

# ♟️ Gamebit (`v2.0.0`)

**Offline-first Android chess application built with Kotlin, Jetpack Compose, Material 3, Room v2, and a modular REST + WebSocket backend.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Android](https://img.shields.io/badge/Android-minSdk_24_%7C_targetSdk_36-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Room DB](https://img.shields.io/badge/Room-v2_Migration-0F9D58?style=for-the-badge)](ARCHITECTURE.md)
[![CI](https://img.shields.io/badge/Gamebit_APK_CI-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](.github/workflows/gamebit-universal-apk.yml)

> *Gamebit is an independent chess platform and is not affiliated with or endorsed by Lichess or Chess.com.*

</div>

---

## ✨ Feature Highlights

<table>
<tr>
<td width="50%" valign="top">

### 🎮 Play, Bots & Variants
- **Clamped Interactive Chessboard** — Tap-to-move or drag-and-drop with board-boundary clamping (off-board drops cancel cleanly), smooth Compose piece sliding animations, `6` board themes, and `6` Staunton piece styles.
- **Pure-Kotlin Engine & 18 Bots** — `LocalChessEngine` move validation + 6 AI difficulty tiers (~450–2650), 18 distinct bot personalities (~250–3000), and live **Bot vs Bot** spectator mode.
- **9 Complete Chess Variants** — Standard FIDE, **Chess960** (`0..959`), Three-Check, King of the Hill, Antichess, Atomic, Horde, Racing Kings, and Crazyhouse (with drop pocket).
- **Tournament Clocks & History** — Fischer, Bronstein, Simple Delay, Correspondence, and Unlimited controls with remaining-time bars, `<30s` amber & `<20s` red tenths alerts, scrollable ply review (`|<`, `<`, `>`, `>|`), and full PGN Import/Export/Share.

</td>
<td width="50%" valign="top">

### 📊 Deep Analysis & Training
- **Interactive Evaluation Graph & Bar** — Full-width smooth centipawn/mate (`M1`, `M2`, …) analysis curve with `1x / 2x / 4x` zoom, vertical move cursor, tap-to-jump sync across board/movelist, and phase accuracy breakdown.
- **11 Move Classifications & Explanations** — `Book`, `Forced`, `Brilliant`, `Great`, `Best`, `Good`, `Inaccuracy`, `Mistake`, `Miss`, `Blunder`, and *"Why Was This a Mistake?"* tactical diagnostics plus a **Post-Game Comparison Dialog**.
- **Puzzles, Tactics & Endgames Coach** — Rated Glicko-2 puzzles (Room v2 `MIGRATION_1_2`), Daily, Streak, Storm, Racer, custom puzzle builder, 20 checkmate patterns, 12 gambits, 7 opening drills, coordinate trainer, and interactive **Endgame Trainer** with real-time coach advice.
- **Audio, Localization & Hub** — Built-in PCM ambient Focus Music (`AudioTrack`) + local audio picker, TTS move/coach voiceovers, Low-RAM `1GB` optimization mode, **English & বাংলা**, plus modular REST/WebSocket online play and Lichess/Chess.com lookups.

</td>
</tr>
</table>

---

## 🏗️ Architecture & Stack

| Layer | Implementation |
|---|---|
| **UI & Design System** | Jetpack Compose, Material 3 (Dynamic Color, Edge-to-Edge, Adaptive Window Size Classes), custom Canvas chessboard & evaluation charts |
| **State & Concurrency** | MVVM (`ViewModel`, `StateFlow`, `collectAsStateWithLifecycle`), Kotlin Coroutines & Flows, Type-safe Navigation |
| **Chess Core** | Pure-Kotlin `MoveGenerator`, `LocalChessEngine`, `ChessEngine` (Alpha-Beta, Quiescence, Transposition Table), `UciChessEngine`, `EndgameClassifier`, `PgnParser`, `Glicko2RatingSystem` |
| **Local Storage** | Room v2 (`9` entities, `3` DAOs, verified `MIGRATION_1_2`), Jetpack Preferences DataStore (`40+` settings) |
| **Modular Networking** | Configurable REST/HTTPS (`Retrofit` + `Moshi`) & real-time WebSocket (`OkHttp`) backend client + Google Sign-In (`CredentialManager`) + read-only Lichess/Chess.com APIs (zero Firebase) |
| **Low-End Optimization** | Automatic & manual `1GB RAM` scaling for search depth, Multi-PV, animation modes (`Full` / `Reduced` / `Off`), and audio buffers (`minSdk 24`) |

---

## 🚀 Quick Start (Local Build & Tests)

> **Build Tooling:** Use `gradle` (`9.3.1`) directly. Do **not** use `./gradlew` or create a `local.properties` file.

```bash
# 1. Initialize secrets placeholder file (optional, ignored by Git)
cp -n .env.example .env

# 2. Run Unit + Robolectric UI & Room Migration Tests
gradle :app:testDebugUnitTest --no-daemon

# 3. Run Android Lint
gradle :app:lintDebug --no-daemon

# 4. Build Universal Debug APK (all ABIs included)
gradle :app:assembleDebug --no-daemon
# Output -> app/build/outputs/apk/debug/app-debug.apk
```

<details>
<summary>Signed Release APK (Optional)</summary>

```bash
KEYSTORE_PATH=/path/to/my-upload-key.jks \
STORE_PASSWORD=your_store_password \
KEY_PASSWORD=your_key_password \
gradle :app:assembleRelease --no-daemon
```

</details>

---

## 🤖 GitHub Actions CI/CD

Workflow: [`.github/workflows/gamebit-universal-apk.yml`](.github/workflows/gamebit-universal-apk.yml)

| Job | Pipeline Steps | Fresh Artifact Naming |
|---|---|---|
| **`Build Fresh Universal Debug APK`** | JDK 21 · Android SDK 36 · Gradle `9.3.1` (`--no-build-cache --rerun-tasks`) · `$GITHUB_WORKSPACE/debug.keystore` · `:app:clean :app:assembleDebug` · `apksigner verify` · SHA-256 Build Summary | `Gamebit-Debug-APK-run<N>-attempt<A>` (`Gamebit-v2.0.0-run<N>-a<A>-<sha>.apk`) |
| **`Unit + Robolectric Tests + Lint`** | JDK 21 · Android SDK 36 · `:app:testDebugUnitTest` (Robolectric SDK 36 + Compose + Room) · `:app:lintDebug` | `Gamebit-Reports-run<N>-attempt<A>` |

> **Why every APK build is guaranteed fresh:** Every CI run wipes `app/build/outputs/apk`, runs `:app:clean :app:assembleDebug` with `--no-build-cache --rerun-tasks`, stamps a unique `versionCode` / `versionName` (`2.0.0-r<run>.<attempt>-<sha>`), and uploads a unique artifact per run and attempt.

---

## 📌 Technical Notes

- **Engine:** Gamebit runs a built-in pure-Kotlin alpha-beta + quiescence engine (`UciChessEngine` / `ChessEngine`) without bundling native C++ binaries, ensuring broad universal ABI compatibility.
- **No Firebase:** All local data runs offline via Room + DataStore, and online multiplayer/sync uses a modular backend-agnostic REST + WebSocket client.

---

## 📚 Project Docs

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — Module & layer design
- [`DATABASE_SCHEMA.md`](DATABASE_SCHEMA.md) — Room v2 entities, DAOs & DataStore schema
- [`FEATURE_STATUS.md`](FEATURE_STATUS.md) — Implemented features inventory
- [`TESTING_REPORT.md`](TESTING_REPORT.md) — Unit, Robolectric & APK verification summary
- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) — Open-source licenses & public APIs
