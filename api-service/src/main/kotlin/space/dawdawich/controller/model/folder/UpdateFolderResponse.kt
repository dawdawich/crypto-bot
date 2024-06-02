package space.dawdawich.controller.model.folder

/**
 * Represents the response returned when updating a folder.
 *
 * @param id The unique identifier of the folder.
 * @param name The name of the folder.
 */
data class UpdateFolderResponse(val id: String, val name: String)
