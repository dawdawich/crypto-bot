package space.dawdawich.strategy.model

typealias MoneyChangePostProcessFunction = (Double, Double) -> Unit
typealias UpdateMiddlePricePostProcessFunction = (Double) -> Unit
typealias CreateOrderFunction = (inPrice: Double, symbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend) -> Order?
