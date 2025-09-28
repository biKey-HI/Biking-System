// Central error handler: On validation errors (missing/invalid fields) → returns 400 with a JSON
// like: {"message":"Validation failed","errors":{"field":"message"}}
//On duplicate email → returns 409 with {"message":"EMAIL_IN_USE"}.
package org.example.app.common

import org.example.app.auth.EmailAlreadyUsedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun onValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ResponseEntity.badRequest().body(mapOf("message" to "Validation failed", "errors" to errors))
    }

    @ExceptionHandler(EmailAlreadyUsedException::class)
    fun onDuplicateEmail(ex: EmailAlreadyUsedException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (ex.message ?: "EMAIL_IN_USE")))
}
