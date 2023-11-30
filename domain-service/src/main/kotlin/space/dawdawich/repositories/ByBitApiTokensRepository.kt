package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.ByBitApiTokens

interface ByBitApiTokensRepository : MongoRepository<ByBitApiTokens, String> {
}
