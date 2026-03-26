package services

object LocationRiskService {

    private val riskMap = mapOf(
        "bar" to 35,
        "night_club" to 40,
        "bus_station" to 30,
        "train_station" to 30,
        "park" to 20,
        "hospital" to 5,
        "police" to 0
    )

    fun getRisk(placeTypes: List<String>): Int {
        return placeTypes.map { riskMap[it] ?: 10 }.maxOrNull() ?: 10
    }
}