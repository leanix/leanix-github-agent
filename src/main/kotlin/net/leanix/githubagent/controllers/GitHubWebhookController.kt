package net.leanix.githubagent.controllers

import net.leanix.githubagent.services.GitHubWebhookService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("github")
class GitHubWebhookController(private val gitHubWebhookService: GitHubWebhookService) {

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun hook(
        @RequestHeader("X-Github-Event") eventType: String,
        @RequestHeader("X-GitHub-Enterprise-Host") host: String,
        @RequestHeader("X-Hub-Signature-256", required = false) signature256: String?,
        @RequestBody payload: String
    ) {
        gitHubWebhookService.processWebhookEvent(eventType, host, signature256, payload)
    }
}
