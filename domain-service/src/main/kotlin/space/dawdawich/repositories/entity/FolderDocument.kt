package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("folder")
data class FolderDocument(
        @Id
        val id: String,
        @Indexed
        val accountId: String,
        var name: String,
        var analyzers: List<String>?
)
