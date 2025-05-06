package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("request_status")
class RequestStatusDocument(
    @Id
    val id: String,
    val accountId: String,
    val status: RequestStatus
)

enum class RequestStatus {
    SUCCESS, IN_PROGRESS, FAILED;
}
