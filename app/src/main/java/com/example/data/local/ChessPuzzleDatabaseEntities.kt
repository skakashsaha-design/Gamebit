package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity managing ChessHub's local collection of tactical & checkmate puzzles.
 * Stores FEN starting positions, multi-move UCI solution sequences, and Glicko-style difficulty ratings.
 */
@Entity(
    tableName = "chess_puzzles",
    indices = [
        Index(value = ["difficultyRating"]),
        Index(value = ["source"]),
        Index(value = ["isCheckmatePattern"]),
        Index(value = ["bookmarked"]),
    ],
)
data class ChessPuzzleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fen: String,
    val solutionMoves: String, // Space-separated UCI move sequence (e.g. "d5f7 g8h8 f7f8")
    val difficultyRating: Int,
    val themes: String, // Comma-separated tactical or checkmate themes
    val hint: String = "",
    val explanation: String = "",
    val isCheckmatePattern: Boolean = false,
    val patternName: String = "",
    val source: String = "builtin", // "builtin", "custom", "mistake", "lichess_daily"
    val solvedCount: Int = 0,
    val attemptCount: Int = 0,
    val bookmarked: Boolean = false,
    val lastAttemptedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun solutionMoveList(): List<String> =
        solutionMoves.trim().split(Regex("[\\s,]+")).filter { it.isNotBlank() }

    fun themeList(): List<String> =
        themes.split(',').map { it.trim() }.filter { it.isNotBlank() }

    fun toTacticalPuzzle(): TacticalPuzzle = TacticalPuzzle(
        id = id,
        title = title,
        fen = fen,
        solutionUci = solutionMoveList(),
        rating = difficultyRating,
        themes = themeList().ifEmpty { listOf("Tactics") },
        hint = hint,
        explanation = explanation,
        isCheckmatePattern = isCheckmatePattern,
        patternName = patternName,
    )
}

@Dao
interface ChessPuzzleDao {
    @Query("SELECT * FROM chess_puzzles ORDER BY difficultyRating ASC, createdAt DESC")
    fun observeAllPuzzles(): Flow<List<ChessPuzzleEntity>>

    @Query("SELECT * FROM chess_puzzles ORDER BY difficultyRating ASC, createdAt DESC")
    suspend fun getAllPuzzles(): List<ChessPuzzleEntity>

    @Query("SELECT * FROM chess_puzzles WHERE id = :id")
    suspend fun getPuzzleById(id: String): ChessPuzzleEntity?

    @Query(
        "SELECT * FROM chess_puzzles WHERE difficultyRating BETWEEN :minRating AND :maxRating " +
            "ORDER BY difficultyRating ASC",
    )
    fun observeByDifficultyRange(minRating: Int, maxRating: Int): Flow<List<ChessPuzzleEntity>>

    @Query("SELECT * FROM chess_puzzles WHERE themes LIKE '%' || :theme || '%' ORDER BY difficultyRating ASC")
    fun observeByTheme(theme: String): Flow<List<ChessPuzzleEntity>>

    @Query("SELECT * FROM chess_puzzles WHERE isCheckmatePattern = 1 ORDER BY difficultyRating ASC")
    fun observeCheckmatePatterns(): Flow<List<ChessPuzzleEntity>>

    @Query("SELECT * FROM chess_puzzles WHERE bookmarked = 1 ORDER BY lastAttemptedAt DESC, difficultyRating ASC")
    fun observeBookmarkedPuzzles(): Flow<List<ChessPuzzleEntity>>

    @Query("SELECT * FROM chess_puzzles WHERE source = :source ORDER BY createdAt DESC")
    fun observeBySource(source: String): Flow<List<ChessPuzzleEntity>>

    @Query("SELECT COUNT(*) FROM chess_puzzles")
    suspend fun countPuzzles(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPuzzle(puzzle: ChessPuzzleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoreConflict(puzzles: List<ChessPuzzleEntity>)

    @Update
    suspend fun updatePuzzle(puzzle: ChessPuzzleEntity)

    @Query("UPDATE chess_puzzles SET bookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)

    @Query(
        "UPDATE chess_puzzles SET attemptCount = attemptCount + 1, " +
            "solvedCount = solvedCount + CASE WHEN :solved THEN 1 ELSE 0 END, " +
            "lastAttemptedAt = :timestamp WHERE id = :id",
    )
    suspend fun recordPuzzleAttemptStats(
        id: String,
        solved: Boolean,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM chess_puzzles WHERE id = :id")
    suspend fun deletePuzzleById(id: String)

    @Query("DELETE FROM chess_puzzles WHERE source = 'custom'")
    suspend fun clearCustomPuzzles()
}

@Entity(
    tableName = "music_tracks",
    indices = [Index(value = ["addedAt"])],
)
data class MusicTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String = "Local Audio",
    val uriString: String,
    val playlistName: String = "My Focus Playlist",
    val durationMs: Long = 0L,
    val isBuiltIn: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cached_news",
    indices = [Index(value = ["fetchedAt"])],
)
data class CachedNewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val sourceName: String,
    val publishedAt: String,
    val category: String = "Chess News",
    val fetchedAt: Long = System.currentTimeMillis(),
)

@Dao
interface MediaAndNewsDao {
    @Query("SELECT * FROM music_tracks ORDER BY addedAt DESC")
    fun observeMusicTracks(): Flow<List<MusicTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMusicTrack(track: MusicTrackEntity)

    @Query("DELETE FROM music_tracks WHERE id = :id")
    suspend fun deleteMusicTrack(id: String)

    @Query("SELECT * FROM cached_news ORDER BY fetchedAt DESC LIMIT 40")
    fun observeCachedNews(): Flow<List<CachedNewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNews(items: List<CachedNewsEntity>)

    @Query("DELETE FROM cached_news")
    suspend fun clearCachedNews()
}
