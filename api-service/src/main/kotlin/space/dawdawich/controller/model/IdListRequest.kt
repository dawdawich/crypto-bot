package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

/**
 * Represents a request to work with a list of IDs.
 *
 * @property ids The list of IDs.
 * @property all Indicates if the operation should be applied to all IDs.
 */
@Serializable
data class IdListRequest(val ids: List<String>, val all: Boolean = false)
