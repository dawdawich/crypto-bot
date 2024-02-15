package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("folder_analyzer")
@CompoundIndex(def = "{'folderId': 1, 'analyzerId': 1}", unique = true)
data class FolderAnalyzerDocument(
        @Id
        val id: String = UUID.randomUUID().toString(),
        var folderId: String,
        var analyzerId: String
)
