package space.dawdawich.repositories

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.AnalyzerPositionDocument

interface AnalyzerPositionRepository : MongoRepository<AnalyzerPositionDocument, ObjectId> {
    fun getAllByAnalyzerId(analyzerId: String): List<AnalyzerPositionDocument>

    @Query("{_id: ?0}")
    @Update("{\$set: {closePrice: ?1, positionEntryPrice: ?2, positionSize: ?3, closeTime: ?4}}")
    fun markPositionAsComplete(positionId: String, closePrice: Double?, positionEntryPrice: Double, positionSize: Double, closeTime: Long = System.currentTimeMillis())
}
