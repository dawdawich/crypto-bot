package space.dawdawich.controller.model.account.transactions

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val amount: Int,
    val operationDate: Long
)
