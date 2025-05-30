package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.FolderAnalyzerDocument

interface FolderAnalyzerRepository : MongoRepository<FolderAnalyzerDocument, String> {

    fun findAllByFolderId(folderId: String): List<FolderAnalyzerDocument>
    fun findAllByAnalyzerId(analyzerIds: String): List<FolderAnalyzerDocument>
    fun deleteByAnalyzerIdIn(analyzerIds: Set<String>): Long
    fun deleteByFolderId(folderId: String): Long
    fun deleteByFolderIdAndAnalyzerIdIn(folderId: String, analyzerIds: Set<String>): Long
    fun countByFolderId(folderId: String): Int
}
