import io.mockk.every
import io.mockk.mockk
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubEnterpriseService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.ResourceLoader

class GitHubAuthenticationServiceTest {

    private val cachingService = mockk<CachingService>()
    private val githubEnterpriseProperties = mockk<GitHubEnterpriseProperties>()
    private val resourceLoader = mockk<ResourceLoader>()
    private val gitHubEnterpriseService = mockk<GitHubEnterpriseService>()
    private val githubAuthenticationService = GitHubAuthenticationService(
        cachingService,
        githubEnterpriseProperties,
        resourceLoader,
        gitHubEnterpriseService
    )

    @Test
    fun `generateJwtToken with valid data should not throw exception`() {
        every { cachingService.get(any()) } returns "dummy-value"
        every { cachingService.set(any(), any()) } returns Unit
        every { githubEnterpriseProperties.pemFile } returns "valid-private-key.pem"
        every { resourceLoader.getResource(any()) } returns ClassPathResource("valid-private-key.pem")
        every { gitHubEnterpriseService.verifyJwt(any()) } returns Unit

        assertDoesNotThrow { githubAuthenticationService.generateJwtToken() }
        assertNotNull(cachingService.get("jwtToken"))
    }

    @Test
    fun `generateJwtToken with invalid data should throw exception`() {
        every { cachingService.get(any()) } returns "dummy-value"
        every { githubEnterpriseProperties.pemFile } returns "invalid-private-key.pem"
        every { resourceLoader.getResource(any()) } returns ClassPathResource("invalid-private-key.pem")

        assertThrows(IllegalArgumentException::class.java) { githubAuthenticationService.generateJwtToken() }
    }
}
