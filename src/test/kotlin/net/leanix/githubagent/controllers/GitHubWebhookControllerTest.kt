package net.leanix.githubagent.controllers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import net.leanix.githubagent.exceptions.WebhookSecretNotSetException
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubWebhookHandler
import net.leanix.githubagent.services.SyncLogService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(GitHubWebhookController::class)
class GitHubWebhookControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var gitHubWebhookHandler: GitHubWebhookHandler

    @MockkBean
    private lateinit var syncLogService: SyncLogService

    @MockkBean
    private lateinit var cachingService: CachingService

    @BeforeEach
    fun setUp() {
        every { syncLogService.sendErrorLog(any()) } returns Unit
        every { cachingService.get(any()) } returns Unit
        every { cachingService.set(any(), any(), any()) } returns Unit
    }

    @Test
    fun `should return 202 if webhook event is processed successfully`() {
        val eventType = "PUSH"
        val payload = "{}"
        val host = "valid.host"

        every { gitHubWebhookHandler.handleWebhookEvent(any(), any(), any(), any()) } returns Unit

        mockMvc.perform(
            MockMvcRequestBuilders.post("/github/webhook")
                .header("X-GitHub-Event", eventType)
                .header("X-GitHub-Enterprise-Host", host)
                .content(payload)
        )
            .andExpect(MockMvcResultMatchers.status().isAccepted)
    }

    @Test
    fun `should return 400 if missing webhook secret when event had signature`() {
        val eventType = "UNSUPPORTED_EVENT"
        val payload = "{}"
        val host = "host"
        val signature256 = "sha256=invalidsignature"

        every {
            gitHubWebhookHandler.handleWebhookEvent(
                eventType, host, signature256, payload
            )
        } throws WebhookSecretNotSetException()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/github/webhook")
                .header("X-GitHub-Event", eventType)
                .header("X-GitHub-Enterprise-Host", host)
                .header("X-Hub-Signature-256", signature256)
                .content(payload)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
