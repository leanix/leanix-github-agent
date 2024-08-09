package net.leanix.githubagent.controllers.advice

import net.leanix.githubagent.exceptions.InvalidEventSignatureException
import net.leanix.githubagent.exceptions.WebhookSecretNotSetException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    val exceptionLogger: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(InvalidEventSignatureException::class)
    fun handleInvalidEventSignatureException(exception: InvalidEventSignatureException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid event signature")
        problemDetail.title = exception.message
        exceptionLogger.warn(exception.message)
        return problemDetail
    }

    @ExceptionHandler(WebhookSecretNotSetException::class)
    fun handleWebhookSecretNotSetException(exception: WebhookSecretNotSetException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Webhook secret not set")
        problemDetail.title = exception.message
        return problemDetail
    }
}
