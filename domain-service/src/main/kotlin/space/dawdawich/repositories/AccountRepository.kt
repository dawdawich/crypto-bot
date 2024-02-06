package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.AccountDocument

interface AccountRepository : MongoRepository<AccountDocument, String>
