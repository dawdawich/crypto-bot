package space.dawdawich.cryptobot.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


object HttpUtils {
    private const val PUBLIC_KEY = "sasce6qaiJfkTMhC5D"
    private const val SECRET_KEY = "NzZQabK4shvHbmtI6lsuzqEWvsQtwtD9Y73p"
    private const val RECV_WINDOW = "5000"

    private val client = HttpClient(CIO)

    suspend fun get(url: String): String {
        return client.get(url) {
            headers {
                headersBuilder(this, url.split("?")[1])
            }
        }.bodyAsText()
    }

    suspend fun post(url: String, body: String): String {
        val post = client.post(url) {
            setBody(body)
            headers {
                headersBuilder(this, body)
            }
        }
        return post.bodyAsText()
    }

    private val headersBuilder: (HeadersBuilder, String) -> Unit = { builder,body ->
        val timestamp = ZonedDateTime.now().toInstant().toEpochMilli().toString()
        builder.append("X-BAPI-API-KEY", PUBLIC_KEY)
        builder.append("X-BAPI-SIGN", genSign(body, timestamp))
        builder.append("X-BAPI-SIGN-TYPE", "2")
        builder.append("X-BAPI-TIMESTAMP", timestamp)
        builder.append("X-BAPI-RECV-WINDOW", RECV_WINDOW)
        builder.append("Content-Type", "application/json")
    }

    private fun genSign(body: String, timestamp: String): String {
        val signature = "$timestamp$PUBLIC_KEY$RECV_WINDOW$body"
        val sha256Mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")

        sha256Mac.init(secretKey)

        return sha256Mac.doFinal(signature.toByteArray()).bytesToHex()
    }

    fun getSighForWebsocket(): Triple<String, Long, String> {
        val expire = System.currentTimeMillis() + 86_400_000
        val sha256Mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")

        sha256Mac.init(secretKey)

        val hash = sha256Mac.doFinal("GET/realtime$expire".toByteArray())
        var signature = ""
        for (byte in hash) {
            signature += String.format("%02x", byte)
        }

        return Triple(PUBLIC_KEY, expire, signature)
    }
}
