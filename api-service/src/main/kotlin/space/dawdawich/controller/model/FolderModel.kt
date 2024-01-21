package space.dawdawich.controller.model

import com.fasterxml.jackson.annotation.JsonInclude

data class FolderModel(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        val id: String?,
        val name: String,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        val analyzers: Set<String>?
) {

    constructor(name: String) : this(null, name, null)

    constructor(id: String, name: String) : this(id, name, null)
}
