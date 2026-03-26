package core

class RiskEngine {

    private var previousRisk = 0

    fun compute(
        distressAudio: Boolean,
        impact: Boolean,
        locationRisk: Int,
        hour: Int
    ): Int {

        val audioScore = if (distressAudio) 50 else 0
        val impactScore = if (impact) 40 else 0
        val timeScore = if (hour in 0..4) 20 else 5

        val raw = audioScore + impactScore + locationRisk + timeScore

        val smoothed = (0.4 * previousRisk + 0.6 * raw).toInt()

        previousRisk = smoothed

        return smoothed.coerceIn(0, 100)
    }
}