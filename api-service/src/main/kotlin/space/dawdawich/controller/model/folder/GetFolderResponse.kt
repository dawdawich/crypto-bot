package space.dawdawich.controller.model.folder

data class GetFolderResponse(
        val id: String,
        val name: String,
        val analyzers: MutableSet<String> = mutableSetOf()
)
