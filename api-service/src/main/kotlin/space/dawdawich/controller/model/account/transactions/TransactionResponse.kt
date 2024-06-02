package space.dawdawich.controller.model.account.transactions

import kotlinx.serialization.Serializable

/**
 * Represents the response for a transaction.
 *
 * @property amount The amount of the transaction.
 * @property operationDate The timestamp of the transaction.
 */
@Serializable
data class TransactionResponse(
    val amount: Int,
    val operationDate: Long
)
