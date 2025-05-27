package net.leanix.githubagent.services

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import net.leanix.githubagent.dto.Account
import net.leanix.githubagent.dto.InstallationRequestDTO
import net.leanix.githubagent.handler.InstallationGetHandler
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class InstallationGetHandlerTest {

    @MockkBean
    private lateinit var webSocketService: WebSocketService

    @MockkBean
    private lateinit var cachingService: CachingService

    @SpykBean
    private lateinit var installationGetHandler: InstallationGetHandler

    @Test
    fun `should wait for active scan to finish before starting scanning new org`() {
        every { cachingService.get("runId") } returnsMany listOf("value", "value", "value", null)
        every { cachingService.set("runId", any(), any()) } just runs
        every { cachingService.remove("runId") } just runs
        every { webSocketService.sendMessage(any(), any()) } returns Unit

        val installationRequestDTO = InstallationRequestDTO(
            30,
            Account("test-org")
        )

        installationGetHandler.fetchAndSendOrganisationData(installationRequestDTO)

        verify(exactly = 6) { cachingService.get("runId") }
    }
}
