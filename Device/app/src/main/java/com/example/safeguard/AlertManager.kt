package com.example.safeguard.core

class AlertManager {

    private var lastTriggerTime = 0L

    fun shouldTrigger(
        risk: Int,
        mode: Mode,
        audioLevel: Int,
        impactLevel: Int
    ): Boolean {

        val now = System.currentTimeMillis()
        val isCooldown = (now - lastTriggerTime < 10000)

        if (isCooldown && risk < 90) return false

        val strongAudio = audioLevel >= 2500
        val strongImpact = impactLevel >= 40

        val trigger = when (mode) {
            Mode.NORMAL -> {
                (risk >= 70 && (strongAudio || strongImpact)) ||
                        (strongImpact && risk >= 60)
            }

            Mode.HIGH_ALERT -> {
                strongAudio || strongImpact
            }
        }

        if (trigger) {
            lastTriggerTime = now
        }

        return trigger
    }

    fun getTriggerType(
        audioLevel: Int,
        impactLevel: Int
    ): String {
        val strongAudio = audioLevel >= 2500
        val strongImpact = impactLevel >= 40

        return when {
            strongAudio && strongImpact -> "AUTO_BOTH"
            strongAudio -> "AUDIO"
            strongImpact -> "IMPACT"
            else -> "UNKNOWN"
        }
    }
}