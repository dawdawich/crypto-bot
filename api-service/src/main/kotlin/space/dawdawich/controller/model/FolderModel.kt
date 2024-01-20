package space.dawdawich.controller.model

import com.fasterxml.jackson.annotation.JsonInclude

data class FolderModel(
        val name: String,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        val analyzers: List<String>?
) {

    constructor(name: String) : this(name, null)
}
