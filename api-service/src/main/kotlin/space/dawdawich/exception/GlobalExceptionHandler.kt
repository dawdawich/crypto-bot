package space.dawdawich.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import space.dawdawich.exception.model.CommonErrorModel
import space.dawdawich.exception.model.ErrorCodes

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler
    fun handleRuntimeException(exception: Exception): ResponseEntity<CommonErrorModel> {
        val errorMessage = exception.message?.let { CommonErrorModel(ErrorCodes.UNEXPECTED_ERROR_CODE.number, it) }
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
