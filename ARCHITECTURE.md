# ChessHub Architecture

## Layer Breakdown
1. **Pure Kotlin Chess Domain (`com.example.chess`)**
   - `ChessModels.kt`: Immutable board primitives (`Piece`, `Square`, `Move`, `ChessVariant`, `TimeControl`, `BotProfile`, `CountryCatalog`).
   - `ChessBoardState.kt`: Variant-aware FEN parser/serializer, Scharnagl Chess960 back-rank generator (`0..959`), and Board Editor validation (`validateForEditor`).
   - `MoveGenerator.kt`: FIDE + 8 variant legal move generation, SAN parser/formatter, terminal state & draw evaluation (checkmate, stalemate, 3/5-fold repetition, 50/75-move, insufficient material, variant win conditions).
   - `PgnParser.kt` & `VariationTree.kt`: Multi-game PGN import/export supporting nested RAV variations, comments `{...}`, and NAGs (`$1`–`$6`).
   - `ChessEngine.kt`: Asynchronous coroutine `Flow` Multi-PV alpha-beta + quiescence search with transposition table capped by `lowMemoryMode`, opponent threat finder, Game Review accuracy/phase/classification analyzer, and deterministic Chess Coach.
   - `OpeningBook.kt` & `Glicko2Rating.kt`: Local ECO opening book recognizer/stats and Glicko-2 rating update math.

2. **Persistence & External Data (`com.example.data`)**
   - `local/`: Room database (`ChessHubDatabase.kt`) for local games, studies, collections, puzzle attempts, custom mistake puzzles, and lesson progress; DataStore `SettingsRepository` with automatic low-RAM detection; offline `PuzzlesAndLessonsCatalog`.
   - `remote/`:
     - `ExternalChessApis.kt`: Read-only OkHttp client for official public Lichess & Chess.com APIs.
     - `FirestoreModels.kt`, `FirestoreErrorHandler.kt`, `FirestoreRepositories.kt`: Cloud Firestore repositories targeting named DB `ai-studio-android-chesshub-7c80fd1a-15f9-41cb-85ee-b807153588f0` with strict zero-trust security rules (`firestore.rules`).

3. **Authentication & Lifecycle (`com.example.auth`, `MainActivity.kt`)**
   - Dual-path startup: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` / `GetGoogleIdOption`) OR isolated Offline Mode (`OfflineGuest`).
   - Strict Auth Gating: `OnlineViewModel` and `ChessHubOnlineRepository` are instantiated exclusively inside `key(user.uid)` when `currentUser != null`. Offline Mode never instantiates Firestore or runs Firestore listeners.

4. **UI & Presentation (`com.example.ui`, `com.example.viewmodel`)**
   - MVVM with `StateFlow` + `collectAsStateWithLifecycle`.
   - Adaptive `NavigationBar` (<600dp) / `NavigationRail` (>=600dp) with full edge-to-edge `WindowInsets.safeDrawing`.
   - Custom Jetpack Compose `Canvas` chessboard (`ChessBoardView`) with tap & drag gestures, premove queue, promotion dialog, vector Staunton pieces, and eval/graph overlays.
