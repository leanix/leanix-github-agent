package net.leanix.githubagent.controllers

import net.leanix.githubagent.exceptions.GitHubEventTypesNotSupported
import net.leanix.githubagent.services.WebhookService
import net.leanix.githubagent.shared.SUPPORTED_EVENT_TYPES
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/github")
class GitHubWebhookController(private val webhookService: WebhookService) {

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun hook(
        @RequestHeader("X-Github-Event") eventType: String,
        @RequestBody payload: String
    ) {
        runCatching {
            if (SUPPORTED_EVENT_TYPES.contains(eventType.uppercase())) {
                webhookService.consumeWebhookEvent(eventType, payload)
            } else {
                throw GitHubEventTypesNotSupported("Event Type not supported: $eventType")
            }
        }.onFailure {
            throw it
        }
    }
}
