package core

enum class Mode {
    NORMAL,
    HIGH_ALERT
}

object ModeManager {
    fun getMode(risk: Int): Mode {
        return if (risk >= 80) Mode.HIGH_ALERT else Mode.NORMAL
    }
}