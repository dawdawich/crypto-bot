package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.mongo.entity.RequestStatus
import space.dawdawich.repositories.mongo.entity.RequestStatusDocument

interface RequestStatusRepository : MongoRepository<RequestStatusDocument, String> {

    @Query("{ _id: ?0 }")
    @Update("{ \$set: { status: ?1 } }")
    fun updateRequestStatus(id: String, status: RequestStatus)
}
