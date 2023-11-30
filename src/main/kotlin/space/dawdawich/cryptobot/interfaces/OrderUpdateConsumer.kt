package space.dawdawich.cryptobot.interfaces

import space.dawdawich.cryptobot.client.data.OrderResponse

fun interface OrderUpdateConsumer {
    fun process(order: OrderResponse)
}
