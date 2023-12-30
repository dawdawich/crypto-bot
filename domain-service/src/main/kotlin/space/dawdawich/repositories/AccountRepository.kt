package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.AccountDocument

interface AccountRepository : MongoRepository<AccountDocument, String> {

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): AccountDocument?

    fun existsByEmailAndUsername(email: String, username: String): Boolean

    @Query("{email:  ?0}")
    @Update("{\$set: { username: ?1, name: ?2, surname: ?3, password: ?4, createTime: ?5, updateTime: ?6, role: ?7 }}")
    fun fillAccountInfo(email: String, username: String, name: String, surname: String, password: String, createTime: Long = System.currentTimeMillis(), updateTime: Long = createTime, role: String = "USER")
}
