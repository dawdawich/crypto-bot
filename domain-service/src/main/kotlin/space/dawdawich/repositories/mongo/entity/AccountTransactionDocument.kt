package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document("account_transaction")
class AccountTransactionDocument(
    val accountId: String,
    val value: Int,
    val time: Long,
    @Id
    val id: String = UUID.randomUUID().toString()
)
