package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.BackTestResultDocument

interface BackTestResultRepository : MongoRepository<BackTestResultDocument, String> {
    fun getByRequestId(requestId: String): List<BackTestResultDocument>
}
