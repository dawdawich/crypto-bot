package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.AccountDocument

interface AccountRepository : MongoRepository<AccountDocument, String>
