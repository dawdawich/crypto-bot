package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.mongo.entity.ServerConfigDocument

interface ServerConfigRepository : MongoRepository<ServerConfigDocument, String> {
    @Query("{_id: 'server-config'}")
    @Update("{\$set: {lastCheckedBlock: ?0}}")
    fun updateLastCheckedBlock(lastCheckedBlock: Long)

    @Query("{_id: 'server-config'}")
    fun getConfig(): ServerConfigDocument
}
