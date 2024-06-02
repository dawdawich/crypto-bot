package space.dawdawich.controller.model.folder

/**
 * Represents the response returned when getting a folder.
 *
 * @property id The unique identifier of the folder.
 * @property name The name of the folder.
 */
data class GetFolderResponse(val id: String, val name: String)
