package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("server_config")
class ServerConfigDocument(
    val lastCheckedBlock: Long = 5381165L // default value is block when joat contact been created
) {
    @Id
    var id = "server-config"
}
