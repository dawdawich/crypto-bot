package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("account")
class AccountDocument(
    @Id
    val walletId: String,
    var saltValidUntil: Long,
    val role: String = "USER",
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = createTime,
    val accepted: Boolean = false,
    val lastCheckedBlock: Long = 0
)
