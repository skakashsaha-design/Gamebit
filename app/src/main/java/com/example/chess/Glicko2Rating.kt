package com.example.chess

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class GlickoRating(
    val rating: Double = 1500.0,
    val rd: Double = 350.0,
    val volatility: Double = 0.06,
) {
    val isProvisional: Boolean get() = rd > 110.0
    val displayInt: Int get() = rating.toInt()
}

/**
 * Standard Glicko-2 rating update implementation (Mark Glickman, 2013).
 */
object Glicko2RatingSystem {
    private const val SCALE = 173.7178
    private const val TAU = 0.5
    private const val EPSILON = 0.000001

    fun updateSingleGame(
        player: GlickoRating,
        opponent: GlickoRating,
        score: Double, // 1.0 = win, 0.5 = draw, 0.0 = loss
    ): GlickoRating {
        val mu = (player.rating - 1500.0) / SCALE
        val phi = player.rd / SCALE
        val muJ = (opponent.rating - 1500.0) / SCALE
        val phiJ = opponent.rd / SCALE

        val gPhiJ = 1.0 / sqrt(1.0 + 3.0 * phiJ * phiJ / (PI * PI))
        val expected = 1.0 / (1.0 + exp(-gPhiJ * (mu - muJ)))
        val v = 1.0 / (gPhiJ * gPhiJ * expected * (1.0 - expected))
        val delta = v * gPhiJ * (score - expected)

        val a = ln(player.volatility * player.volatility)
        fun f(x: Double): Double {
            val ex = exp(x)
            val phi2 = phi * phi
            val d2 = delta * delta
            val num = ex * (d2 - phi2 - v - ex)
            val den = 2.0 * (phi2 + v + ex) * (phi2 + v + ex)
            return num / den - (x - a) / (TAU * TAU)
        }

        var bigA = a
        var bigB = if (delta * delta > phi * phi + v) {
            ln(delta * delta - phi * phi - v)
        } else {
            var k = 1
            while (f(a - k * TAU) < 0) k++
            a - k * TAU
        }

        var fA = f(bigA)
        var fB = f(bigB)
        var iter = 0
        while (abs(bigB - bigA) > EPSILON && iter < 50) {
            val bigC = bigA + (bigA - bigB) * fA / (fB - fA)
            val fC = f(bigC)
            if (fC * fB <= 0.0) {
                bigA = bigB
                fA = fB
            } else {
                fA /= 2.0
            }
            bigB = bigC
            fB = fC
            iter++
        }

        val newSigma = exp(bigA / 2.0)
        val phiStar = sqrt(phi * phi + newSigma * newSigma)
        val newPhi = 1.0 / sqrt(1.0 / (phiStar * phiStar) + 1.0 / v)
        val newMu = mu + newPhi * newPhi * gPhiJ * (score - expected)

        return GlickoRating(
            rating = (1500.0 + SCALE * newMu).coerceIn(100.0, 3800.0),
            rd = (SCALE * newPhi).coerceIn(30.0, 350.0),
            volatility = newSigma,
        )
    }
}
