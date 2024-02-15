package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.FolderAnalyzerDocument

interface FolderAnalyzerRepository : MongoRepository<FolderAnalyzerDocument, String> {

    fun findAllByFolderId(folderId: String): List<FolderAnalyzerDocument>
    fun deleteByAnalyzerIdIn(analyzerIds: Set<String>): Long
}
