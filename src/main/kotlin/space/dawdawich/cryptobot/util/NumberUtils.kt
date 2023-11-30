package space.dawdawich.cryptobot.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun Double.calculatePercentageChange(value: Double): Double {
    return ((value - this) / abs(this)) * 100.0
}

fun Double.plusPercentAccurate(value: Number): Double {
    return BigDecimal(this).multiply(BigDecimal(1 + value.toDouble() / 100)).toDouble()
}

fun Double.plusPercent(value: Number): Double {
    return (this * (1 + value.toDouble() / 100))
}

fun Double.leaveTail(length: Int): Double {
    return BigDecimal(this).setScale(length, RoundingMode.HALF_UP).toDouble()
}

fun ByteArray.bytesToHex(): String {
    val hexString = StringBuilder()
    for (b in this) {
        val hex = Integer.toHexString(0xff and b.toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    return hexString.toString()
}

operator fun ClosedRange<Double>.iterator(): Iterator<Double> =
    DoubleProgression(from = start, to = endInclusive, step = 1.0).iterator()

infix fun ClosedRange<Double>.step(step: Double): DoubleProgression =
    DoubleProgression(from = start, to = endInclusive, step = step)

data class DoubleProgression(
    val from: Double,
    val to: Double,
    val step: Double
) : Iterable<Double> {
    override fun iterator(): Iterator<Double> = DoubleProgressionIterator(from, to, step)
}

class DoubleProgressionIterator(
    start: Double,
    private val end: Double,
    private val step: Double
) : Iterator<Double> {
    private var current = start

    override fun hasNext(): Boolean = current <= end

    override fun next(): Double {
        val next = current
        current += step
        return next
    }
}
