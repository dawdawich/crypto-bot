package space.dawdawich.controller.model.folder

import kotlinx.serialization.Serializable

/**
 * Represents a request to update a folder.
 *
 * @param name The new name of the folder.
 */
@Serializable
data class UpdateFolderRequest(val name: String)
