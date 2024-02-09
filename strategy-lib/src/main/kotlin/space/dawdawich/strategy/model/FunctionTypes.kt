package space.dawdawich.strategy.model

typealias MoneyChangePostProcessFunction = (old: Double, new: Double) -> Unit
typealias UpdateMiddlePricePostProcessFunction = (Double) -> Unit
typealias CreateOrderFunction = (inPrice: Double, symbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend) -> Order?
typealias CancelOrderFunction = (symbol: String, orderId: String) -> Boolean
typealias ClosePositionFunction = () -> Unit
