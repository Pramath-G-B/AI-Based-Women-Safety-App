package core

class AlertManager {

    private var lastTriggerTime = 0L

    fun shouldTrigger(
        risk: Int,
        mode: Mode,
        audio: Boolean,
        impact: Boolean
    ): Boolean {

        val now = System.currentTimeMillis()

        // cooldown 10 seconds
        val isCooldown = (now - lastTriggerTime < 10000)

        if (isCooldown && risk < 90) return false


        val trigger = when (mode) {

            Mode.NORMAL -> {
                (risk >= 70 && (audio || impact)) ||
                (impact && risk >= 60)   
            }

            Mode.HIGH_ALERT -> {
                audio || impact
            }
        }

        if (trigger) {
            lastTriggerTime = now
        }

        return trigger
    }
}