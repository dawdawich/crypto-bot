package space.dawdawich.model

import space.dawdawich.common.TpAndSlChecker
import space.dawdawich.strategy.model.Position

data class NewListingSession(
    val symbol: String,
    val leverage: Double,
    val money: Double,
    val position: Position,
    val sl: Int
) {
    val slChecker: TpAndSlChecker = TpAndSlChecker(money, 100, sl)
}
