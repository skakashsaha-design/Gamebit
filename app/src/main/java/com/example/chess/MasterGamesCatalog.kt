package com.example.chess

data class MasterGameEntry(
    val id: String,
    val title: String,
    val event: String,
    val white: String,
    val black: String,
    val year: Int,
    val eco: String,
    val opening: String,
    val result: String,
    val summary: String,
    val pgn: String,
)

object MasterGamesCatalog {
    val FAMOUS_GAMES: List<MasterGameEntry> = listOf(
        MasterGameEntry(
            id = "mg_opera_1858",
            title = "The Opera Game (Morphy)",
            event = "Paris Opera House",
            white = "Paul Morphy",
            black = "Duke Karl / Count Isouard",
            year = 1858,
            eco = "C41",
            opening = "Philidor Defense",
            result = "1-0",
            summary = "Textbook piece development, open-file sacrifices, and back-rank checkmate.",
            pgn = """
                [Event "Paris Opera"]
                [White "Paul Morphy"]
                [Black "Duke Karl & Count Isouard"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0
            """.trimIndent(),
        ),
        MasterGameEntry(
            id = "mg_century_1956",
            title = "Game of the Century (Fischer)",
            event = "Rosenwald Memorial, New York",
            white = "Donald Byrne",
            black = "Bobby Fischer (13)",
            year = 1956,
            eco = "D97",
            opening = "Grunfeld Defense",
            result = "0-1",
            summary = "13-year-old Fischer sacrifices his queen for an irresistible coordinated attack.",
            pgn = """
                [Event "Rosenwald Memorial"]
                [White "Donald Byrne"]
                [Black "Bobby Fischer"]
                [Result "0-1"]

                1. Nf3 Nf6 2. c4 g6 3. Nc3 Bg7 4. d4 O-O 5. Bf4 d5 6. Qb3 dxc4 7. Qxc4 c6 8. e4 Nbd7 9. Rd1 Nb6 10. Qc5 Bg4 11. Bg5 Na4 12. Qa3 Nxc3 13. bxc3 Nxe4 14. Bxe7 Qb6 15. Bc4 Nxc3 16. Bc5 Rfe8+ 17. Kf1 Be6 18. Bxb6 Bxc4+ 19. Kg1 Ne2+ 20. Kf1 Nxd4+ 21. Kg1 Ne2+ 22. Kf1 Nc3+ 23. Kg1 axb6 24. Qb4 Ra4 25. Qxb6 Nxd1 26. h3 Rxa2 27. Kh2 Nxf2 28. Re1 Rxe1 29. Qd8+ Bf8 30. Nxe1 Bd5 31. Nf3 Ne4 32. Qb8 b5 33. h4 h5 34. Ne5 Kg7 35. Kg1 Bc5+ 36. Kf1 Ng3+ 37. Ke1 Bb4+ 38. Kd1 Bb3+ 39. Kc1 Ne2+ 40. Kb1 Nc3+ 41. Kc1 Rc2# 0-1
            """.trimIndent(),
        ),
        MasterGameEntry(
            id = "mg_legals_1750",
            title = "Legal's Classic Trap",
            event = "Cafe de la Regence, Paris",
            white = "Sire de Legal",
            black = "Saint Brie",
            year = 1750,
            eco = "C41",
            opening = "Philidor Defense",
            result = "1-0",
            summary = "The archetype pseudo-pin queen sacrifice culminating in Bxf7+ Ke7 Nd5#.",
            pgn = """
                [Event "Paris 1750"]
                [White "Sire de Legal"]
                [Black "Saint Brie"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 d6 3. Bc4 Bg4 4. Nc3 g6 5. Nxe5 Bxd1 6. Bxf7+ Ke7 7. Nd5# 1-0
            """.trimIndent(),
        ),
        MasterGameEntry(
            id = "mg_immortal_1851",
            title = "The Immortal Game (Anderssen)",
            event = "London Casual Match",
            white = "Adolf Anderssen",
            black = "Lionel Kieseritzky",
            year = 1851,
            eco = "C33",
            opening = "King's Gambit Accepted",
            result = "1-0",
            summary = "Anderssen sacrifices a bishop, both rooks, and his queen to deliver checkmate with three minor pieces.",
            pgn = """
                [Event "London 1851"]
                [White "Adolf Anderssen"]
                [Black "Lionel Kieseritzky"]
                [Result "1-0"]

                1. e4 e5 2. f4 exf4 3. Bc4 Qh4+ 4. Kf1 b5 5. Bxb5 Nf6 6. Nf3 Qh6 7. d3 Nh5 8. Nh4 Qg5 9. Nf5 c6 10. g4 Nf6 11. Rg1 cxb5 12. h4 Qg6 13. h5 Qg5 14. Qf3 Ng8 15. Bxf4 Qf6 16. Nc3 Bc5 17. Nd5 Qxb2 18. Bd6 Bxg1 19. e5 Qxa1+ 20. Ke2 Na6 21. Nxg7+ Kd8 22. Qf6+ Nxf6 23. Be7# 1-0
            """.trimIndent(),
        ),
    )
}
