package space.dawdawich.utils

import space.dawdawich.constants.BYBIT_TEST_TICKER_TOPIC
import space.dawdawich.constants.BYBIT_TICKER_TOPIC
import space.dawdawich.model.constants.Market

fun getRightTopic(market: Market, demoAcc: Boolean): String {
    return when(market) {
        Market.BYBIT -> if (!demoAcc) BYBIT_TICKER_TOPIC else BYBIT_TEST_TICKER_TOPIC
    }
}
