package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

/**
 * Data class representing a request to create a symbol.
 *
 * @param symbol The symbol to be created.
 */
@Serializable
data class CreateSymbolRequest(val symbol: String)
