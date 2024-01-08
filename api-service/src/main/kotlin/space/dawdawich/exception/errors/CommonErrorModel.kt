package space.dawdawich.exception.errors

import kotlinx.serialization.Serializable

@Serializable
class CommonErrorModel(val errorId: Int, val errorDescription: String)
