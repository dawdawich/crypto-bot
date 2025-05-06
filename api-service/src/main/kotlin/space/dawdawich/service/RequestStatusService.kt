package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.repositories.mongo.RequestStatusRepository
import space.dawdawich.repositories.mongo.entity.RequestStatus
import space.dawdawich.repositories.mongo.entity.RequestStatusDocument
import java.util.*

@Service
class RequestStatusService(private val requestStatusRepository: RequestStatusRepository) {

    fun createRequestStatus(accountId: String, requestId: String) {
        requestStatusRepository.insert(RequestStatusDocument(requestId, accountId, RequestStatus.IN_PROGRESS))
    }

    fun getRequestStatus(requestId: String): RequestStatus? =
        requestStatusRepository.findById(requestId).toNullable()?.status

    fun <T> Optional<T>.toNullable(): T? = orElse(null)
}
