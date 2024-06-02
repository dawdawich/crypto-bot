package space.dawdawich.controller.model.folder

/**
 * Represents the response returned when creating a folder.
 *
 * @property id The unique identifier of the folder.
 * @property name The name of the folder.
 */
data class CreateFolderResponse(val id: String, val name: String)
