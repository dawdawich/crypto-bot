package space.dawdawich.integration.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import space.dawdawich.exception.UnsuccessfulOperationException

abstract class DefaultHttpClient(private val serverUrl: String, private val client: HttpClient) {
    suspend fun get(
        url: String,
        queryString: String = "",
        headers: Array<Pair<String, String>> = arrayOf()
    ): HttpResponse {
        return client.get("$serverUrl$url?$queryString") {
            headers {
                headers.forEach { append(it.first, it.second) }
            }
        }
    }

    suspend fun post(url: String, body: String, headers: Array<Pair<String, String>>): HttpResponse {
        return client.post("$serverUrl$url") {
            headers {
                headers.forEach { append(it.first, it.second) }
            }
            setBody(body)
        }
    }

    suspend fun post(url: String, queryParameters: Array<Pair<String, String>>, headers: Array<Pair<String, String>>): HttpResponse {
        return client.post("$serverUrl$url") {
            queryParameters.forEach {
                parameter(it.first, it.second)
            }
            headers {
                headers.forEach { append(it.first, it.second) }
            }
            setBody(body)

        }
    }

    suspend fun delete(url: String, queryParameters: Array<Pair<String, String>>, headers: Array<Pair<String, String>>): HttpResponse {
        return client.delete("$serverUrl$url") {
            queryParameters.forEach {
                parameter(it.first, it.second)
            }
            headers {
                headers.forEach { append(it.first, it.second) }
            }
            setBody(body)
        }
    }
}
