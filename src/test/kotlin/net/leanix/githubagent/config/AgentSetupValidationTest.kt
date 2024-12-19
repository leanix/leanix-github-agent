package net.leanix.githubagent.config

import io.mockk.every
import io.mockk.mockk
import net.leanix.githubagent.exceptions.GitHubEnterpriseConfigurationMissingException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgentSetupValidationTest {

    private lateinit var gitHubEnterpriseProperties: GitHubEnterpriseProperties
    private lateinit var leanIXProperties: LeanIXProperties
    private lateinit var agentSetupValidation: AgentSetupValidation

    @BeforeEach
    fun setUp() {
        gitHubEnterpriseProperties = mockk()
        leanIXProperties = mockk()
        agentSetupValidation = AgentSetupValidation(gitHubEnterpriseProperties, leanIXProperties)
    }

    @Test
    fun `validateConfiguration should pass when all properties are valid`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "https://example.com"
        every { gitHubEnterpriseProperties.gitHubAppId } returns "appId"
        every { gitHubEnterpriseProperties.pemFile } returns "pemFile"
        every { leanIXProperties.wsBaseUrl } returns "https://leanix.net"
        every { leanIXProperties.auth.accessTokenUri } returns "https://auth.leanix.net"

        agentSetupValidation.validateConfiguration()
    }

    @Test
    fun `validateConfiguration should throw exception when GitHubEnterpriseProperties are missing`() {
        every { gitHubEnterpriseProperties.baseUrl } returns ""
        every { gitHubEnterpriseProperties.gitHubAppId } returns ""
        every { gitHubEnterpriseProperties.pemFile } returns ""
        every { leanIXProperties.wsBaseUrl } returns "https://leanix.net"
        every { leanIXProperties.auth.accessTokenUri } returns "https://auth.leanix.net"

        assertThrows(GitHubEnterpriseConfigurationMissingException::class.java) {
            agentSetupValidation.validateConfiguration()
        }
    }

    @Test
    fun `validateConfiguration should throw exception when LeanIXProperties are missing`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "https://example.com"
        every { gitHubEnterpriseProperties.gitHubAppId } returns "appId"
        every { gitHubEnterpriseProperties.pemFile } returns "pemFile"
        every { leanIXProperties.wsBaseUrl } returns ""
        every { leanIXProperties.auth.accessTokenUri } returns ""

        assertThrows(GitHubEnterpriseConfigurationMissingException::class.java) {
            agentSetupValidation.validateConfiguration()
        }
    }
}
