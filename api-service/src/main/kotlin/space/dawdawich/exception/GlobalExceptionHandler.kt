package space.dawdawich.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import space.dawdawich.exception.model.CommonErrorModel
import space.dawdawich.exception.model.ErrorCodes

/**
 * This class is a global exception handler for handling runtime exceptions in the application.
 *
 * @ControllerAdvice - Indicates that this class is meant to handle exceptions across multiple controllers.
 */
@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handles a runtime exception by creating a `CommonErrorModel` with the error code
     * and error description from the exception message. Returns a `ResponseEntity`
     * with the `CommonErrorModel` and HTTP status code `INTERNAL_SERVER_ERROR`.
     *
     * @param exception The runtime exception to handle.
     * @return The `ResponseEntity` containing the `CommonErrorModel` and HTTP status code.
     */
    @ExceptionHandler
    fun handleRuntimeException(exception: Exception): ResponseEntity<CommonErrorModel> {
        val errorMessage = exception.message?.let { CommonErrorModel(ErrorCodes.UNEXPECTED_ERROR_CODE.number, it) }
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
