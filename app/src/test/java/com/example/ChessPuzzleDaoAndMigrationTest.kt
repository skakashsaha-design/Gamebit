package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedNewsEntity
import com.example.data.local.ChessHubDatabase
import com.example.data.local.ChessPuzzleEntity
import com.example.data.local.LocalChessRepository
import com.example.data.local.MusicTrackEntity
import com.example.data.local.PuzzlesAndLessonsCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChessPuzzleDaoAndMigrationTest {

    private lateinit var context: Context
    private lateinit var db: ChessHubDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ChessHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `chess puzzle dao supports insert, difficulty range, theme, checkmate patterns, bookmarks, stats and delete`() = runTest {
        val puzzleDao = db.chessPuzzleDao()
        val repo = LocalChessRepository(
            dao = db.chessHubDao(),
            puzzleDao = puzzleDao,
            mediaDao = db.mediaAndNewsDao(),
        )

        repo.seedBuiltInPuzzlesIfNeeded()
        val count = puzzleDao.countPuzzles()
        assertEquals(PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.size, count)

        val checkmates = puzzleDao.observeCheckmatePatterns().first()
        assertTrue(checkmates.size >= 20)
        assertTrue(checkmates.all { it.isCheckmatePattern })

        val midRange = puzzleDao.observeByDifficultyRange(1000, 1400).first()
        assertTrue(midRange.isNotEmpty())
        assertTrue(midRange.all { it.difficultyRating in 1000..1400 })

        val forks = puzzleDao.observeByTheme("Fork").first()
        assertTrue(forks.isNotEmpty())

        val custom = ChessPuzzleEntity(
            id = "custom_test_1",
            title = "Back Rank Tactic",
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionMoves = "d1d8",
            difficultyRating = 920,
            themes = "Back Rank Mate,Mate in 1",
            hint = "Use the open d-file.",
            explanation = "Rd8# checkmates on the back rank.",
            source = "custom",
        )
        puzzleDao.upsertPuzzle(custom)
        assertNotNull(puzzleDao.getPuzzleById("custom_test_1"))
        assertEquals(listOf("d1d8"), puzzleDao.getPuzzleById("custom_test_1")?.solutionMoveList())

        puzzleDao.setBookmarked("custom_test_1", true)
        val bookmarked = puzzleDao.observeBookmarkedPuzzles().first()
        assertTrue(bookmarked.any { it.id == "custom_test_1" })

        puzzleDao.recordPuzzleAttemptStats("custom_test_1", solved = true, timestamp = 123456L)
        puzzleDao.recordPuzzleAttemptStats("custom_test_1", solved = false, timestamp = 123999L)
        val updated = puzzleDao.getPuzzleById("custom_test_1")!!
        assertEquals(2, updated.attemptCount)
        assertEquals(1, updated.solvedCount)
        assertEquals(123999L, updated.lastAttemptedAt)

        puzzleDao.deletePuzzleById("custom_test_1")
        assertNull(puzzleDao.getPuzzleById("custom_test_1"))
    }

    @Test
    fun `media and news dao persists built-in and user music tracks and cached news`() = runTest {
        val mediaDao = db.mediaAndNewsDao()
        mediaDao.upsertMusicTrack(
            MusicTrackEntity(
                id = "track_1",
                title = "Nocturne",
                artist = "Local File",
                uriString = "content://media/external/audio/1",
                playlistName = "Study",
                durationMs = 120_000L,
                isBuiltIn = false,
            ),
        )
        val tracks = mediaDao.observeMusicTracks().first()
        assertEquals(1, tracks.size)
        assertFalse(tracks.first().isBuiltIn)

        mediaDao.insertAllNews(
            listOf(
                CachedNewsEntity(
                    id = "news_1",
                    title = "Candidates Tournament Update",
                    summary = "Round 7 decisive games.",
                    url = "https://lichess.org/blog/test",
                    sourceName = "Lichess",
                    publishedAt = "2026-01-01",
                ),
            ),
        )
        assertEquals(1, mediaDao.observeCachedNews().first().size)
        mediaDao.clearCachedNews()
        assertTrue(mediaDao.observeCachedNews().first().isEmpty())
    }

    @Test
    fun `migration 1 to 2 preserves v1 custom puzzles and initializes new tables`() = runTest {
        val dbName = "migration_test_v1_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(sqLite: SQLiteDatabase) {
                sqLite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_games` (
                        `id` TEXT NOT NULL,
                        `whiteName` TEXT NOT NULL,
                        `blackName` TEXT NOT NULL,
                        `whiteRating` INTEGER NOT NULL,
                        `blackRating` INTEGER NOT NULL,
                        `result` TEXT NOT NULL,
                        `resultReason` TEXT NOT NULL,
                        `variant` TEXT NOT NULL,
                        `timeControl` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `openingEco` TEXT NOT NULL,
                        `openingName` TEXT NOT NULL,
                        `initialFen` TEXT NOT NULL,
                        `finalFen` TEXT NOT NULL,
                        `pgn` TEXT NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        `tags` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `collectionId` TEXT NOT NULL,
                        `whiteAccuracy` INTEGER,
                        `blackAccuracy` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_local_games_createdAt` ON `local_games` (`createdAt`)")
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_local_games_variant` ON `local_games` (`variant`)")
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_local_games_openingEco` ON `local_games` (`openingEco`)")
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_local_games_favorite` ON `local_games` (`favorite`)")

                sqLite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_studies` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `chaptersJson` TEXT NOT NULL,
                        `publicVisible` INTEGER NOT NULL,
                        `syncedCloudId` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_local_studies_updatedAt` ON `local_studies` (`updatedAt`)")

                sqLite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `game_collections` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_game_collections_createdAt` ON `game_collections` (`createdAt`)")

                sqLite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `puzzle_attempts` (
                        `id` TEXT NOT NULL,
                        `puzzleId` TEXT NOT NULL,
                        `theme` TEXT NOT NULL,
                        `puzzleRating` INTEGER NOT NULL,
                        `solved` INTEGER NOT NULL,
                        `timeTakenMs` INTEGER NOT NULL,
                        `mode` TEXT NOT NULL,
                        `attemptedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_puzzle_attempts_attemptedAt` ON `puzzle_attempts` (`attemptedAt`)")
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_puzzle_attempts_theme` ON `puzzle_attempts` (`theme`)")

                sqLite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_puzzles` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `fen` TEXT NOT NULL,
                        `solutionUci` TEXT NOT NULL,
                        `theme` TEXT NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `sourceGameId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                sqLite.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_puzzles_createdAt` ON `custom_puzzles` (`createdAt`)")

                sqLite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lesson_progress` (
                        `lessonId` TEXT NOT NULL,
                        `completedSteps` INTEGER NOT NULL,
                        `totalSteps` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `downloadedOffline` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`lessonId`)
                    )
                    """.trimIndent(),
                )

                sqLite.execSQL(
                    """
                    INSERT INTO `custom_puzzles` (`id`, `title`, `fen`, `solutionUci`, `theme`, `rating`, `sourceGameId`, `createdAt`)
                    VALUES ('v1_puzzle_1', 'Migrated Tactic', '6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1', 'd1d8', 'Fork', 1250, NULL, 9999)
                    """.trimIndent(),
                )
            }

            override fun onUpgrade(sqLite: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, ChessHubDatabase::class.java, dbName)
            .addMigrations(ChessHubDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            val migratedPuzzle = migratedDb.chessPuzzleDao().getPuzzleById("v1_puzzle_1")
            assertNotNull(migratedPuzzle)
            assertEquals("Migrated Tactic", migratedPuzzle!!.title)
            assertEquals("d1d8", migratedPuzzle.solutionMoves)
            assertEquals(1250, migratedPuzzle.difficultyRating)
            assertEquals("custom", migratedPuzzle.source)

            // Verify new music_tracks table accepts inserts including isBuiltIn column
            migratedDb.mediaAndNewsDao().upsertMusicTrack(
                MusicTrackEntity(
                    id = "m_1",
                    title = "Focus",
                    uriString = "content://test/1",
                    isBuiltIn = false,
                ),
            )
            assertEquals(1, migratedDb.mediaAndNewsDao().observeMusicTracks().first().size)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }
}
