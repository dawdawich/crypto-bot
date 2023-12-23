package space.dawdawich.integration.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import space.dawdawich.exception.UnsuccessfulOperationException

abstract class DefaultHttpClient(private val serverUrl: String, private val client: HttpClient) {
    suspend fun get(url: String, queryString: String = "", headers: Array<Pair<String, List<String>>> = arrayOf()): HttpResponse {
        val response = client.get("$serverUrl$url?$queryString") {
            headersOf(*headers)
        }
        if (response.status != HttpStatusCode.OK) {
            throw UnsuccessfulOperationException(response.status.value)
        }
        return response
    }

    suspend fun post(url: String, body: String, headers: Array<Pair<String, List<String>>>): HttpResponse {
        val response = client.post("$serverUrl$url") {
            headersOf(*headers)
            setBody(body)
        }
        if (response.status != HttpStatusCode.OK) {
            throw UnsuccessfulOperationException(response.status.value)
        }
        return response
    }
}
