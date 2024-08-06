package net.leanix.githubagent.controllers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import net.leanix.githubagent.services.WebhookService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
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
    private lateinit var webhookService: WebhookService

    @Test
    fun `should not process not supported events`() {
        val eventType = "UNSUPPORTED_EVENT"
        val payload = "{}"

        mockMvc.perform(
            MockMvcRequestBuilders.post("/github/webhook")
                .header("X-GitHub-Event", eventType)
                .content(payload)
        )
            .andExpect(MockMvcResultMatchers.status().isAccepted)

        verify(exactly = 0) { webhookService.consumeWebhookEvent(anyString(), anyString()) }
    }
}