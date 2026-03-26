package com.example.safeguard.core

class RiskEngine {

    private var previousRisk = 0

    fun compute(
        audioLevel: Int,
        impactLevel: Int,
        locationRisk: Int,
        hour: Int
    ): Int {

        val audioScore = when {
            audioLevel >= 4000 -> 50
            audioLevel >= 2500 -> 35
            audioLevel >= 1200 -> 20
            audioLevel >= 500 -> 10
            else -> 0
        }

        val impactScore = when {
            impactLevel >= 70 -> 40
            impactLevel >= 40 -> 25
            impactLevel >= 20 -> 10
            else -> 0
        }

        val timeScore = if (hour in 0..4 || hour in 22..23) 20 else 5

        val passiveRisk = (locationRisk * 0.5).toInt() + timeScore
        val raw = audioScore + impactScore + passiveRisk

        val smoothed = if (audioScore == 0 && impactScore == 0) {
            (previousRisk * 0.5 + raw * 0.5).toInt()
        } else {
            (previousRisk * 0.3 + raw * 0.7).toInt()
        }

        previousRisk = smoothed.coerceIn(0, 100)
        return previousRisk
    }
}