package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("account")
class AccountDocument(
    @Id
    val id: String,
    val username: String,
    val name: String,
    val surname: String,
    val email: String,
    val role: String,
    val password: String,
    val createTime: Long,
    val updateTime: Long
) {
}
