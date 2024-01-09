package space.dawdawich.exception.model

import kotlinx.serialization.Serializable

@Serializable
class CommonErrorModel(val errorId: Int, val errorDescription: String)
