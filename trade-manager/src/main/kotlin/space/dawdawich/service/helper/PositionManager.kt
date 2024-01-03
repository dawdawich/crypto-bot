package space.dawdawich.service.helper

import space.dawdawich.service.model.Position

class PositionManager(instructionPositions: List<Position>) {
    private val isOneWay: Boolean
    private val positions: MutableSet<Position>

    init {
        if (instructionPositions.size == 1 && instructionPositions[0].positionIdx == 0) {
            isOneWay = true
            positions = mutableSetOf(
                Position(instructionPositions[0].symbol, true, 0.0, 0.0, 0, instructionPositions[0].updateTime),
                Position(instructionPositions[0].symbol, false, 0.0, 0.0, 0, instructionPositions[0].updateTime)
            )
        } else {
            isOneWay = false
            positions = instructionPositions.map { Position(instructionPositions[0].symbol, instructionPositions[0].isLong, instructionPositions[0].size, instructionPositions[0].entryPrice, instructionPositions[0].positionIdx, instructionPositions[0].updateTime) }.toMutableSet()
        }
    }

    fun updatePosition(positionsToUpdate: List<Position>) {
        positionsToUpdate.forEach {position ->
            positions.find { it.isLong == position.isLong && it.updateTime < position.updateTime }?.let {
                positions.remove(it)
                positions.add(position)
            }
        }
    }

    fun checkPositions(price: Double, sl: Double, tp: Double, exceedFunction: (Position) -> Unit = {_ ->}): Boolean {
        var result = false
        positions.filter { it.size > 0.0 && it.isTpOrSlCrossed(price, sl, tp) }.forEach {
            result = true
            exceedFunction(it)
        }
        return result
    }

    fun getPositionIdx(isLong: Boolean): Int {
        if (isOneWay) {
            return 0;
        }
        return positions.first { it.isLong == isLong }.positionIdx
    }

    fun getPositionsValue(): Double {
        return positions.sumOf {
            it.size * it.entryPrice
        }
    }

    fun getPositions(): Set<Position> {
        return positions
    }

    fun isOneWay() = isOneWay
}
