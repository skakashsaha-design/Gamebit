# Third-Party Notices & License Audit

ChessHub is an independent Android chess application. Every library, font, and external API used in this project has been audited for license compatibility:

## Android & Kotlin Libraries
| Component | License | Usage |
|---|---|---|
| AndroidX Core, Lifecycle, Activity Compose, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore | Apache License 2.0 | UI, local SQLite & preference persistence, navigation |
| Kotlin Standard Library & Kotlinx Coroutines | Apache License 2.0 | Language runtime and asynchronous engine/network flows |
| OkHttp, Retrofit, Moshi | Apache License 2.0 | ChessHub REST/HTTPS & WebSocket client plus read-only public Lichess / Chess.com HTTP API access |
| Robolectric, JUnit 4 | MIT / Eclipse Public License 1.0 | Automated unit and UI verification tests |

## Bundled Fonts (`/app/src/main/res/font/`)
| Font | License |
|---|---|
| Space Grotesk | SIL Open Font License 1.1 |
| Plus Jakarta Sans | SIL Open Font License 1.1 |
| JetBrains Mono | SIL Open Font License 1.1 |

## External Public Endpoints (Runtime Read-Only)
- **Lichess Public API** (`lichess.org/api`, `explorer.lichess.ovh`, `tablebase.lichess.ovh`): Used read-only in accordance with public API guidelines with descriptive User-Agent.
- **Chess.com Public Data API** (`api.chess.com/pub`): Used read-only for user-requested public profile/archive lookup.
- **No proprietary Stockfish GPL binary or private scraping is bundled.**

Full license texts are stored in `THIRD_PARTY_LICENSES/`.
