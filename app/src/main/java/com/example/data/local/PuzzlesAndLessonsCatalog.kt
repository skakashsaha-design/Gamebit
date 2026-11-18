package com.example.data.local

data class TacticalPuzzle(
    val id: String,
    val title: String,
    val fen: String,
    val solutionUci: List<String>,
    val rating: Int,
    val themes: List<String>,
    val hint: String,
    val explanation: String,
)

data class LessonStep(
    val title: String,
    val explanation: String,
    val fen: String,
    val expectedMoveUci: String?, // null = reading/example step
    val hint: String = "",
)

data class ChessLesson(
    val id: String,
    val level: String, // Beginner, Intermediate, Advanced
    val category: String,
    val title: String,
    val summary: String,
    val steps: List<LessonStep>,
)

data class EndgameExercise(
    val id: String,
    val category: String,
    val title: String,
    val objective: String,
    val fen: String,
    val bestStartUci: String,
    val tip: String,
)

object PuzzlesAndLessonsCatalog {
    val PUZZLE_THEMES = listOf(
        "Mate in 1", "Mate in 2", "Fork", "Pin", "Skewer",
        "Discovered Attack", "Double Attack", "Deflection", "Sacrifice",
        "Attraction", "Interference", "Promotion", "Endgame",
        "Opening", "Defensive Move", "Back Rank Mate",
    )

    val OFFLINE_PUZZLES: List<TacticalPuzzle> = listOf(
        TacticalPuzzle(
            id = "pz_backrank_1",
            title = "Classic Back Rank",
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d1d8"),
            rating = 850,
            themes = listOf("Mate in 1", "Back Rank Mate"),
            hint = "Look at Black's trapped king behind the f7-g7-h7 pawns.",
            explanation = "Rd8# delivers an immediate back-rank checkmate because the pawns block the king's escape.",
        ),
        TacticalPuzzle(
            id = "pz_fork_1",
            title = "Royal Knight Fork",
            fen = "r3k3/8/8/3N4/8/8/8/4K3 w q - 0 1",
            solutionUci = listOf("d5c7"),
            rating = 950,
            themes = listOf("Fork", "Double Attack"),
            hint = "Attack the Black king and rook simultaneously with your knight.",
            explanation = "Nc7+ forks the king on e8 and the rook on a8.",
        ),
        TacticalPuzzle(
            id = "pz_smothered_1",
            title = "Arabian Mate",
            fen = "6k1/5Npp/8/8/8/8/8/6RK w - - 0 1",
            solutionUci = listOf("g1g8"),
            rating = 1100,
            themes = listOf("Mate in 1", "Endgame"),
            hint = "Your knight covers h8 and g5? Actually check g8!",
            explanation = "Wait: Rg8# is defended by the knight? No, knight on f7 covers g5 and h6/h8; let's check h6—rook on g1 goes g8? Wait f7 knight doesn't defend g8, f7 covers h8! Look: Rg8# forces... wait Kxf7? No, king is on g8->Rg8 is capture! Check pz_smothered_2.",
        ).let {
            // Corrected Arabian mate where Knight on f6 defends g8 and covers h7
            TacticalPuzzle(
                id = "pz_arabian_1",
                title = "Arabian Mate",
                fen = "7k/8/5N2/8/8/8/8/6RK w - - 0 1",
                solutionUci = listOf("g1g8"),
                rating = 1050,
                themes = listOf("Mate in 1", "Endgame"),
                hint = "Your knight on f6 guards g8 and takes away g8/h7.",
                explanation = "Rg8# is the classic Arabian mate: the knight defends the rook and seals h7.",
            )
        },
        TacticalPuzzle(
            id = "pz_pin_1",
            title = "Deadly Diagonal Pin",
            fen = "4k3/8/3q4/8/1B6/8/8/4K3 w - - 0 1",
            solutionUci = listOf("b4d6"),
            rating = 900,
            themes = listOf("Pin"),
            hint = "Exploit the alignment along the a3-f8 diagonal.",
            explanation = "Bxd6 captures the pinned queen? Even better, wait if queen was pinned to king, Bxd6 wins the queen directly.",
        ),
        TacticalPuzzle(
            id = "pz_skewer_1",
            title = "Royal Skewer",
            fen = "4k3/8/8/8/8/8/4q3/R3K3 w - - 0 1",
            solutionUci = listOf("a1a8"),
            rating = 1150,
            themes = listOf("Skewer", "Endgame"),
            hint = "Check the Black king on the 8th rank? No, use a rank/file skewer!",
            explanation = "Ra8+ drives the king off the back rank and wins? Wait queen is on e2, let's check a cleaner skewer: e1-e8 file!",
        ).let {
            TacticalPuzzle(
                id = "pz_skewer_1",
                title = "Back-Rank Skewer",
                fen = "4k2q/8/8/8/8/8/8/R3K3 w - - 0 1",
                solutionUci = listOf("a1a8", "e8f7", "a8h8"),
                rating = 1200,
                themes = listOf("Skewer", "Endgame"),
                hint = "Force the king to step away from protecting the corner queen.",
                explanation = "Ra8+ skewers the king on e8 and the queen on h8.",
            )
        },
        TacticalPuzzle(
            id = "pz_discovery_1",
            title = "Discovered Check Tactics",
            fen = "3qk3/8/8/8/3B4/8/3R4/4K3 w - - 0 1",
            solutionUci = listOf("d4b6"),
            rating = 1250,
            themes = listOf("Discovered Attack", "Double Attack"),
            hint = "Move the bishop with tempo so the rook uncovers an attack on the d8 queen or king.",
            explanation = "Bb6+ (or moving the bishop) discovers an attack on the d8 queen while threatening!",
        ).let {
            TacticalPuzzle(
                id = "pz_discovery_1",
                title = "Discovered Check",
                fen = "3k3r/8/8/8/3B4/8/3R4/4K3 w - - 0 1",
                solutionUci = listOf("d4h8"),
                rating = 1180,
                themes = listOf("Discovered Attack", "Double Attack"),
                hint = "Move the bishop out of the d-file while capturing a free piece with discovered check!",
                explanation = "Bxh8+ delivers a discovered check from the d2 rook and wins the h8 rook.",
            )
        },
        TacticalPuzzle(
            id = "pz_promotion_1",
            title = "Underpromotion Twist",
            fen = "8/4P1k1/8/8/8/8/5K2/8 w - - 0 1",
            solutionUci = listOf("e7e8q"),
            rating = 980,
            themes = listOf("Promotion", "Endgame"),
            hint = "Push the pawn to the 8th rank to gain decisive material.",
            explanation = "e8=Q promotes with overwhelming material advantage.",
        ),
        TacticalPuzzle(
            id = "pz_deflection_1",
            title = "Deflect the Defender",
            fen = "3r2k1/5ppp/8/8/8/8/2Q2PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d1d8"),
            rating = 1120,
            themes = listOf("Deflection", "Back Rank Mate"),
            hint = "Challenge the defender of the d8 square directly.",
            explanation = "Rxd8# wins immediately because Black's rook is the sole defender of the back rank.",
        ),
        TacticalPuzzle(
            id = "pz_scholars_defense",
            title = "Defend f7 Correctly",
            fen = "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 3 3",
            solutionUci = listOf("g7g6"),
            rating = 920,
            themes = listOf("Defensive Move", "Opening"),
            hint = "Block the queen's attack on f7 and kick the queen away.",
            explanation = "g6 defends against Qxf7# and attacks the White queen simultaneously.",
        ),
        TacticalPuzzle(
            id = "pz_attraction_1",
            title = "Queen Sacrifice Attraction",
            fen = "6k1/5ppp/8/8/8/5Q2/5PPP/4R1K1 w - - 0 1",
            solutionUci = listOf("f3a8"),
            rating = 1320,
            themes = listOf("Mate in 1", "Back Rank Mate"),
            hint = "Look at the open a8-h1 diagonal and the 8th rank.",
            explanation = "Qa8# delivers checkmate along the back rank.",
        ),
        TacticalPuzzle(
            id = "pz_sacrifice_1",
            title = "Greek Gift Sacrifice",
            fen = "r1bq1rk1/pppn1ppp/4pn2/3p4/1bPP4/2NBPN2/PP3PPP/R1BQK2R w KQ - 2 7",
            solutionUci = listOf("d3h7"),
            rating = 1520,
            themes = listOf("Sacrifice", "Opening"),
            hint = "Look at the classic bishop sacrifice on h7 against the castled king.",
            explanation = "Bxh7+ opens up the Black king for Ng5+ and Qh5 follow-ups.",
        ),
        TacticalPuzzle(
            id = "pz_interference_1",
            title = "Line Interference",
            fen = "4r1k1/5ppp/8/3Q4/3B4/8/5PPP/6K1 w - - 0 1",
            solutionUci = listOf("d5f7"),
            rating = 1350,
            themes = listOf("Mate in 2", "Attraction"),
            hint = "Attack f7 to force the king into a corner mate.",
            explanation = "Qxf7+ Kxf7 forces? Wait rook on e8 defends nothing on f7, so Qxf7+ Kh8 Qxg7# or Qxe8#!",
        ).let {
            TacticalPuzzle(
                id = "pz_mate2_1",
                title = "Corner Force Mate in 2",
                fen = "6k1/5ppp/8/3Q4/2B5/8/5PPP/6K1 w - - 0 1",
                solutionUci = listOf("d5f7", "g8h8", "f7f8"),
                rating = 1280,
                themes = listOf("Mate in 2", "Attraction"),
                hint = "Check on f7 supported by your bishop on c4.",
                explanation = "1. Qxf7+ Kh8 2. Qf8# is a clean mate in two.",
            )
        },
        TacticalPuzzle(
            id = "pz_endgame_oppos",
            title = "Opposition Breakthrough",
            fen = "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
            solutionUci = listOf("d3e3"),
            rating = 1400,
            themes = listOf("Endgame", "Promotion"),
            hint = "Keep the king in front of the pawn to secure key squares.",
            explanation = "Ke3 outflanks the defending king and prepares to shepherd the d-pawn.",
        ),
        TacticalPuzzle(
            id = "pz_opening_trap",
            title = "Legal's Mate Motif",
            fen = "r2qkbnr/ppp2ppp/2np4/4N3/2B1P1b1/2N5/PPPP1PPP/R1BQK2R w KQkq - 0 5",
            solutionUci = listOf("c4f7"),
            rating = 1450,
            themes = listOf("Opening", "Sacrifice", "Mate in 2"),
            hint = "Target f7 with your bishop now that the knight guards e5.",
            explanation = "Bxf7+ Ke7 Nd5# is the famous Legal's Mate pattern.",
        ),
        TacticalPuzzle(
            id = "pz_interference_real",
            title = "Interference Blockade",
            fen = "2r3k1/5ppp/8/4N3/8/8/5PPP/2R3K1 w - - 0 1",
            solutionUci = listOf("c1c8"),
            rating = 1080,
            themes = listOf("Interference", "Back Rank Mate"),
            hint = "Trade on c8 to exploit the back rank.",
            explanation = "Rxc8# delivers immediate checkmate on the back rank.",
        ),
    )

    val LESSONS: List<ChessLesson> = listOf(
        ChessLesson(
            id = "les_beg_basics",
            level = "Beginner",
            category = "Chess Basics",
            title = "Piece Activity & Center Control",
            summary = "Control e4, d4, e5, d5 early and develop knights before bishops.",
            steps = listOf(
                LessonStep(
                    title = "Claim the Center",
                    explanation = "Pawns on e4 or d4 immediately influence the center squares and open lines for your queen and bishops.",
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    expectedMoveUci = "e2e4",
                    hint = "Push the king's pawn two squares to e4.",
                ),
                LessonStep(
                    title = "Develop Toward the Center",
                    explanation = "Knights belong on f3 and c3 where they control central squares and prepare kingside castling.",
                    fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                    expectedMoveUci = "g1f3",
                    hint = "Develop your kingside knight to attack e5.",
                ),
            ),
        ),
        ChessLesson(
            id = "les_beg_castling",
            level = "Beginner",
            category = "King Safety",
            title = "Castling to Safety",
            summary = "Protect your king and activate your rook in a single move.",
            steps = listOf(
                LessonStep(
                    title = "Castle Kingside",
                    explanation = "Once the squares between your king and rook are clear and unattacked, castle kingside to tuck the king behind pawns.",
                    fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                    expectedMoveUci = "e1g1",
                    hint = "Move your king two squares toward the h1 rook (O-O).",
                ),
            ),
        ),
        ChessLesson(
            id = "les_int_tactics",
            level = "Intermediate",
            category = "Tactics",
            title = "Double Attacks & Forks",
            summary = "Attack two valuable targets at once so the opponent cannot save both.",
            steps = listOf(
                LessonStep(
                    title = "Execute the Knight Fork",
                    explanation = "Knights are supreme forking pieces because they jump over defenders and attack in L-shapes.",
                    fen = "r3k3/8/8/3N4/8/8/8/4K3 w q - 0 1",
                    expectedMoveUci = "d5c7",
                    hint = "Jump to c7 to attack both the king and the a8 rook.",
                ),
            ),
        ),
        ChessLesson(
            id = "les_int_openings",
            level = "Intermediate",
            category = "Openings",
            title = "The Italian Game & Evan's Gambit Ideas",
            summary = "Build rapid pressure on f7 and control the a2-g8 diagonal.",
            steps = listOf(
                LessonStep(
                    title = "Develop the Italian Bishop",
                    explanation = "Bc4 eyes the vulnerable f7 square directly.",
                    fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
                    expectedMoveUci = "f1c4",
                    hint = "Place your light-squared bishop on c4.",
                ),
            ),
        ),
        ChessLesson(
            id = "les_adv_endgame",
            level = "Advanced",
            category = "Endgames",
            title = "Lucena & Opposition Technique",
            summary = "Convert pawn advantages using king opposition and outflanking.",
            steps = listOf(
                LessonStep(
                    title = "Seize Key Squares",
                    explanation = "Advance the king ahead of the pawn to clear the promotion path.",
                    fen = "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
                    expectedMoveUci = "d3e3",
                    hint = "Step up to e3 to outflank Black's king.",
                ),
            ),
        ),
    )

    val ENDGAMES: List<EndgameExercise> = listOf(
        EndgameExercise(
            id = "eg_kp_1",
            category = "King + Pawn",
            title = "Direct Opposition",
            objective = "Win the king-and-pawn endgame by stepping in front of your pawn.",
            fen = "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
            bestStartUci = "d3e3",
            tip = "Never push the pawn prematurely when your king can outflank.",
        ),
        EndgameExercise(
            id = "eg_rook_1",
            category = "Rook Endgame",
            title = "Cut Off the King",
            objective = "Use your rook to restrict the enemy king along a file.",
            fen = "8/8/8/2k5/8/4K3/4P3/R7 w - - 0 1",
            bestStartUci = "a1d1",
            tip = "Cutting the defending king off by one or two files often decides rook endgames.",
        ),
        EndgameExercise(
            id = "eg_queen_1",
            category = "Queen Endgame",
            title = "Queen Box Checkmate",
            objective = "Coordinate Queen and King to force mate on the edge.",
            fen = "7k/8/5Q2/8/8/8/6K1/8 w - - 0 1",
            bestStartUci = "f6g6",
            tip = "Avoid stalemate! Leave the defending king at least one legal square until mate.",
        ),
        EndgameExercise(
            id = "eg_minor_1",
            category = "Minor Piece",
            title = "Bishop & Knight Coordination",
            objective = "Centralize your knight and bishop to dominate the board.",
            fen = "7k/8/6K1/8/4B3/3N4/8/8 w - - 0 1",
            bestStartUci = "d3e5",
            tip = "Drive the defending king toward the corner matching your bishop's square color.",
        ),
    )
}
