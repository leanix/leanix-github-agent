package net.leanix.githubagent.listener

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.services.GitHubStartService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationReadyListenerTest {

    private lateinit var gitHubStartService: GitHubStartService
    private lateinit var applicationListener: ApplicationReadyListener

    @BeforeEach
    fun setUp() {
        gitHubStartService = mockk()

        applicationListener = ApplicationReadyListener(
            gitHubStartService
        )

        every { gitHubStartService.startAgent() } returns Unit
    }

    @Test
    fun `should start the agent process`() {
        applicationListener.onApplicationEvent(mockk())

        verify { gitHubStartService.startAgent() }
    }
}
