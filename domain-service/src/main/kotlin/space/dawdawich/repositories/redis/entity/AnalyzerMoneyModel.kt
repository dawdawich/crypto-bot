package space.dawdawich.repositories.redis.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed
import java.io.Serializable
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@RedisHash("AnalyzersMoney")
class AnalyzerMoneyModel(
        @Indexed
        var analyzerId: String,
        var money: Double,
        @Id
        @JsonIgnore
        val id: String = UUID.randomUUID().toString()
) : Serializable {

    @JsonIgnore
    @TimeToLive(unit = TimeUnit.MINUTES)
    var timeToLive: Long = 1440

    var timestamp: Long = Instant.now().epochSecond
}
