package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.backtest.RequestStatusResponse
import space.dawdawich.repositories.mongo.RequestStatusRepository
import space.dawdawich.repositories.mongo.entity.RequestStatus
import space.dawdawich.repositories.mongo.entity.RequestStatusDocument
import kotlin.jvm.optionals.getOrNull

@Service
class RequestStatusService(private val requestStatusRepository: RequestStatusRepository) {

    fun createRequestStatus(requestId: String, accountId: String) {
        requestStatusRepository.insert(RequestStatusDocument(requestId, accountId, RequestStatus.IN_PROGRESS))
    }

    fun isRequestIdExistForAccount(requestId: String, accountId: String) = requestStatusRepository.existsByAccountIdAndId(accountId, requestId)

    fun getRequestStatus(requestId: String): RequestStatusResponse? =
        requestStatusRepository.findById(requestId).getOrNull()?.toResponse()

    fun getRequestStatusesForAccountId(accountId: String): List<RequestStatusResponse> = requestStatusRepository.getRequestStatusDocumentsByAccountId(accountId).map { it.toResponse() }

    fun RequestStatusDocument.toResponse(): RequestStatusResponse = RequestStatusResponse(this.id, this.accountId, this.status.name) }
