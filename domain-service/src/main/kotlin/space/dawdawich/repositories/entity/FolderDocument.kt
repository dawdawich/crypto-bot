package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("folder")
@CompoundIndex(def = "{'accountId': 1, 'name': 1}", unique = true)
data class FolderDocument(
        @Id
        val id: String = UUID.randomUUID().toString(),
        @Indexed
        val accountId: String,
        var name: String,
        var analyzers: MutableSet<String> = mutableSetOf()
)
