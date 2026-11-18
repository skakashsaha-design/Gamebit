package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "local_games",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["variant"]),
        Index(value = ["openingEco"]),
        Index(value = ["favorite"]),
    ],
)
data class LocalGameEntity(
    @PrimaryKey val id: String,
    val whiteName: String,
    val blackName: String,
    val whiteRating: Int,
    val blackRating: Int,
    val result: String, // "1-0", "0-1", "1/2-1/2", "*"
    val resultReason: String,
    val variant: String,
    val timeControl: String,
    val mode: String, // "otb", "bot", "online", "imported"
    val openingEco: String,
    val openingName: String,
    val initialFen: String,
    val finalFen: String,
    val pgn: String,
    val favorite: Boolean = false,
    val tags: String = "",
    val notes: String = "",
    val collectionId: String = "",
    val whiteAccuracy: Int? = null,
    val blackAccuracy: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "local_studies",
    indices = [Index(value = ["updatedAt"])],
)
data class LocalStudyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val tags: String,
    val chaptersJson: String, // JSON array of StudyChapterDto
    val publicVisible: Boolean = false,
    val syncedCloudId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "game_collections",
    indices = [Index(value = ["createdAt"])],
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "puzzle_attempts",
    indices = [Index(value = ["attemptedAt"]), Index(value = ["theme"])],
)
data class PuzzleAttemptEntity(
    @PrimaryKey val id: String,
    val puzzleId: String,
    val theme: String,
    val puzzleRating: Int,
    val solved: Boolean,
    val timeTakenMs: Long,
    val mode: String, // "rated", "daily", "streak", "storm", "racer", "mistake_drill"
    val attemptedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "custom_puzzles",
    indices = [Index(value = ["createdAt"])],
)
data class CustomPuzzleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fen: String,
    val solutionUci: String,
    val theme: String,
    val rating: Int,
    val sourceGameId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val completed: Boolean,
    val downloadedOffline: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface ChessHubDao {
    @Query("SELECT * FROM local_games ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentGames(limit: Int = 100): Flow<List<LocalGameEntity>>

    @Query("SELECT * FROM local_games WHERE id = :id")
    suspend fun getGameById(id: String): LocalGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: LocalGameEntity)

    @Query("DELETE FROM local_games WHERE id = :id")
    suspend fun deleteGame(id: String)

    @Query("SELECT * FROM local_studies ORDER BY updatedAt DESC")
    fun observeStudies(): Flow<List<LocalStudyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudy(study: LocalStudyEntity)

    @Query("DELETE FROM local_studies WHERE id = :id")
    suspend fun deleteStudy(id: String)

    @Query("SELECT * FROM game_collections ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollection(collection: CollectionEntity)

    @Query("DELETE FROM game_collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("SELECT * FROM puzzle_attempts ORDER BY attemptedAt DESC LIMIT 200")
    fun observePuzzleAttempts(): Flow<List<PuzzleAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzleAttempt(attempt: PuzzleAttemptEntity)

    @Query("SELECT * FROM custom_puzzles ORDER BY createdAt DESC")
    fun observeCustomPuzzles(): Flow<List<CustomPuzzleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomPuzzle(puzzle: CustomPuzzleEntity)

    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: LessonProgressEntity)

    @Query("DELETE FROM local_games")
    suspend fun clearAllLocalGames()

    @Query("DELETE FROM puzzle_attempts")
    suspend fun clearPuzzleAttempts()
}

@Database(
    entities = [
        LocalGameEntity::class,
        LocalStudyEntity::class,
        CollectionEntity::class,
        PuzzleAttemptEntity::class,
        CustomPuzzleEntity::class,
        LessonProgressEntity::class,
        ChessPuzzleEntity::class,
        MusicTrackEntity::class,
        CachedNewsEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class ChessHubDatabase : RoomDatabase() {
    abstract fun chessHubDao(): ChessHubDao
    abstract fun chessPuzzleDao(): ChessPuzzleDao
    abstract fun mediaAndNewsDao(): MediaAndNewsDao

    companion object {
        @Volatile
        private var instance: ChessHubDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chess_puzzles` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `fen` TEXT NOT NULL,
                        `solutionMoves` TEXT NOT NULL,
                        `difficultyRating` INTEGER NOT NULL,
                        `themes` TEXT NOT NULL,
                        `hint` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `isCheckmatePattern` INTEGER NOT NULL,
                        `patternName` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `solvedCount` INTEGER NOT NULL,
                        `attemptCount` INTEGER NOT NULL,
                        `bookmarked` INTEGER NOT NULL,
                        `lastAttemptedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chess_puzzles_difficultyRating` ON `chess_puzzles` (`difficultyRating`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chess_puzzles_source` ON `chess_puzzles` (`source`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chess_puzzles_isCheckmatePattern` ON `chess_puzzles` (`isCheckmatePattern`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chess_puzzles_bookmarked` ON `chess_puzzles` (`bookmarked`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `music_tracks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `uriString` TEXT NOT NULL,
                        `playlistName` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL DEFAULT 0,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_music_tracks_addedAt` ON `music_tracks` (`addedAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_news` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `sourceName` TEXT NOT NULL,
                        `publishedAt` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_news_fetchedAt` ON `cached_news` (`fetchedAt`)")

                // Preserve any existing custom puzzles created in v1
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `chess_puzzles` (
                        `id`, `title`, `fen`, `solutionMoves`, `difficultyRating`, `themes`,
                        `hint`, `explanation`, `isCheckmatePattern`, `patternName`, `source`,
                        `solvedCount`, `attemptCount`, `bookmarked`, `lastAttemptedAt`, `createdAt`
                    )
                    SELECT `id`, `title`, `fen`, `solutionUci`, `rating`, `theme`,
                        'Find the best continuation from your analyzed game.',
                        'Imported from v1 custom puzzles.', 0, '', 'custom',
                        0, 0, 0, NULL, `createdAt`
                    FROM `custom_puzzles`
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): ChessHubDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessHubDatabase::class.java,
                    "chesshub_local.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
