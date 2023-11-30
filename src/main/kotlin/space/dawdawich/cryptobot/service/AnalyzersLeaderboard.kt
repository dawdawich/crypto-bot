package space.dawdawich.cryptobot.service

import space.dawdawich.cryptobot.analyzer.AnalyzerCore

class AnalyzersLeaderboard(private val analyzers: List<AnalyzerCore>) {
    private val balanceLeaderboard: Map<AnalyzerCore, MutableMap<Int, Int>> =
        mapOf(*analyzers.map { it to mutableMapOf<Int, Int>() }.toTypedArray())

    private val ordersLeaderboard: Map<AnalyzerCore, MutableMap<Int, Int>> =
        mapOf(*analyzers.map { it to mutableMapOf<Int, Int>() }.toTypedArray())

    fun processPlaces() {
        analyzers.groupBy { it.wallet }.toList().sortedByDescending { it.first }.forEachIndexed { index, listPair ->
            listPair.second.forEach { analyzer ->
                balanceLeaderboard[analyzer]?.let {
                    val place = it.getOrPut(index) { 0 }
                    if (place < 4) {
                        it[index] = place + 1
                    }

                    val leaderboardIterator = it.iterator()
                    while (leaderboardIterator.hasNext()) {
                        val element = leaderboardIterator.next()

                        if (element.key != index) {
                            if (element.value > 0) {
                                element.setValue(element.value - 1)
                            } else {
                                leaderboardIterator.remove()
                            }
                        }
                    }
                }
            }
        }

        analyzers.groupBy { it.getOrderCompletionFactor() }.toList().sortedByDescending { it.first }.forEachIndexed { index, listPair ->
            listPair.second.forEach { analyzer ->
                ordersLeaderboard[analyzer]?.let {
                    val place = it.getOrPut(index) { 0 }
                    if (place < 4) {
                        it[index] = place + 1
                    }

                    val leaderboardIterator = it.iterator()
                    while (leaderboardIterator.hasNext()) {
                        val element = leaderboardIterator.next()

                        if (element.key != index) {
                            if (element.value > 0) {
                                element.setValue(element.value - 1)
                            } else {
                                leaderboardIterator.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getBalanceHigherPlace(gap: Int): AnalyzerCore {
        return balanceLeaderboard.entries.sortedBy { it.value.maxBy { it.value }.key }.take(gap).maxBy { it.key.wallet }.key
    }

    fun getOrderHigherPlace(gap: Int): AnalyzerCore {
        return ordersLeaderboard.entries.sortedBy { it.value.maxBy { it.value }.key }.take(gap).maxBy { it.key.wallet }.key
    }

    fun getHigherPlaceByBalanceAndOrder(gap: Int): AnalyzerCore {
        return balanceLeaderboard.entries.sortedBy { it.value.maxBy { it.value }.key }.take(gap).maxBy { ordersLeaderboard[it.key]!!.entries.maxBy { it.value }.key }.key
    }

    fun getHigherPlaces(gap: Int): List<AnalyzerCore> {
        return balanceLeaderboard.entries.sortedBy { it.value.maxBy { it.value }.key }.take(gap).map { it.key }
    }
}
