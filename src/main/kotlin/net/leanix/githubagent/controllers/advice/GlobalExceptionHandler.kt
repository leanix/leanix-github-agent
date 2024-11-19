package net.leanix.githubagent.controllers.advice

import net.leanix.githubagent.exceptions.InvalidEventSignatureException
import net.leanix.githubagent.exceptions.WebhookSecretNotSetException
import net.leanix.githubagent.services.SyncLogService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler(
    private val syncLogService: SyncLogService
) {

    val exceptionLogger: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(InvalidEventSignatureException::class)
    fun handleInvalidEventSignatureException(exception: InvalidEventSignatureException): ProblemDetail {
        val detail = "Received an event with an invalid signature."
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail)
        problemDetail.title = exception.message
        exceptionLogger.warn(exception.message)
        syncLogService.sendErrorLog(detail)
        return problemDetail
    }

    @ExceptionHandler(WebhookSecretNotSetException::class)
    fun handleWebhookSecretNotSetException(exception: WebhookSecretNotSetException): ProblemDetail {
        val detail = "Unable to process GitHub event. Webhook secret is not set. " +
            "Please configure the webhook secret in the agent settings."
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
        problemDetail.title = exception.message
        syncLogService.sendErrorLog(detail)
        return problemDetail
    }

    @ExceptionHandler(Exception::class)
    fun handleUncaughtException(exception: Exception): ProblemDetail {
        val detail = "An unexpected error occurred. ${exception.message}"
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail)
        problemDetail.title = exception.message
        exceptionLogger.error("Uncaught exception: ${exception.message}", exception)
        syncLogService.sendErrorLog(detail)
        return problemDetail
    }
}
