package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.FolderAnalyzerDocument

interface FolderAnalyzerRepository : MongoRepository<FolderAnalyzerDocument, String> {

    fun findAllByFolderId(folderId: String): List<FolderAnalyzerDocument>
    fun findAllByAnalyzerId(analyzerIds: String): List<FolderAnalyzerDocument>
    fun deleteByAnalyzerIdIn(analyzerIds: Set<String>): Long
    fun deleteByFolderId(folderId: String): Long
    fun deleteByFolderIdAndAnalyzerIdIn(folderId: String, analyzerIds: Set<String>): Long
}
