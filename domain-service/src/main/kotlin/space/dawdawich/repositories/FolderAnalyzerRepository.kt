package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.FolderAnalyzerDocument

interface FolderAnalyzerRepository : MongoRepository<FolderAnalyzerDocument, String> {

    fun findAllByFolderId(folderId: String): List<FolderAnalyzerDocument>
    fun deleteByAnalyzerIdIn(analyzerIds: Set<String>): Long
    fun deleteAllByAnalyzerId(analyzerId: String): Long
}
