package space.dawdawich.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import space.dawdawich.exception.errors.CommonErrorModel
import space.dawdawich.exception.errors.ConflictException
import space.dawdawich.exception.errors.PreconditionException

@ControllerAdvice
class GlobalExceptionHandler {

    enum class ErrorCodes(val codeNumber: Int) {
        CONFLICT_CODE(1),
        PRECONDITION_CODE(2)
    }

    @ExceptionHandler
    fun handleConflictException(exception: ConflictException): ResponseEntity<CommonErrorModel> {
        val errorMessage = exception.message?.let { CommonErrorModel(ErrorCodes.CONFLICT_CODE.codeNumber, it) }
        return ResponseEntity(errorMessage, HttpStatus.CONFLICT)
    }

    @ExceptionHandler
    fun handlePreconditionException(exception: PreconditionException): ResponseEntity<CommonErrorModel> {
        val errorMessage = exception.message?.let { CommonErrorModel(ErrorCodes.PRECONDITION_CODE.codeNumber, it) }
        return ResponseEntity(errorMessage, HttpStatus.CONFLICT)
    }
}