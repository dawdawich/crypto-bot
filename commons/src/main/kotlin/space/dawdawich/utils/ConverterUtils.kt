package space.dawdawich.utils

import java.util.*

val decoder: Base64.Decoder = Base64.getDecoder()
val encoder: Base64.Encoder = Base64.getEncoder()
fun String.baseDecode(): String = String(decoder.decode(this))
fun String.baseEncode(): String = String(encoder.encode(this.toByteArray()))
fun ByteArray.baseEncode(): String = encoder.encodeToString(this)
