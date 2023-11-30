package space.dawdawich.cryptobot.util

import java.util.concurrent.atomic.AtomicInteger

private val counter = AtomicInteger()

fun generateId(): String {
    val timestamp = System.currentTimeMillis()
    val currentCounter = counter.incrementAndGet()
    return "${timestamp}--${currentCounter}"
}
